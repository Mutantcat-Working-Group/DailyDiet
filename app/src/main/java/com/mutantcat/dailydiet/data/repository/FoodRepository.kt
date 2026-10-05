package com.mutantcat.dailydiet.data.repository

import com.mutantcat.dailydiet.data.local.FoodDao
import com.mutantcat.dailydiet.data.local.FoodEntity
import com.mutantcat.dailydiet.data.local.FoodSeeder

class FoodRepository(
    private val foodDao: FoodDao,
    private val seeder: FoodSeeder,
) {
    suspend fun ensureSeeded() {
        seeder.seedIfEmpty()
    }

    suspend fun search(query: String): List<FoodEntity> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) foodDao.first() else foodDao.search(trimmed)
    }

    suspend fun findById(id: Long): FoodEntity? = foodDao.findById(id)

    suspend fun addCustomFood(
        name: String,
        kcalPer100g: Double,
        proteinPer100g: Double = 0.0,
        fatPer100g: Double = 0.0,
        carbPer100g: Double = 0.0,
        defaultPortionGrams: Double = 100.0,
    ): Long = foodDao.insert(
        FoodEntity(
            name = name.trim(),
            category = "自定义",
            kcalPer100g = kcalPer100g,
            proteinPer100g = proteinPer100g,
            fatPer100g = fatPer100g,
            carbPer100g = carbPer100g,
            defaultPortionGrams = defaultPortionGrams,
            portionLabel = "${defaultPortionGrams.toInt()} 克",
            source = "custom",
            isCustom = true,
        ),
    )
}

