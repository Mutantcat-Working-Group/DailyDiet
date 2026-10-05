package com.mutantcat.dailydiet

import com.mutantcat.dailydiet.domain.calculator.ExerciseCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseCalculatorTest {

    @Test
    fun `met formula uses hours`() {
        assertEquals(280.0, ExerciseCalculator.kcal(met = 8.0, weightKg = 70.0, minutes = 30), 0.001)
    }

    @Test
    fun `invalid input returns zero`() {
        assertEquals(0.0, ExerciseCalculator.kcal(met = 0.0, weightKg = 70.0, minutes = 30), 0.001)
        assertEquals(0.0, ExerciseCalculator.kcal(met = 8.0, weightKg = 0.0, minutes = 30), 0.001)
        assertEquals(0.0, ExerciseCalculator.kcal(met = 8.0, weightKg = 70.0, minutes = 0), 0.001)
    }
}

