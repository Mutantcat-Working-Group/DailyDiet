package com.mutantcat.dailydiet.core.units

import java.util.Locale

enum class EnergyUnit(val label: String, val symbol: String, val kcalPerUnit: Double) {
    KILOCALORIE("千卡 / 大卡", "kcal", 1.0),
    KILOJOULE("千焦", "kJ", 1.0 / 4.184),
    CALORIE("卡路里 / 卡", "cal", 0.001),
    ;

    fun fromKcal(kcal: Double): Double = kcal / kcalPerUnit

    fun toKcal(value: Double): Double = value * kcalPerUnit
}

enum class MassUnit(val label: String, val symbol: String, val gramsPerUnit: Double) {
    GRAM("克", "g", 1.0),
    KILOGRAM("千克", "kg", 1000.0),
    OUNCE("盎司", "oz", 28.349523125),
    ;

    fun toGrams(value: Double): Double = value * gramsPerUnit

    fun fromGrams(grams: Double): Double = grams / gramsPerUnit
}

object UnitFormatter {
    fun format(value: Double, decimals: Int = 0): String =
        String.format(Locale.US, "%.${decimals}f", value)

    /** 按当前热量单位格式化，例如 "1234 kcal"。数据内部一律按 kcal 存储。 */
    fun energy(kcal: Double, unit: EnergyUnit, decimals: Int = 0): String =
        "${format(unit.fromKcal(kcal), decimals)} ${unit.symbol}"

    /** 按当前重量单位格式化，例如 "150 g"。数据内部一律按克存储。 */
    fun mass(grams: Double, unit: MassUnit, decimals: Int = 0): String =
        "${format(unit.fromGrams(grams), decimals)} ${unit.symbol}"

    /** 把以当前单位显示的重量解析回克，输入非法时返回 null。 */
    fun parseMass(text: String, unit: MassUnit): Double? =
        text.trim().toDoubleOrNull()?.let { unit.toGrams(it) }
}

