package com.mutantcat.dailydiet

import com.mutantcat.dailydiet.core.units.EnergyUnit
import com.mutantcat.dailydiet.core.units.MassUnit
import com.mutantcat.dailydiet.core.units.UnitFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnitFormatterTest {

    @Test
    fun `energy formats in current unit`() {
        assertEquals("1500 kcal", UnitFormatter.energy(1500.0, EnergyUnit.KILOCALORIE))
        assertEquals("6276 kJ", UnitFormatter.energy(1500.0, EnergyUnit.KILOJOULE))
        assertEquals("1500000 cal", UnitFormatter.energy(1500.0, EnergyUnit.CALORIE))
    }

    @Test
    fun `mass formats in current unit`() {
        assertEquals("150 g", UnitFormatter.mass(150.0, MassUnit.GRAM))
        assertEquals("0 kg", UnitFormatter.mass(150.0, MassUnit.KILOGRAM))
        assertEquals("0.15 kg", UnitFormatter.mass(150.0, MassUnit.KILOGRAM, 2))
        assertEquals("5.3 oz", UnitFormatter.mass(150.0, MassUnit.OUNCE, 1))
    }

    @Test
    fun `parseMass round-trips through every unit`() {
        val grams = 237.0
        MassUnit.entries.forEach { unit ->
            val shown = UnitFormatter.format(unit.fromGrams(grams), 3)
            val parsed = UnitFormatter.parseMass(shown, unit)
            assertEquals(grams, parsed!!, 0.01)
        }
    }

    @Test
    fun `parseMass rejects garbage`() {
        assertNull(UnitFormatter.parseMass("", MassUnit.GRAM))
        assertNull(UnitFormatter.parseMass("abc", MassUnit.GRAM))
        assertNull(UnitFormatter.parseMass("1,2", MassUnit.GRAM))
    }
}
