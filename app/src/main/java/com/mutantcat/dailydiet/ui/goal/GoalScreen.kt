package com.mutantcat.dailydiet.ui.goal

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mutantcat.dailydiet.core.units.EnergyUnit
import com.mutantcat.dailydiet.core.units.UnitFormatter
import com.mutantcat.dailydiet.domain.calculator.CalorieTargetCalculator
import com.mutantcat.dailydiet.domain.model.ActivityLevel
import com.mutantcat.dailydiet.domain.model.Sex
import com.mutantcat.dailydiet.domain.model.UserProfile
import com.mutantcat.dailydiet.ui.components.MetricRow
import com.mutantcat.dailydiet.ui.components.NumberField
import com.mutantcat.dailydiet.ui.components.SectionCard
import com.mutantcat.dailydiet.ui.description
import com.mutantcat.dailydiet.ui.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalScreen(
    initialProfile: UserProfile,
    energyUnit: EnergyUnit,
    onSave: (UserProfile) -> Unit,
    showBack: Boolean,
    onBack: () -> Unit,
) {
    var sex by remember { mutableStateOf(initialProfile.sex) }
    var ageText by remember { mutableStateOf(initialProfile.age.toString()) }
    var heightText by remember { mutableStateOf(initialProfile.heightCm.toTrimmedString()) }
    var currentWeightText by remember {
        mutableStateOf(initialProfile.currentWeightKg.toTrimmedString())
    }
    var targetWeightText by remember {
        mutableStateOf(initialProfile.targetWeightKg.toTrimmedString())
    }
    var targetDaysText by remember { mutableStateOf(initialProfile.targetDays.toString()) }
    var activityLevel by remember { mutableStateOf(initialProfile.activityLevel) }

    val age = ageText.toIntOrNull() ?: 0
    val heightCm = heightText.toDoubleOrNull() ?: 0.0
    val currentWeightKg = currentWeightText.toDoubleOrNull() ?: 0.0
    val targetWeightKg = targetWeightText.toDoubleOrNull() ?: 0.0
    val targetDays = targetDaysText.toIntOrNull() ?: 0

    val previewProfile = UserProfile(
        sex = sex,
        age = age,
        heightCm = heightCm,
        currentWeightKg = currentWeightKg,
        targetWeightKg = targetWeightKg,
        targetDays = targetDays,
        activityLevel = activityLevel,
    )
    val target = CalorieTargetCalculator.calculate(previewProfile)

    // 输入不合法时禁止保存，避免把全 0 的 profile 写进 DataStore 后只能靠计算器兜 INVALID_INPUT。
    val inputsValid = age in MIN_AGE..MAX_AGE &&
        heightCm in MIN_HEIGHT_CM..MAX_HEIGHT_CM &&
        currentWeightKg in MIN_WEIGHT_KG..MAX_WEIGHT_KG &&
        targetWeightKg in MIN_WEIGHT_KG..MAX_WEIGHT_KG &&
        targetDays >= MIN_TARGET_DAYS

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showBack) "修改目标" else "设置目标") },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard(title = "基本信息") {
                Text("性别", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Sex.entries.forEach { option ->
                        FilterChip(
                            selected = sex == option,
                            onClick = { sex = option },
                            label = { Text(option.label()) },
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(
                        value = ageText,
                        onValueChange = { ageText = it },
                        label = "年龄",
                        modifier = Modifier.weight(1f),
                        suffix = "岁",
                    )
                    NumberField(
                        value = heightText,
                        onValueChange = { heightText = it },
                        label = "身高",
                        modifier = Modifier.weight(1f),
                        suffix = "cm",
                    )
                }
            }

            SectionCard(title = "体重目标") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(
                        value = currentWeightText,
                        onValueChange = { currentWeightText = it },
                        label = "当前体重",
                        modifier = Modifier.weight(1f),
                        suffix = "kg",
                    )
                    NumberField(
                        value = targetWeightText,
                        onValueChange = { targetWeightText = it },
                        label = "目标体重",
                        modifier = Modifier.weight(1f),
                        suffix = "kg",
                    )
                }
                NumberField(
                    value = targetDaysText,
                    onValueChange = { targetDaysText = it },
                    label = "目标周期",
                    suffix = "天",
                )
            }

            SectionCard(title = "日常活动水平") {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ActivityLevel.entries.forEach { option ->
                        FilterChip(
                            selected = activityLevel == option,
                            onClick = { activityLevel = option },
                            label = { Text(option.label()) },
                        )
                    }
                }
                Text(
                    text = activityLevel.description(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionCard(title = "每日预算预览") {
                MetricRow(
                    label = "基础代谢 BMR",
                    value = UnitFormatter.energy(target.bmr, energyUnit),
                )
                MetricRow(
                    label = "每日总消耗 TDEE",
                    value = UnitFormatter.energy(target.tdee, energyUnit),
                )
                MetricRow(
                    label = "目标热量缺口",
                    value = UnitFormatter.energy(target.deficit, energyUnit),
                )
                MetricRow(
                    label = "每日可摄入",
                    value = UnitFormatter.energy(target.dailyIntake, energyUnit),
                    emphasize = true,
                )
            }

            if (!inputsValid) {
                Text(
                    text = "请输入有效数值：年龄 $MIN_AGE-$MAX_AGE 岁，身高 $MIN_HEIGHT_CM-${MAX_HEIGHT_CM.toInt()} cm，" +
                        "体重 $MIN_WEIGHT_KG-${MAX_WEIGHT_KG.toInt()} kg，目标周期至少 $MIN_TARGET_DAYS 天。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (target.warnings.isNotEmpty()) {
                SectionCard(title = "提示") {
                    target.warnings.forEach { warning ->
                        Text(
                            text = warning.label(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }

            Text(
                text = "BMR、TDEE 和运动消耗都是估算值，个体误差通常在 10-20%。体重变化后建议重新校准目标。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = { onSave(previewProfile) },
                modifier = Modifier.fillMaxWidth(),
                enabled = inputsValid,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Text(
                    text = "保存目标",
                    modifier = Modifier.padding(start = 8.dp),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private fun Double.toTrimmedString(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()

// 与 CalorieTargetCalculator 的兜底逻辑对应的合理输入区间。
private const val MIN_AGE = 10
private const val MAX_AGE = 100
private const val MIN_HEIGHT_CM = 80.0
private const val MAX_HEIGHT_CM = 250.0
private const val MIN_WEIGHT_KG = 25.0
private const val MAX_WEIGHT_KG = 300.0
private const val MIN_TARGET_DAYS = 7

