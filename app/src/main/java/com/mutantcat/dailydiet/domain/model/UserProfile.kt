package com.mutantcat.dailydiet.domain.model

enum class Sex {
    MALE,
    FEMALE,
}

enum class ActivityLevel(val factor: Double) {
    SEDENTARY(1.2),
    LIGHT(1.375),
    MODERATE(1.55),
    HIGH(1.725),
    EXTREME(1.9),
}

data class UserProfile(
    val sex: Sex = Sex.MALE,
    val age: Int = 30,
    val heightCm: Double = 170.0,
    val currentWeightKg: Double = 70.0,
    val targetWeightKg: Double = 65.0,
    val targetDays: Int = 90,
    val activityLevel: ActivityLevel = ActivityLevel.LIGHT,
)

enum class TargetWarning {
    INVALID_INPUT,
    SHORT_PERIOD,
    LARGE_DEFICIT,
    INTAKE_FLOOR,
    TARGET_NOT_BELOW_CURRENT,
}

data class CalorieTarget(
    val bmr: Double,
    val tdee: Double,
    val deficit: Double,
    val dailyIntake: Double,
    val minIntakeFloor: Double,
    val warnings: List<TargetWarning> = emptyList(),
)

enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK,
}

data class TodaySummary(
    val target: CalorieTarget,
    val intakeKcal: Double = 0.0,
    val exerciseKcal: Double = 0.0,
    val proteinG: Double = 0.0,
    val fatG: Double = 0.0,
    val carbG: Double = 0.0,
) {
    val netIntakeKcal: Double get() = intakeKcal - exerciseKcal
    val remainingKcal: Double get() = target.dailyIntake + exerciseKcal - intakeKcal
    val actualDeficitKcal: Double get() = target.tdee - netIntakeKcal
}

