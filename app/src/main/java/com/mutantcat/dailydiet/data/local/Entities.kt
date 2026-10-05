package com.mutantcat.dailydiet.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "foods",
    indices = [Index("name"), Index("aliases")],
)
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val aliases: String = "",
    val category: String = "",
    val kcalPer100g: Double,
    val proteinPer100g: Double = 0.0,
    val fatPer100g: Double = 0.0,
    val carbPer100g: Double = 0.0,
    val defaultPortionGrams: Double = 100.0,
    val portionLabel: String = "100 克",
    val source: String = "builtin",
    val isCustom: Boolean = false,
)

@Entity(
    tableName = "food_logs",
    indices = [Index("loggedAt"), Index("mealType")],
)
data class FoodLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodId: Long? = null,
    val foodName: String,
    val grams: Double,
    val kcal: Double,
    val proteinG: Double = 0.0,
    val fatG: Double = 0.0,
    val carbG: Double = 0.0,
    val mealType: String,
    val loggedAt: Long,
    val source: String = "local",
    val note: String = "",
)

@Entity(tableName = "exercise_logs", indices = [Index("loggedAt")])
data class ExerciseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "",
    val minutes: Int,
    val met: Double,
    val kcal: Double,
    val loggedAt: Long,
    val note: String = "",
)

@Entity(tableName = "weight_logs", indices = [Index("loggedAt")])
data class WeightLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weightKg: Double,
    val loggedAt: Long,
)

