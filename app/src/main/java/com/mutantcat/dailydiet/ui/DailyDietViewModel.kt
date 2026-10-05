package com.mutantcat.dailydiet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mutantcat.dailydiet.core.units.EnergyUnit
import com.mutantcat.dailydiet.core.units.MassUnit
import com.mutantcat.dailydiet.data.ai.VisionEstimateResult
import com.mutantcat.dailydiet.data.ai.VisionFoodItem
import com.mutantcat.dailydiet.data.local.ExerciseLogEntity
import com.mutantcat.dailydiet.data.local.FoodEntity
import com.mutantcat.dailydiet.data.local.FoodLogEntity
import com.mutantcat.dailydiet.data.local.WeightLogEntity
import com.mutantcat.dailydiet.data.repository.AiSettings
import com.mutantcat.dailydiet.di.AppContainer
import com.mutantcat.dailydiet.domain.calculator.CalorieTargetCalculator
import com.mutantcat.dailydiet.domain.model.CalorieTarget
import com.mutantcat.dailydiet.domain.model.MealType
import com.mutantcat.dailydiet.domain.model.TodaySummary
import com.mutantcat.dailydiet.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class DailyDietViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val dayStart: Long = LocalDate.now()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    private val dayEnd: Long = dayStart + DAY_MILLIS - 1

    val profile: StateFlow<UserProfile> = container.profileRepository.profileFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserProfile(),
    )

    val profileConfigured: StateFlow<Boolean?> = container.profileRepository.profileConfiguredFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    val energyUnit: StateFlow<EnergyUnit> = container.profileRepository.energyUnitFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = EnergyUnit.KILOCALORIE,
    )

    val massUnit: StateFlow<MassUnit> = container.profileRepository.massUnitFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = MassUnit.GRAM,
    )

    val aiSettings: StateFlow<AiSettings> = container.aiSettingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AiSettings(),
    )

    val todayFoodLogs: StateFlow<List<FoodLogEntity>> =
        container.logRepository.foodLogs(dayStart, dayEnd).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val todayExerciseLogs: StateFlow<List<ExerciseLogEntity>> =
        container.logRepository.exerciseLogs(dayStart, dayEnd).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val weightLogs: StateFlow<List<WeightLogEntity>> = container.logRepository.weightLogs().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val calorieTarget: StateFlow<CalorieTarget> = profile
        .map { CalorieTargetCalculator.calculate(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = CalorieTargetCalculator.calculate(UserProfile()),
        )

    val todaySummary: StateFlow<TodaySummary> = combine(
        calorieTarget,
        todayFoodLogs,
        todayExerciseLogs,
    ) { target, foods, exercises ->
        TodaySummary(
            target = target,
            intakeKcal = foods.sumOf { it.kcal },
            exerciseKcal = exercises.sumOf { it.kcal },
            proteinG = foods.sumOf { it.proteinG },
            fatG = foods.sumOf { it.fatG },
            carbG = foods.sumOf { it.carbG },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodaySummary(target = calorieTarget.value),
    )

    private val _foods = MutableStateFlow<List<FoodEntity>>(emptyList())
    val foods: StateFlow<List<FoodEntity>> = _foods.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _pendingMeal = MutableStateFlow<MealType?>(null)
    val pendingMeal: StateFlow<MealType?> = _pendingMeal.asStateFlow()

    init {
        viewModelScope.launch {
            container.initialize()
            _foods.value = container.foodRepository.search("")
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun openFoodForMeal(mealType: MealType) {
        _pendingMeal.value = mealType
    }

    fun consumePendingMeal() {
        _pendingMeal.value = null
    }

    fun searchFoods(query: String) {
        viewModelScope.launch {
            _foods.value = container.foodRepository.search(query)
        }
    }

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            container.profileRepository.saveProfile(profile)
            _message.value = "目标已保存"
        }
    }

    fun saveEnergyUnit(unit: EnergyUnit) {
        viewModelScope.launch { container.profileRepository.saveEnergyUnit(unit) }
    }

    fun saveMassUnit(unit: MassUnit) {
        viewModelScope.launch { container.profileRepository.saveMassUnit(unit) }
    }

    fun saveAiSettings(settings: AiSettings) {
        viewModelScope.launch {
            container.aiSettingsRepository.save(settings)
            _message.value = "AI 识别设置已保存"
        }
    }

    fun addFood(food: FoodEntity, grams: Double, mealType: MealType) {
        viewModelScope.launch {
            container.logRepository.addFood(
                food = food,
                grams = grams,
                mealType = mealType,
                loggedAt = System.currentTimeMillis(),
            )
            _message.value = "已记录 ${food.name}"
        }
    }

    fun addCustomFood(
        name: String,
        kcalPer100g: Double,
        grams: Double,
        mealType: MealType,
    ) {
        viewModelScope.launch {
            val id = container.foodRepository.addCustomFood(
                name = name,
                kcalPer100g = kcalPer100g,
            )
            val food = container.foodRepository.findById(id)
            if (food != null) {
                container.logRepository.addFood(
                    food = food,
                    grams = grams,
                    mealType = mealType,
                    loggedAt = System.currentTimeMillis(),
                )
                _message.value = "已添加自定义食物"
            } else {
                _message.value = "自定义食物保存失败"
            }
            searchFoods("")
        }
    }

    fun addEstimatedFood(
        item: VisionFoodItem,
        grams: Double,
        kcalPer100g: Double,
        mealType: MealType,
    ) {
        viewModelScope.launch {
            container.logRepository.addEstimatedFood(
                name = item.name,
                grams = grams,
                kcalPer100g = kcalPer100g,
                mealType = mealType,
                loggedAt = System.currentTimeMillis(),
                note = item.note,
            )
            _message.value = "已记录 ${item.name}"
        }
    }

    fun deleteFoodLog(log: FoodLogEntity) {
        viewModelScope.launch { container.logRepository.deleteFoodLog(log) }
    }

    fun addExercise(
        name: String,
        category: String,
        met: Double,
        minutes: Int,
    ) {
        viewModelScope.launch {
            container.logRepository.addExercise(
                name = name,
                category = category,
                met = met,
                minutes = minutes,
                weightKg = profile.value.currentWeightKg,
                loggedAt = System.currentTimeMillis(),
            )
            _message.value = "已记录 $name"
        }
    }

    fun deleteExerciseLog(log: ExerciseLogEntity) {
        viewModelScope.launch { container.logRepository.deleteExerciseLog(log) }
    }

    fun addWeight(weightKg: Double) {
        viewModelScope.launch {
            container.logRepository.addWeight(weightKg, System.currentTimeMillis())
            _message.value = "体重已记录"
        }
    }

    fun deleteWeightLog(log: WeightLogEntity) {
        viewModelScope.launch { container.logRepository.deleteWeightLog(log) }
    }

    fun estimateFood(
        imageBytes: ByteArray,
        onResult: (Result<VisionEstimateResult>) -> Unit,
    ) {
        viewModelScope.launch {
            val result = container.visionFoodEstimator.estimate(imageBytes)
            result.exceptionOrNull()?.let { _message.value = it.message }
            onResult(result)
        }
    }

    companion object {
        private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DailyDietViewModel(container)
            }
        }
    }
}

