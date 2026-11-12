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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class DailyDietViewModel(
    private val container: AppContainer,
) : ViewModel() {

    /** 当前展示的是哪一天（本地时区）。跨零点或夏令时切换时由 [dayWindow] 重新计算。 */
    private val currentDay = MutableStateFlow(LocalDate.now())

    private val dayWindow: StateFlow<DayWindow> = currentDay
        .map(::windowOf)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = windowOf(currentDay.value),
        )

    init {
        // 每分钟检查一次「今天」是否变了（跨零点或夏令时切换），变了就重新订阅新的一天。
        viewModelScope.launch {
            while (true) {
                delay(DAY_TICK_MS)
                val today = LocalDate.now()
                if (today != currentDay.value) {
                    currentDay.value = today
                }
            }
        }
    }

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

    val todayFoodLogs: StateFlow<List<FoodLogEntity>> = dayWindow
        .flatMapLatest { window -> container.logRepository.foodLogs(window.start, window.end) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val todayExerciseLogs: StateFlow<List<ExerciseLogEntity>> = dayWindow
        .flatMapLatest { window -> container.logRepository.exerciseLogs(window.start, window.end) }
        .stateIn(
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
            // 冷启动时 Application 可能还没播种完；这里再等一次互斥锁，然后立刻加载列表。
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

    private var searchJob: Job? = null

    /** 输入防抖：打字过程中取消上一次查询，停顿 250ms 后才真正查库。 */
    fun searchFoods(query: String, debounceMs: Long = SEARCH_DEBOUNCE_MS) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounceMs > 0) delay(debounceMs)
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
        proteinPer100g: Double = 0.0,
        fatPer100g: Double = 0.0,
        carbPer100g: Double = 0.0,
    ) {
        viewModelScope.launch {
            val id = container.foodRepository.addCustomFood(
                name = name,
                kcalPer100g = kcalPer100g,
                proteinPer100g = proteinPer100g,
                fatPer100g = fatPer100g,
                carbPer100g = carbPer100g,
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
                item = item,
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
            container.profileRepository.updateCurrentWeight(weightKg)
            _message.value = "体重已记录"
        }
    }

    fun deleteWeightLog(log: WeightLogEntity) {
        viewModelScope.launch {
            container.logRepository.deleteWeightLog(log)
            // 删掉最新一条记录后，回退当前体重到剩下的最新值；没有记录时保持不变。
            val latest = container.logRepository.weightLogs().first().firstOrNull()
            latest?.let { container.profileRepository.updateCurrentWeight(it.weightKg) }
        }
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

    private data class DayWindow(val start: Long, val end: Long)

    companion object {
        private const val DAY_TICK_MS = 60L * 1000L
        private const val SEARCH_DEBOUNCE_MS = 250L

        /** 按本地时区的「当天零点 ~ 次日零点」计算区间，跨夏令时也不会错一秒。 */
        private fun windowOf(day: LocalDate): DayWindow {
            val zone = ZoneId.systemDefault()
            val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            return DayWindow(start = start, end = end)
        }

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DailyDietViewModel(container)
            }
        }
    }
}

