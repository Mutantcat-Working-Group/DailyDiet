package com.mutantcat.dailydiet.data.repository

import com.mutantcat.dailydiet.data.local.ExerciseLogDao
import com.mutantcat.dailydiet.data.local.ExerciseLogEntity
import com.mutantcat.dailydiet.data.local.FoodEntity
import com.mutantcat.dailydiet.data.local.FoodLogDao
import com.mutantcat.dailydiet.data.local.FoodLogEntity
import com.mutantcat.dailydiet.data.local.WeightLogDao
import com.mutantcat.dailydiet.data.local.WeightLogEntity
import com.mutantcat.dailydiet.domain.calculator.ExerciseCalculator
import com.mutantcat.dailydiet.domain.model.MealType
import kotlinx.coroutines.flow.Flow

class LogRepository(
    private val foodLogDao: FoodLogDao,
    private val exerciseLogDao: ExerciseLogDao,
    private val weightLogDao: WeightLogDao,
) {
    fun foodLogs(start: Long, end: Long): Flow<List<FoodLogEntity>> =
        foodLogDao.observeBetween(start, end)

    fun exerciseLogs(start: Long, end: Long): Flow<List<ExerciseLogEntity>> =
        exerciseLogDao.observeBetween(start, end)

    fun weightLogs(): Flow<List<WeightLogEntity>> = weightLogDao.observeRecent()

    suspend fun addFood(
        food: FoodEntity,
        grams: Double,
        mealType: MealType,
        loggedAt: Long,
        source: String = "local",
    ) {
        val factor = grams / 100.0
        foodLogDao.insert(
            FoodLogEntity(
                foodId = food.id,
                foodName = food.name,
                grams = grams,
                kcal = food.kcalPer100g * factor,
                proteinG = food.proteinPer100g * factor,
                fatG = food.fatPer100g * factor,
                carbG = food.carbPer100g * factor,
                mealType = mealType.name,
                loggedAt = loggedAt,
                source = source,
            ),
        )
    }

    suspend fun addEstimatedFood(
        name: String,
        grams: Double,
        kcalPer100g: Double,
        mealType: MealType,
        loggedAt: Long,
        note: String = "",
    ) {
        foodLogDao.insert(
            FoodLogEntity(
                foodName = name,
                grams = grams,
                kcal = kcalPer100g * grams / 100.0,
                mealType = mealType.name,
                loggedAt = loggedAt,
                source = "ai",
                note = note,
            ),
        )
    }

    suspend fun deleteFoodLog(log: FoodLogEntity) {
        foodLogDao.delete(log)
    }

    suspend fun addExercise(
        name: String,
        category: String,
        met: Double,
        minutes: Int,
        weightKg: Double,
        loggedAt: Long,
    ) {
        exerciseLogDao.insert(
            ExerciseLogEntity(
                name = name,
                category = category,
                minutes = minutes,
                met = met,
                kcal = ExerciseCalculator.kcal(met, weightKg, minutes),
                loggedAt = loggedAt,
            ),
        )
    }

    suspend fun deleteExerciseLog(log: ExerciseLogEntity) {
        exerciseLogDao.delete(log)
    }

    suspend fun addWeight(weightKg: Double, loggedAt: Long) {
        weightLogDao.insert(WeightLogEntity(weightKg = weightKg, loggedAt = loggedAt))
    }

    suspend fun deleteWeightLog(log: WeightLogEntity) {
        weightLogDao.delete(log)
    }
}

