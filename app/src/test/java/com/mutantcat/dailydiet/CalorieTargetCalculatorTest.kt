package com.mutantcat.dailydiet

import com.mutantcat.dailydiet.domain.calculator.CalorieTargetCalculator
import com.mutantcat.dailydiet.domain.model.ActivityLevel
import com.mutantcat.dailydiet.domain.model.Sex
import com.mutantcat.dailydiet.domain.model.TargetWarning
import com.mutantcat.dailydiet.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalorieTargetCalculatorTest {

    @Test
    fun `male bmr uses mifflin st jeor`() {
        val profile = UserProfile(
            sex = Sex.MALE,
            age = 30,
            heightCm = 175.0,
            currentWeightKg = 80.0,
            targetWeightKg = 75.0,
            targetDays = 90,
            activityLevel = ActivityLevel.SEDENTARY,
        )

        assertEquals(1748.75, CalorieTargetCalculator.bmr(profile), 0.001)
    }

    @Test
    fun `female bmr uses mifflin st jeor`() {
        val profile = UserProfile(
            sex = Sex.FEMALE,
            age = 30,
            heightCm = 160.0,
            currentWeightKg = 60.0,
            targetWeightKg = 55.0,
            targetDays = 90,
            activityLevel = ActivityLevel.SEDENTARY,
        )

        assertEquals(1289.0, CalorieTargetCalculator.bmr(profile), 0.001)
    }

    @Test
    fun `tdee multiplies bmr by activity factor`() {
        val profile = UserProfile(
            sex = Sex.MALE,
            age = 30,
            heightCm = 175.0,
            currentWeightKg = 80.0,
            targetWeightKg = 75.0,
            targetDays = 90,
            activityLevel = ActivityLevel.MODERATE,
        )

        assertEquals(1748.75 * 1.55, CalorieTargetCalculator.tdee(profile), 0.001)
    }

    @Test
    fun `intake floor prevents unsafe low calorie target`() {
        val profile = UserProfile(
            sex = Sex.FEMALE,
            age = 25,
            heightCm = 150.0,
            currentWeightKg = 45.0,
            targetWeightKg = 40.0,
            targetDays = 30,
            activityLevel = ActivityLevel.SEDENTARY,
        )

        val target = CalorieTargetCalculator.calculate(profile)

        assertEquals(1200.0, target.dailyIntake, 0.001)
        assertTrue(target.warnings.contains(TargetWarning.INTAKE_FLOOR))
    }

    @Test
    fun `target above current weight has no deficit`() {
        val profile = UserProfile(
            sex = Sex.MALE,
            age = 30,
            heightCm = 175.0,
            currentWeightKg = 70.0,
            targetWeightKg = 75.0,
            targetDays = 90,
        )

        val target = CalorieTargetCalculator.calculate(profile)

        assertEquals(0.0, target.deficit, 0.001)
        assertTrue(target.warnings.contains(TargetWarning.TARGET_NOT_BELOW_CURRENT))
    }

    @Test
    fun `deficit is capped to a safe share of tdee`() {
        val profile = UserProfile(
            sex = Sex.MALE,
            age = 30,
            heightCm = 175.0,
            currentWeightKg = 80.0,
            targetWeightKg = 60.0,
            targetDays = 30,
            activityLevel = ActivityLevel.SEDENTARY,
        )

        val target = CalorieTargetCalculator.calculate(profile)

        assertTrue(target.warnings.contains(TargetWarning.LARGE_DEFICIT))
        assertTrue(target.deficit <= target.tdee * 0.25 + 0.001)
    }
}

