package com.mutantcat.dailydiet.ui.exercise

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mutantcat.dailydiet.data.local.ExerciseLogEntity
import com.mutantcat.dailydiet.domain.calculator.ExerciseCalculator
import com.mutantcat.dailydiet.domain.calculator.ExercisePreset
import com.mutantcat.dailydiet.ui.components.MetricRow
import com.mutantcat.dailydiet.ui.components.NumberField
import com.mutantcat.dailydiet.ui.components.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseScreen(
    weightKg: Double,
    exerciseLogs: List<ExerciseLogEntity>,
    onAdd: (ExercisePreset, Int) -> Unit,
    onDelete: (ExerciseLogEntity) -> Unit,
) {
    var selectedPreset by remember { mutableStateOf<ExercisePreset?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("运动") }) },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(contentPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SectionCard(
                    title = "今日运动",
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    if (exerciseLogs.isEmpty()) {
                        Text(
                            text = "还没有运动记录",
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
                                Text(log.name, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "${log.minutes} 分钟 · ${log.kcal.toInt()} kcal · " +
                                        "MET ${log.met}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { onDelete(log) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "删除")
                            }
                        }
                    }
                    MetricRow("今日运动消耗", "${exerciseLogs.sumOf { it.kcal }.toInt()} kcal")
                }
            }

            item {
                Text(
                    text = "选择运动",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            items(ExerciseCalculator.presets, key = { it.name }) { preset ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedPreset = preset }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(preset.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "${preset.category} · MET ${preset.met}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "${ExerciseCalculator.kcal(preset.met, weightKg, 30).toInt()} kcal/30min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider()
            }
        }
    }

    selectedPreset?.let { preset ->
        ExerciseDialog(
            preset = preset,
            weightKg = weightKg,
            onDismiss = { selectedPreset = null },
            onConfirm = { minutes ->
                onAdd(preset, minutes)
                selectedPreset = null
            },
        )
    }
}

@Composable
private fun ExerciseDialog(
    preset: ExercisePreset,
    weightKg: Double,
    onDismiss: () -> Unit,
    onConfirm: (minutes: Int) -> Unit,
) {
    var minutesText by remember(preset.name) { mutableStateOf("30") }
    val minutes = minutesText.toIntOrNull() ?: 0
    val kcal = ExerciseCalculator.kcal(preset.met, weightKg, minutes)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(preset.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(
                    value = minutesText,
                    onValueChange = { minutesText = it },
                    label = "运动时长",
                    suffix = "分钟",
                )
                MetricRow("估算消耗", "${kcal.toInt()} kcal")
                Text(
                    text = "按体重 ${weightKg.toInt()} kg 和 MET ${preset.met} 估算，可在记录后手动删除重记。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(minutes) },
                enabled = minutes > 0,
            ) {
                Text("记录")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

