package com.mutantcat.dailydiet.ui.food

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mutantcat.dailydiet.core.units.EnergyUnit
import com.mutantcat.dailydiet.core.units.MassUnit
import com.mutantcat.dailydiet.core.units.UnitFormatter
import com.mutantcat.dailydiet.data.ai.VisionFoodItem
import com.mutantcat.dailydiet.data.local.FoodEntity
import com.mutantcat.dailydiet.domain.model.MealType
import com.mutantcat.dailydiet.ui.DailyDietViewModel
import com.mutantcat.dailydiet.ui.components.MetricRow
import com.mutantcat.dailydiet.ui.components.NumberField
import com.mutantcat.dailydiet.ui.components.SectionCard
import com.mutantcat.dailydiet.ui.label
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

private enum class PortionUnitMode(val label: String) {
    PORTION("份"),
    GRAM("克"),
    KILOGRAM("千克"),
    OUNCE("盎司"),
}

private data class ConfirmedEstimate(
    val item: VisionFoodItem,
    val grams: Double,
    val kcalPer100g: Double,
)

@Stable
private class EditableEstimate(item: VisionFoodItem) {
    val item: VisionFoodItem = item
    var gramsText by mutableStateOf(item.estimatedGrams.toInt().toString())
    var kcalText by mutableStateOf(item.kcalPer100g?.toInt()?.toString().orEmpty())
    var included by mutableStateOf(true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodScreen(
    viewModel: DailyDietViewModel,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val foods by viewModel.foods.collectAsStateWithLifecycle()
    val energyUnit by viewModel.energyUnit.collectAsStateWithLifecycle()
    val defaultMassUnit by viewModel.massUnit.collectAsStateWithLifecycle()
    val pendingMeal by viewModel.pendingMeal.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var selectedFood by remember { mutableStateOf<FoodEntity?>(null) }
    var showCustomFoodDialog by remember { mutableStateOf(false) }
    var aiItems by remember { mutableStateOf<List<VisionFoodItem>>(emptyList()) }
    var showAiSheet by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(query) {
        viewModel.searchFoods(query)
    }

    DisposableEffect(viewModel) {
        onDispose { viewModel.consumePendingMeal() }
    }

    val handleImage: (ByteArray) -> Unit = { bytes ->
        viewModel.estimateFood(bytes) { result ->
            result.onSuccess { estimate ->
                aiItems = estimate.items
                showAiSheet = true
            }
        }
    }

    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) {
            pendingCameraUri?.let { uri ->
                readCompressedImageBytes(context, uri)?.let(handleImage)
            }
        }
    }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let { readCompressedImageBytes(context, it)?.let(handleImage) }
    }

    val launchCamera: () -> Unit = {
        val directory = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(directory, "meal_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        pendingCameraUri = uri
        takePicture.launch(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("记录饮食") },
                actions = {
                    IconButton(onClick = launchCamera) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "AI 拍照识别")
                    }
                    IconButton(
                        onClick = {
                            pickImage.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
                            )
                        },
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = "从相册识别")
                    }
                },
            )
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(contentPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    placeholder = { Text("搜索食物，例如：馒头") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                )
            }

            item {
                Text(
                    text = "本地食物库优先；没有收录时可用 AI 拍照估算，或添加自定义食物。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            items(foods, key = { it.id }) { food ->
                FoodRow(
                    food = food,
                    energyUnit = energyUnit,
                    onClick = { selectedFood = food },
                )
            }

            item {
                TextButton(
                    onClick = { showCustomFoodDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("没有找到？添加自定义食物", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }

    selectedFood?.let { food ->
        ModalBottomSheet(onDismissRequest = { selectedFood = null }) {
            PortionSheet(
                food = food,
                energyUnit = energyUnit,
                defaultMassUnit = defaultMassUnit,
                initialMeal = pendingMeal ?: MealType.LUNCH,
                onConfirm = { grams, meal ->
                    viewModel.addFood(food, grams, meal)
                    selectedFood = null
                    viewModel.consumePendingMeal()
                    onSaved()
                },
            )
        }
    }

    if (showAiSheet && aiItems.isNotEmpty()) {
        ModalBottomSheet(onDismissRequest = { showAiSheet = false }) {
            AiEstimateSheet(
                items = aiItems,
                energyUnit = energyUnit,
                initialMeal = pendingMeal ?: MealType.LUNCH,
                onConfirm = { confirmed, meal ->
                    confirmed.forEach { entry ->
                        viewModel.addEstimatedFood(
                            item = entry.item,
                            grams = entry.grams,
                            kcalPer100g = entry.kcalPer100g,
                            mealType = meal,
                        )
                    }
                    showAiSheet = false
                    viewModel.consumePendingMeal()
                    onSaved()
                },
            )
        }
    }

    if (showCustomFoodDialog) {
        CustomFoodDialog(
            initialMeal = pendingMeal ?: MealType.LUNCH,
            onDismiss = { showCustomFoodDialog = false },
            onSubmit = { name, kcalPer100g, grams, protein, fat, carb, meal ->
                viewModel.addCustomFood(
                    name = name,
                    kcalPer100g = kcalPer100g,
                    grams = grams,
                    mealType = meal,
                    proteinPer100g = protein,
                    fatPer100g = fat,
                    carbPer100g = carb,
                )
                showCustomFoodDialog = false
                viewModel.consumePendingMeal()
                onSaved()
            },
        )
    }
}

@Composable
private fun FoodRow(
    food: FoodEntity,
    energyUnit: EnergyUnit,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(food.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = listOfNotNull(
                        food.category.takeIf { it.isNotBlank() },
                        food.aliases.takeIf { it.isNotBlank() },
                        food.portionLabel,
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${UnitFormatter.format(energyUnit.fromKcal(food.kcalPer100g))} " +
                    "${energyUnit.symbol}/100g",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun PortionSheet(
    food: FoodEntity,
    energyUnit: EnergyUnit,
    defaultMassUnit: MassUnit,
    initialMeal: MealType,
    onConfirm: (grams: Double, meal: MealType) -> Unit,
) {
    // 默认数量按「单位模式的换算值」填，保证初次打开时 grams 恰好等于 defaultPortionGrams。
    var amountText by remember(food.id) {
        mutableStateOf(
            when (defaultMassUnit) {
                MassUnit.GRAM -> "1"
                MassUnit.KILOGRAM ->
                    (food.defaultPortionGrams / 1000.0).toTrimmedString()
                MassUnit.OUNCE ->
                    (food.defaultPortionGrams / 28.349523125).toTrimmedString()
            },
        )
    }
    var unitMode by remember(food.id) {
        mutableStateOf(
            when (defaultMassUnit) {
                MassUnit.GRAM -> PortionUnitMode.PORTION
                MassUnit.KILOGRAM -> PortionUnitMode.KILOGRAM
                MassUnit.OUNCE -> PortionUnitMode.OUNCE
            },
        )
    }
    var meal by remember(food.id, initialMeal) { mutableStateOf(initialMeal) }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val grams = when (unitMode) {
        PortionUnitMode.PORTION -> food.defaultPortionGrams * amount
        PortionUnitMode.GRAM -> amount
        PortionUnitMode.KILOGRAM -> amount * 1000.0
        PortionUnitMode.OUNCE -> amount * 28.349523125
    }
    val kcal = food.kcalPer100g * grams / 100.0

    // 切换单位时把当前重量换算到新单位，否则 "1 份" 切成 "克" 会变成 1 克。
    val switchUnitMode: (PortionUnitMode) -> Unit = { next ->
        if (next != unitMode) {
            val currentGrams = grams
            amountText = when (next) {
                PortionUnitMode.PORTION ->
                    (currentGrams / food.defaultPortionGrams).toTrimmedString()
                PortionUnitMode.GRAM -> currentGrams.toTrimmedString()
                PortionUnitMode.KILOGRAM -> (currentGrams / 1000.0).toTrimmedString()
                PortionUnitMode.OUNCE -> (currentGrams / 28.349523125).toTrimmedString()
            }
            unitMode = next
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(food.name, style = MaterialTheme.typography.titleLarge)
        Text(
            text = "每 100 克 ${UnitFormatter.energy(food.kcalPer100g, energyUnit)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("数量") },
            singleLine = true,
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PortionUnitMode.entries.forEach { mode ->
                FilterChip(
                    selected = unitMode == mode,
                    onClick = { switchUnitMode(mode) },
                    label = {
                        Text(
                            if (mode == PortionUnitMode.PORTION) {
                                "份（${food.defaultPortionGrams.toInt()}g）"
                            } else {
                                mode.label
                            },
                        )
                    },
                )
            }
        }

        Text("餐次", style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MealType.entries.forEach { option ->
                FilterChip(
                    selected = meal == option,
                    onClick = { meal = option },
                    label = { Text(option.label()) },
                )
            }
        }

        SectionCard {
            MetricRow("换算重量", "${UnitFormatter.format(grams, 1)} g")
            MetricRow(
                label = "估算热量",
                value = "${UnitFormatter.format(energyUnit.fromKcal(kcal))} ${energyUnit.symbol}",
                emphasize = true,
            )
        }

        Button(
            onClick = { onConfirm(grams, meal) },
            modifier = Modifier.fillMaxWidth(),
            enabled = grams > 0.0,
        ) {
            Text("记录到${meal.label()}")
        }
    }
}

@Composable
private fun AiEstimateSheet(
    items: List<VisionFoodItem>,
    energyUnit: EnergyUnit,
    initialMeal: MealType,
    onConfirm: (List<ConfirmedEstimate>, MealType) -> Unit,
) {
    val editableItems = remember(items) { items.map { EditableEstimate(it) }.toMutableStateList() }
    var meal by remember(initialMeal) { mutableStateOf(initialMeal) }

    val confirmed = editableItems.mapNotNull { editable ->
        val grams = editable.gramsText.toDoubleOrNull() ?: 0.0
        val kcalPer100g = editable.kcalText.toDoubleOrNull() ?: 0.0
        if (editable.included && grams > 0.0 && kcalPer100g > 0.0) {
            ConfirmedEstimate(editable.item, grams, kcalPer100g)
        } else {
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("AI 识别结果", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "AI 只能估算份量，请在记录前核对重量和热量。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        editableItems.forEach { editable ->
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = editable.included,
                    onCheckedChange = { editable.included = it },
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(editable.item.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = buildString {
                            append("置信度 ${(editable.item.confidence * 100).toInt()}%")
                            if (editable.item.note.isNotBlank()) {
                                append(" · ${editable.item.note}")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(
                    value = editable.gramsText,
                    onValueChange = { editable.gramsText = it },
                    label = "重量",
                    modifier = Modifier.weight(1f),
                    suffix = "g",
                )
                NumberField(
                    value = editable.kcalText,
                    onValueChange = { editable.kcalText = it },
                    label = "每100克热量",
                    modifier = Modifier.weight(1f),
                    suffix = "kcal",
                )
            }
            Text(
                text = "该项热量：" + UnitFormatter.energy(
                    (editable.kcalText.toDoubleOrNull() ?: 0.0) *
                        (editable.gramsText.toDoubleOrNull() ?: 0.0) / 100.0,
                    energyUnit,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalDivider()
        Text("记录到哪一餐", style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MealType.entries.forEach { option ->
                FilterChip(
                    selected = meal == option,
                    onClick = { meal = option },
                    label = { Text(option.label()) },
                )
            }
        }

        Button(
            onClick = { onConfirm(confirmed, meal) },
            modifier = Modifier.fillMaxWidth(),
            enabled = confirmed.isNotEmpty(),
        ) {
            Text("记录选中项（${confirmed.size}）")
        }
    }
}

@Composable
private fun CustomFoodDialog(
    initialMeal: MealType,
    onDismiss: () -> Unit,
    onSubmit: (
        name: String,
        kcalPer100g: Double,
        grams: Double,
        proteinPer100g: Double,
        fatPer100g: Double,
        carbPer100g: Double,
        meal: MealType,
    ) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var kcalText by remember { mutableStateOf("") }
    var gramsText by remember { mutableStateOf("100") }
    var proteinText by remember { mutableStateOf("") }
    var fatText by remember { mutableStateOf("") }
    var carbText by remember { mutableStateOf("") }
    var meal by remember(initialMeal) { mutableStateOf(initialMeal) }

    val kcal = kcalText.toDoubleOrNull() ?: 0.0
    val grams = gramsText.toDoubleOrNull() ?: 0.0
    val protein = proteinText.toDoubleOrNull() ?: 0.0
    val fat = fatText.toDoubleOrNull() ?: 0.0
    val carb = carbText.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加自定义食物") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("食物名称") },
                    singleLine = true,
                )
                NumberField(
                    value = kcalText,
                    onValueChange = { kcalText = it },
                    label = "每 100 克热量",
                    suffix = "kcal",
                )
                NumberField(
                    value = gramsText,
                    onValueChange = { gramsText = it },
                    label = "本次食用重量",
                    suffix = "g",
                )
                Text(
                    text = "宏量营养（选填，每 100 克）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(
                        value = proteinText,
                        onValueChange = { proteinText = it },
                        label = "蛋白质",
                        modifier = Modifier.weight(1f),
                        suffix = "g",
                    )
                    NumberField(
                        value = fatText,
                        onValueChange = { fatText = it },
                        label = "脂肪",
                        modifier = Modifier.weight(1f),
                        suffix = "g",
                    )
                    NumberField(
                        value = carbText,
                        onValueChange = { carbText = it },
                        label = "碳水",
                        modifier = Modifier.weight(1f),
                        suffix = "g",
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MealType.entries.forEach { option ->
                        FilterChip(
                            selected = meal == option,
                            onClick = { meal = option },
                            label = { Text(option.label()) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(name.trim(), kcal, grams, protein, fat, carb, meal) },
                enabled = name.isNotBlank() && kcal > 0.0 && grams > 0.0,
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

/** 单边最大像素。相机原图直接 base64 上传容易顶到模型限制，也拖慢请求。 */
private const val MAX_IMAGE_EDGE_PX = 1280

private const val JPEG_QUALITY = 85

/**
 * 读取图片并压缩：
 * 1. 按 [MAX_IMAGE_EDGE_PX] 等比降采样（两轮 inSampleSize，避免一次性解码超大图 OOM）；
 * 2. 按 EXIF 方向旋正，否则竖拍照片传给模型会 sideways；
 * 3. 重新编码为 JPEG 并把原始 EXIF 元数据写回（含方向信息已被消费掉，故直接省略）。
 */
private fun readCompressedImageBytes(context: Context, uri: Uri): ByteArray? = runCatching {
    val resolver = context.contentResolver

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val opts = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_IMAGE_EDGE_PX)
    }
    val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        ?: return null

    val orientation = resolver.openInputStream(uri)?.use { readExifOrientation(it) }
        ?: ExifInterface.ORIENTATION_NORMAL
    val rotated = decoded.applyExifOrientation(orientation)

    ByteArrayOutputStream().use { out ->
        rotated.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        if (rotated !== decoded) rotated.recycle()
        decoded.recycle()
        out.toByteArray()
    }
}.getOrNull()

private fun calculateInSampleSize(width: Int, height: Int, maxEdge: Int): Int {
    var sample = 1
    while (width / (sample * 2) >= maxEdge || height / (sample * 2) >= maxEdge) {
        sample *= 2
    }
    return sample
}

private fun readExifOrientation(input: InputStream): Int {
    val exif = ExifInterface(input)
    return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
}

private fun Bitmap.applyExifOrientation(orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.postScale(-1f, 1f)
        }
        else -> return this
    }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

private fun Double.toTrimmedString(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()

