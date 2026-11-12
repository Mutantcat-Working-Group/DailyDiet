package com.mutantcat.dailydiet.data.local

import android.content.Context
import android.util.Log
import org.json.JSONArray

class FoodSeeder(
    private val context: Context,
    private val foodDao: FoodDao,
) {
    /**
     * 首次启动时把内置食物写入 foods 表。只在表为空时执行。
     *
     * 并发安全由调用方 [com.mutantcat.dailydiet.data.repository.FoodRepository.ensureSeeded]
     * 的互斥锁保证；这里保持 count() 判定 + 一次性 insertAll（Room 对 List 插入自带事务）。
     */
    suspend fun seedIfEmpty() {
        if (foodDao.count() > 0) return

        val json = context.assets.open(SEED_FILE).bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        val foods = buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    FoodEntity(
                        name = item.getString("name"),
                        aliases = item.optString("aliases", ""),
                        category = item.optString("category", ""),
                        kcalPer100g = item.getDouble("kcalPer100g"),
                        proteinPer100g = item.optDouble("proteinPer100g", 0.0),
                        fatPer100g = item.optDouble("fatPer100g", 0.0),
                        carbPer100g = item.optDouble("carbPer100g", 0.0),
                        defaultPortionGrams = item.optDouble("defaultPortionGrams", 100.0),
                        portionLabel = item.optString("portionLabel", "100 克"),
                        source = item.optString("source", "builtin"),
                    ),
                )
            }
        }
        foodDao.insertAll(foods)
        Log.i(TAG, "Seeded ${foods.size} built-in foods")
    }

    private companion object {
        const val TAG = "FoodSeeder"
        const val SEED_FILE = "food_seed.json"
    }
}

