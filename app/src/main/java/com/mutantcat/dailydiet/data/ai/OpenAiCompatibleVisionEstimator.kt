package com.mutantcat.dailydiet.data.ai

import android.util.Base64
import com.mutantcat.dailydiet.data.repository.AiSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class OpenAiCompatibleVisionEstimator(
    private val settingsProvider: suspend () -> AiSettings,
    private val client: OkHttpClient = defaultClient(),
) : VisionFoodEstimator {

    override suspend fun estimate(
        imageBytes: ByteArray,
        hint: String,
    ): Result<VisionEstimateResult> = withContext(Dispatchers.IO) {
        val settings = settingsProvider()
        if (!settings.isConfigured) {
            return@withContext Result.failure(
                IllegalStateException("请先在「我的 - AI 识别设置」中填写模型地址、API Key 和模型名"),
            )
        }

        runCatching { requestEstimate(settings, imageBytes, hint) }
    }

    private fun requestEstimate(
        settings: AiSettings,
        imageBytes: ByteArray,
        hint: String,
    ): VisionEstimateResult {
        val encodedImage = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        val payload = JSONObject().apply {
            put("model", settings.model)
            put("temperature", 0.1)
            put(
                "messages",
                JSONArray().apply {
                    put(
                        JSONObject().apply {
                            put("role", "system")
                            put("content", SYSTEM_PROMPT)
                        },
                    )
                    put(
                        JSONObject().apply {
                            put("role", "user")
                            put(
                                "content",
                                JSONArray().apply {
                                    put(
                                        JSONObject().apply {
                                            put("type", "text")
                                            put("text", userPrompt(hint))
                                        },
                                    )
                                    put(
                                        JSONObject().apply {
                                            put("type", "image_url")
                                            put(
                                                "image_url",
                                                JSONObject().put(
                                                    "url",
                                                    "data:image/jpeg;base64,$encodedImage",
                                                ),
                                            )
                                        },
                                    )
                                },
                            )
                        },
                    )
                },
            )
        }

        val request = Request.Builder()
            .url(chatCompletionsUrl(settings.baseUrl))
            .header("Authorization", "Bearer ${settings.apiKey}")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("AI 请求失败（${response.code}）：${body.take(300)}")
            }

            val content = JSONObject(body)
                .optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                .orEmpty()

            val json = JSONObject(extractJson(content))
            val itemArray = json.optJSONArray("items") ?: JSONArray()
            val items = buildList {
                for (index in 0 until itemArray.length()) {
                    val item = itemArray.optJSONObject(index) ?: continue
                    val name = item.optString("name").trim()
                    val grams = item.optDouble("estimatedGrams", 0.0)
                    if (name.isEmpty() || grams <= 0.0) continue

                    val kcalPer100g = item.optDouble("kcalPer100g", -1.0)
                        .takeIf { it > 0.0 }

                    add(
                        VisionFoodItem(
                            name = name,
                            estimatedGrams = grams,
                            kcalPer100g = kcalPer100g,
                            confidence = item.optDouble("confidence", 0.5).coerceIn(0.0, 1.0),
                            note = item.optString("note", ""),
                        ),
                    )
                }
            }

            if (items.isEmpty()) {
                throw IllegalStateException("模型没有返回可识别的食物，请换一张更清晰的照片")
            }
            return VisionEstimateResult(items = items, rawText = content)
        }
    }

    private fun chatCompletionsUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        return if (trimmed.endsWith("/chat/completions")) {
            trimmed
        } else {
            "$trimmed/chat/completions"
        }
    }

    private fun extractJson(content: String): String {
        val cleaned = content
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        return if (start >= 0 && end > start) cleaned.substring(start, end + 1) else cleaned
    }

    private fun userPrompt(hint: String): String = buildString {
        append("请识别这张饮食照片里的食物。")
        if (hint.isNotBlank()) {
            append("用户补充：")
            append(hint)
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        const val SYSTEM_PROMPT = """
你是饮食份量估算助手。根据图片中的食物，估计每种食物的名称、可食用重量、每 100 克热量、置信度和备注。
要求：
1. 重量单位为克，只计算可食用部分，不包含餐具和包装。
2. 热量单位为千卡（kcal）。
3. 无法判断热量时把 kcalPer100g 设为 null。
4. 只输出 JSON，不要输出解释文字。
输出格式：
{"items":[{"name":"馒头","estimatedGrams":120,"kcalPer100g":223,"confidence":0.7,"note":"约 1 个中等大小"}]}
"""

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .build()
    }
}

