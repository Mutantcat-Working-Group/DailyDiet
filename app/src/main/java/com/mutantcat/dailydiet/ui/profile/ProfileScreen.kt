package com.mutantcat.dailydiet.ui.profile

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.mutantcat.dailydiet.BuildConfig
import com.mutantcat.dailydiet.core.units.EnergyUnit
import com.mutantcat.dailydiet.core.units.MassUnit
import com.mutantcat.dailydiet.core.units.UnitFormatter
import com.mutantcat.dailydiet.data.local.WeightLogEntity
import com.mutantcat.dailydiet.domain.model.CalorieTarget
import com.mutantcat.dailydiet.domain.model.UserProfile
import com.mutantcat.dailydiet.ui.components.MetricRow
import com.mutantcat.dailydiet.ui.components.NumberField
import com.mutantcat.dailydiet.ui.components.SectionCard
import com.mutantcat.dailydiet.ui.label
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: UserProfile,
    calorieTarget: CalorieTarget,
    energyUnit: EnergyUnit,
    massUnit: MassUnit,
    weightLogs: List<WeightLogEntity>,
    onEditGoal: () -> Unit,
    onOpenSettings: () -> Unit,
    onChangeEnergyUnit: (EnergyUnit) -> Unit,
    onChangeMassUnit: (MassUnit) -> Unit,
    onAddWeight: (Double) -> Unit,
    onDeleteWeight: (WeightLogEntity) -> Unit,
) {
    var showWeightDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("我的") }) },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard(title = "目标") {
                MetricRow("性别 / 年龄", "${profile.sex.label()} / ${profile.age} 岁")
                MetricRow("身高", "${profile.heightCm.toInt()} cm")
                MetricRow("当前体重", "${profile.currentWeightKg} kg")
                MetricRow("目标体重", "${profile.targetWeightKg} kg")
                MetricRow("目标周期", "${profile.targetDays} 天")
                MetricRow("活动水平", profile.activityLevel.label())
                MetricRow(
                    label = "每日可摄入",
                    value = "${calorieTarget.dailyIntake.toInt()} kcal",
                    emphasize = true,
                )
                MetricRow("每日目标缺口", "${calorieTarget.deficit.toInt()} kcal")
                OutlinedButton(
                    onClick = onEditGoal,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Text("修改目标", modifier = Modifier.padding(start = 8.dp))
                }
            }

            SectionCard(title = "体重趋势") {
                val latest = weightLogs.firstOrNull()
                val previous = weightLogs.getOrNull(1)
                if (latest == null) {
                    Text(
                        text = "还没有体重记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "${latest.weightKg} kg",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (previous != null) {
                        val delta = latest.weightKg - previous.weightKg
                        Text(
                            text = "较上次 ${if (delta >= 0) "+" else ""}${UnitFormatter.format(delta, 1)} kg",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (delta <= 0) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.tertiary
                            },
                        )
                    }
                }

                Button(
                    onClick = { showWeightDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.MonitorWeight, contentDescription = null)
                    Text("记录体重", modifier = Modifier.padding(start = 8.dp))
                }

                weightLogs.take(7).forEach { log ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${log.weightKg} kg",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = DATE_FORMAT.format(Date(log.loggedAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        IconButton(onClick = { onDeleteWeight(log) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "删除")
                        }
                    }
                }
            }

            SectionCard(title = "单位") {
                Text("热量单位", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    EnergyUnit.entries.forEach { unit ->
                        FilterChip(
                            selected = energyUnit == unit,
                            onClick = { onChangeEnergyUnit(unit) },
                            label = { Text("${unit.label}（${unit.symbol}）") },
                        )
                    }
                }
                Text("重量单位", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MassUnit.entries.forEach { unit ->
                        FilterChip(
                            selected = massUnit == unit,
                            onClick = { onChangeMassUnit(unit) },
                            label = { Text("${unit.label}（${unit.symbol}）") },
                        )
                    }
                }
                Text(
                    text = "1 千卡 = 4.184 千焦 = 1000 卡路里；1 千克 = 1000 克；1 盎司 = 28.35 克",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionCard(title = "AI 识别") {
                Text(
                    text = "配置任意 OpenAI 兼容的视觉模型，拍照后由模型估算食物种类、重量和热量，再由你确认入库。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = null)
                    Text("AI 识别设置", modifier = Modifier.padding(start = 8.dp))
                }
            }

            Text(
                text = "本应用只做日常记录和估算，不构成医疗建议。身高、体重等属于敏感个人信息，数据默认只保存在本机。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = "版本 ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showWeightDialog) {
        WeightDialog(
            initialWeight = profile.currentWeightKg,
            onDismiss = { showWeightDialog = false },
            onConfirm = { weight ->
                onAddWeight(weight)
                showWeightDialog = false
            },
        )
    }
}

@Composable
private fun WeightDialog(
    initialWeight: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var weightText by remember { mutableStateOf(initialWeight.toString()) }
    val weight = weightText.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记录体重") },
        text = {
            NumberField(
                value = weightText,
                onValueChange = { weightText = it },
                label = "体重",
                suffix = "kg",
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(weight) },
                enabled = weight > 0.0,
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

private val DATE_FORMAT = SimpleDateFormat("MM-dd", Locale.CHINA)

