package com.mutantcat.dailydiet.domain.calculator

import com.mutantcat.dailydiet.domain.model.CalorieTarget
import com.mutantcat.dailydiet.domain.model.Sex
import com.mutantcat.dailydiet.domain.model.TargetWarning
import com.mutantcat.dailydiet.domain.model.UserProfile

object CalorieTargetCalculator {
    const val KCAL_PER_KG_FAT = 7700.0
    const val MIN_INTAKE_FEMALE = 1200.0
    const val MIN_INTAKE_MALE = 1500.0
    const val RECOMMENDED_MAX_DEFICIT = 500.0
    const val ABSOLUTE_MAX_DEFICIT = 1000.0

    fun bmr(profile: UserProfile): Double {
        val base = 10.0 * profile.currentWeightKg +
            6.25 * profile.heightCm -
            5.0 * profile.age
        return if (profile.sex == Sex.MALE) base + 5.0 else base - 161.0
    }

    fun tdee(profile: UserProfile): Double = bmr(profile) * profile.activityLevel.factor

    fun calculate(profile: UserProfile): CalorieTarget {
        val minFloor = if (profile.sex == Sex.MALE) MIN_INTAKE_MALE else MIN_INTAKE_FEMALE
        val rawBmr = bmr(profile)
        val rawTdee = tdee(profile)

        if (profile.age <= 0 || profile.heightCm <= 0.0 ||
            profile.currentWeightKg <= 0.0 || profile.targetDays <= 0
        ) {
            return CalorieTarget(
                bmr = rawBmr.coerceAtLeast(0.0),
                tdee = rawTdee.coerceAtLeast(0.0),
                deficit = 0.0,
                dailyIntake = rawTdee.coerceAtLeast(0.0),
                minIntakeFloor = minFloor,
                warnings = listOf(TargetWarning.INVALID_INPUT),
            )
        }

        val warnings = linkedSetOf<TargetWarning>()
        val weightToLose = profile.currentWeightKg - profile.targetWeightKg
        if (weightToLose <= 0.0) {
            warnings += TargetWarning.TARGET_NOT_BELOW_CURRENT
            return CalorieTarget(
                bmr = rawBmr,
                tdee = rawTdee,
                deficit = 0.0,
                dailyIntake = rawTdee,
                minIntakeFloor = minFloor,
                warnings = warnings.toList(),
            )
        }

        if (profile.targetDays < 30) {
            warnings += TargetWarning.SHORT_PERIOD
        }

        val desiredDeficit = weightToLose * KCAL_PER_KG_FAT / profile.targetDays
        val safeDeficitCeiling = minOf(ABSOLUTE_MAX_DEFICIT, rawTdee * 0.25)
        val cappedDeficit = desiredDeficit.coerceIn(0.0, safeDeficitCeiling)

        if (desiredDeficit > RECOMMENDED_MAX_DEFICIT) {
            warnings += TargetWarning.LARGE_DEFICIT
        }

        var deficit = cappedDeficit
        var intake = rawTdee - deficit
        if (intake < minFloor) {
            intake = minFloor
            deficit = (rawTdee - intake).coerceAtLeast(0.0)
            warnings += TargetWarning.INTAKE_FLOOR
        }

        return CalorieTarget(
            bmr = rawBmr,
            tdee = rawTdee,
            deficit = deficit,
            dailyIntake = intake,
            minIntakeFloor = minFloor,
            warnings = warnings.toList(),
        )
    }
}

