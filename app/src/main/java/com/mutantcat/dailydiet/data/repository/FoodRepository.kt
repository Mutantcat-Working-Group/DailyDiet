package com.mutantcat.dailydiet.data.repository

import com.mutantcat.dailydiet.data.local.FoodDao
import com.mutantcat.dailydiet.data.local.FoodEntity
import com.mutantcat.dailydiet.data.local.FoodSeeder
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FoodRepository(
    private val foodDao: FoodDao,
    private val seeder: FoodSeeder,
) {
    // Application 和 ViewModel 都会触发 initialize()，用互斥锁避免 check-then-act 重复播种。
    private val seedMutex = Mutex()

    suspend fun ensureSeeded() {
        seedMutex.withLock { seeder.seedIfEmpty() }
    }

    suspend fun search(query: String): List<FoodEntity> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) foodDao.first() else foodDao.search(escapeLike(trimmed))
    }

    suspend fun findById(id: Long): FoodEntity? = foodDao.findById(id)

    /**
     * 转义 LIKE 通配符，避免用户输入 % 或 _ 时把搜索结果放大成整表。
     * 配合 DAO 里的 ESCAPE '\\' 子句使用。
     */
    private fun escapeLike(query: String): String = query
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")

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

