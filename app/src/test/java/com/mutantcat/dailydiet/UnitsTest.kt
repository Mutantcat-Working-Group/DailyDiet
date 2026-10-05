package com.mutantcat.dailydiet

import com.mutantcat.dailydiet.core.units.EnergyUnit
import com.mutantcat.dailydiet.core.units.MassUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitsTest {

    @Test
    fun `kcal converts to kilojoule`() {
        assertEquals(418.4, EnergyUnit.KILOJOULE.fromKcal(100.0), 0.01)
        assertEquals(100.0, EnergyUnit.KILOJOULE.toKcal(418.4), 0.01)
    }

    @Test
    fun `kcal converts to calorie`() {
        assertEquals(1000.0, EnergyUnit.CALORIE.fromKcal(1.0), 0.001)
        assertEquals(1.0, EnergyUnit.CALORIE.toKcal(1000.0), 0.001)
    }

    @Test
    fun `mass units convert to grams`() {
        assertEquals(1000.0, MassUnit.KILOGRAM.toGrams(1.0), 0.001)
        assertEquals(28.349523125, MassUnit.OUNCE.toGrams(1.0), 0.0000001)
        assertEquals(1.0, MassUnit.GRAM.fromGrams(1.0), 0.001)
    }
}

