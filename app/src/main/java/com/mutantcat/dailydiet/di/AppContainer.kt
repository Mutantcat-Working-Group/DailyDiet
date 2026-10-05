package com.mutantcat.dailydiet.di

import android.content.Context
import com.mutantcat.dailydiet.data.ai.OpenAiCompatibleVisionEstimator
import com.mutantcat.dailydiet.data.ai.VisionFoodEstimator
import com.mutantcat.dailydiet.data.local.DailyDietDatabase
import com.mutantcat.dailydiet.data.local.FoodSeeder
import com.mutantcat.dailydiet.data.repository.AiSettingsRepository
import com.mutantcat.dailydiet.data.repository.FoodRepository
import com.mutantcat.dailydiet.data.repository.LogRepository
import com.mutantcat.dailydiet.data.repository.ProfileRepository
import com.mutantcat.dailydiet.data.repository.settingsDataStore

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = DailyDietDatabase.build(appContext)
    private val foodSeeder = FoodSeeder(appContext, database.foodDao())

    val profileRepository = ProfileRepository(appContext.settingsDataStore)
    val aiSettingsRepository = AiSettingsRepository(appContext.settingsDataStore)
    val foodRepository = FoodRepository(database.foodDao(), foodSeeder)
    val logRepository = LogRepository(
        foodLogDao = database.foodLogDao(),
        exerciseLogDao = database.exerciseLogDao(),
        weightLogDao = database.weightLogDao(),
    )
    val visionFoodEstimator: VisionFoodEstimator = OpenAiCompatibleVisionEstimator(
        settingsProvider = { aiSettingsRepository.current() },
    )

    suspend fun initialize() {
        foodRepository.ensureSeeded()
    }
}

