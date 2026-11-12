package com.mutantcat.dailydiet.data.ai

data class VisionFoodItem(
    val name: String,
    val estimatedGrams: Double,
    val kcalPer100g: Double?,
    val confidence: Double,
    val note: String = "",
    /** 模型给出的宏量营养，单位克 / 每 100 克可食部分。模型没给时保持 null。 */
    val proteinPer100g: Double? = null,
    val fatPer100g: Double? = null,
    val carbPer100g: Double? = null,
)

data class VisionEstimateResult(
    val items: List<VisionFoodItem>,
    val rawText: String,
)

/**
 * Pluggable interface so a user can point the app at any OpenAI-compatible
 * vision endpoint without the app depending on a specific vendor.
 */
interface VisionFoodEstimator {
    suspend fun estimate(imageBytes: ByteArray, hint: String = ""): Result<VisionEstimateResult>
}

