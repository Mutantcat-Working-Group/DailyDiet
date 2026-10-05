package com.mutantcat.dailydiet.domain.calculator

data class ExercisePreset(
    val name: String,
    val met: Double,
    val category: String,
)

object ExerciseCalculator {
    val presets: List<ExercisePreset> = listOf(
        ExercisePreset("走路 4.8 km/h", 3.5, "有氧"),
        ExercisePreset("快走 6.4 km/h", 5.0, "有氧"),
        ExercisePreset("慢跑 8 km/h", 8.3, "有氧"),
        ExercisePreset("跑步 10 km/h", 9.8, "有氧"),
        ExercisePreset("骑行 休闲", 4.0, "有氧"),
        ExercisePreset("骑行 中速", 6.8, "有氧"),
        ExercisePreset("跳绳 中速", 11.8, "有氧"),
        ExercisePreset("游泳 自由泳 中速", 8.3, "有氧"),
        ExercisePreset("瑜伽 哈他", 2.5, "柔韧"),
        ExercisePreset("力量训练 中等", 5.0, "力量"),
        ExercisePreset("力量训练 高强度", 6.0, "力量"),
        ExercisePreset("HIIT", 8.0, "力量"),
        ExercisePreset("羽毛球", 5.5, "球类"),
        ExercisePreset("篮球", 6.5, "球类"),
        ExercisePreset("足球", 7.0, "球类"),
        ExercisePreset("爬楼梯", 8.8, "日常"),
        ExercisePreset("家务 中等强度", 3.3, "日常"),
    )

    /** kcal = MET x weight(kg) x hours */
    fun kcal(met: Double, weightKg: Double, minutes: Int): Double {
        if (met <= 0.0 || weightKg <= 0.0 || minutes <= 0) return 0.0
        return met * weightKg * (minutes / 60.0)
    }
}

