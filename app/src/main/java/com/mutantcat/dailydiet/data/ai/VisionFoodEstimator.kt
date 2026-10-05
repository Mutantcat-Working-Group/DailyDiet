package com.mutantcat.dailydiet.data.ai

data class VisionFoodItem(
    val name: String,
    val estimatedGrams: Double,
    val kcalPer100g: Double?,
    val confidence: Double,
    val note: String = "",
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

