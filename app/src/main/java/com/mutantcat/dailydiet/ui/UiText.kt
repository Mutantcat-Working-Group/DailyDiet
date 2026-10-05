package com.mutantcat.dailydiet.ui

import com.mutantcat.dailydiet.domain.model.ActivityLevel
import com.mutantcat.dailydiet.domain.model.MealType
import com.mutantcat.dailydiet.domain.model.Sex
import com.mutantcat.dailydiet.domain.model.TargetWarning

fun MealType.label(): String = when (this) {
    MealType.BREAKFAST -> "早餐"
    MealType.LUNCH -> "午餐"
    MealType.DINNER -> "晚餐"
    MealType.SNACK -> "加餐"
}

fun Sex.label(): String = when (this) {
    Sex.MALE -> "男"
    Sex.FEMALE -> "女"
}

fun ActivityLevel.label(): String = when (this) {
    ActivityLevel.SEDENTARY -> "久坐"
    ActivityLevel.LIGHT -> "轻度活动"
    ActivityLevel.MODERATE -> "中度活动"
    ActivityLevel.HIGH -> "高度活动"
    ActivityLevel.EXTREME -> "极高活动"
}

fun ActivityLevel.description(): String = when (this) {
    ActivityLevel.SEDENTARY -> "几乎不运动"
    ActivityLevel.LIGHT -> "每周 1-3 次"
    ActivityLevel.MODERATE -> "每周 3-5 次"
    ActivityLevel.HIGH -> "每周 6-7 次"
    ActivityLevel.EXTREME -> "体力工作或一天两练"
}

fun TargetWarning.label(): String = when (this) {
    TargetWarning.INVALID_INPUT -> "请检查年龄、身高、体重和目标天数"
    TargetWarning.SHORT_PERIOD -> "目标周期偏短，建议至少 30 天"
    TargetWarning.LARGE_DEFICIT -> "目标缺口偏大，已按安全上限收敛"
    TargetWarning.INTAKE_FLOOR -> "已应用最低摄入保护值"
    TargetWarning.TARGET_NOT_BELOW_CURRENT -> "目标体重不低于当前体重，暂不计算减脂缺口"
}

