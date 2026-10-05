package com.mutantcat.dailydiet.data.local

import android.content.Context
import org.json.JSONArray

class FoodSeeder(
    private val context: Context,
    private val foodDao: FoodDao,
) {
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
    }

    private companion object {
        const val SEED_FILE = "food_seed.json"
    }
}

