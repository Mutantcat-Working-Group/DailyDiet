package com.mutantcat.dailydiet.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mutantcat.dailydiet.core.units.EnergyUnit
import com.mutantcat.dailydiet.core.units.UnitFormatter
import com.mutantcat.dailydiet.data.local.ExerciseLogEntity
import com.mutantcat.dailydiet.data.local.FoodLogEntity
import com.mutantcat.dailydiet.domain.model.MealType
import com.mutantcat.dailydiet.domain.model.TodaySummary
import com.mutantcat.dailydiet.ui.components.MetricRow
import com.mutantcat.dailydiet.ui.components.SectionCard
import com.mutantcat.dailydiet.ui.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    summary: TodaySummary,
    energyUnit: EnergyUnit,
    foodLogs: List<FoodLogEntity>,
    exerciseLogs: List<ExerciseLogEntity>,
    onAddFood: (MealType) -> Unit,
    onAddExercise: () -> Unit,
    onDeleteFoodLog: (FoodLogEntity) -> Unit,
    onDeleteExerciseLog: (ExerciseLogEntity) -> Unit,
) {
    val display: (Double) -> String = { kcal ->
        "${UnitFormatter.format(energyUnit.fromKcal(kcal))} ${energyUnit.symbol}"
    }
    val progress = if (summary.target.dailyIntake > 0.0) {
        (summary.netIntakeKcal / summary.target.dailyIntake).coerceIn(0.0, 1.0).toFloat()
    } else {
        0f
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("今天") }) },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard {
                Text(
                    text = "剩余可摄入",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = display(summary.remainingKcal),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (summary.remainingKcal >= 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (summary.remainingKcal >= 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
                HorizontalDivider()
                MetricRow("已摄入", display(summary.intakeKcal))
                MetricRow("运动消耗", display(summary.exerciseKcal))
                MetricRow("每日预算", display(summary.target.dailyIntake))
            }

            SectionCard(title = "热量缺口") {
                MetricRow("基础代谢 BMR", display(summary.target.bmr))
                MetricRow("每日总消耗 TDEE", display(summary.target.tdee))
                MetricRow("净摄入", display(summary.netIntakeKcal))
                MetricRow(
                    label = "今日实际缺口",
                    value = display(summary.actualDeficitKcal),
                    emphasize = true,
                )
            }

            SectionCard(title = "宏量营养") {
                MetricRow("蛋白质", "${UnitFormatter.format(summary.proteinG, 1)} g")
                MetricRow("脂肪", "${UnitFormatter.format(summary.fatG, 1)} g")
                MetricRow("碳水", "${UnitFormatter.format(summary.carbG, 1)} g")
            }

            SectionCard(title = "饮食记录") {
                MealType.entries.forEachIndexed { index, meal ->
                    if (index > 0) HorizontalDivider()
                    val logs = foodLogs.filter { it.mealType == meal.name }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(meal.label(), fontWeight = FontWeight.SemiBold)
                            Text(
                                text = display(logs.sumOf { it.kcal }),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onAddFood(meal) }) {
                            Icon(Icons.Filled.Add, contentDescription = "添加${meal.label()}")
                        }
                    }
                    logs.forEach { log ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(log.foodName, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "${UnitFormatter.format(log.grams)} g" +
                                        if (log.source == "ai") " · AI 估算" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(display(log.kcal))
                            IconButton(onClick = { onDeleteFoodLog(log) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "删除")
                            }
                        }
                    }
                }
            }

            SectionCard(title = "运动记录") {
                if (exerciseLogs.isEmpty()) {
                    Text(
                        text = "今天还没有运动记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                exerciseLogs.forEach { log ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(log.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "${log.minutes} 分钟 · ${log.kcal.toInt()} kcal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onDeleteExerciseLog(log) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "删除")
                        }
                    }
                }
                Button(
                    onClick = onAddExercise,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null)
                    Text("记录运动", modifier = Modifier.padding(start = 8.dp))
                }
            }

            Button(
                onClick = { onAddFood(MealType.SNACK) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Restaurant, contentDescription = null)
                Text("记录饮食", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

