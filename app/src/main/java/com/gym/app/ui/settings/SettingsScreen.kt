package com.gym.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gym.app.data.repository.WorkoutRepository
import com.gym.app.ui.components.AnimationConfig
import com.gym.app.ui.components.FrostedBottomSheet
import com.gym.app.ui.components.LocaleStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val DarkBg = Color(0xFF3E2E23)
private val AccentOrange = Color(0xFFF2994A)
private val AccentGreen = Color(0xFF4CAF50)
private val LightGray = Color(0xFFF5F5F5)
private val MediumGray = Color(0xFF9E9E9E)
private val WhiteColor = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    workoutRepository: WorkoutRepository,
    scope: CoroutineScope,
    onBack: () -> Unit,
    sheetShowing: androidx.compose.runtime.MutableState<Boolean> = mutableStateOf(false),
) {
    // 从 DataStore 读取当前值作为初始状态
    val savedLanguage by settings.language.collectAsState(initial = "zh")
    val savedWeightUnit by settings.weightUnit.collectAsState(initial = "kg")
    val savedGender by settings.gender.collectAsState(initial = "")
    val savedHeight by settings.heightCm.collectAsState(initial = 0f)
    val savedWeight by settings.bodyWeightKg.collectAsState(initial = 0f)
    // 功能开关即时生效（不参与下方「应用保存」）
    val savedShowImageImport by settings.showImageImport.collectAsState(initial = true)

    // 本地编辑状态（未保存前不生效）
    var editLanguage by remember { mutableStateOf(savedLanguage) }
    var editWeightUnit by remember { mutableStateOf(savedWeightUnit) }
    var editGender by remember { mutableStateOf("") }
    var editHeight by remember { mutableStateOf("") }
    var editWeight by remember { mutableStateOf("") }

    // 首次加载时同步（DataStore 异步加载）
    if (editLanguage != savedLanguage && editLanguage == "zh") {
        editLanguage = savedLanguage
    }
    if (editWeightUnit != savedWeightUnit && editWeightUnit == "kg") {
        editWeightUnit = savedWeightUnit
    }
    if (editGender.isEmpty() && savedGender.isNotEmpty()) editGender = savedGender
    if (editHeight.isEmpty() && savedHeight > 0f) editHeight = formatNum(savedHeight)
    // 体重界面单位为斤，存储为 kg
    if (editWeight.isEmpty() && savedWeight > 0f) editWeight = formatNum(savedWeight * 2f)

    // 是否有未保存的修改
    val bodyHasChanges = editGender != savedGender ||
        (editHeight.toFloatOrNull() ?: 0f) != savedHeight ||
        ((editWeight.toFloatOrNull() ?: 0f) / 2f) != savedWeight
    val hasChanges = editLanguage != savedLanguage ||
        editWeightUnit != savedWeightUnit ||
        bodyHasChanges

    // 体重历史折线图弹层
    var showWeightHistory by remember { mutableStateOf(false) }
    var sheetContentVisible by remember { mutableStateOf(false) }
    var weightPoints by remember { mutableStateOf<List<WeightPoint>>(emptyList()) }

    LaunchedEffect(showWeightHistory) {
        sheetShowing.value = showWeightHistory
        if (showWeightHistory) {
            // 体重快照来自每次训练开始时确认的值（同一天多次训练取最后一次）；界面单位为斤
            weightPoints = workoutRepository.getAllSessions().first()
                .filter { it.bodyWeightKg > 0f }
                .sortedBy { it.date }
                .groupBy { it.date }
                .map { (date, sessions) -> WeightPoint(date, sessions.first().bodyWeightKg * 2f) }
            delay(250L)
            sheetContentVisible = true
        } else {
            sheetContentVisible = false
        }
    }

    // 未保存退出提醒对话框
    var showExitDialog by remember { mutableStateOf(false) }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("未保存的修改", fontWeight = FontWeight.Bold) },
            text = { Text("你有未保存的设置修改，确定要放弃更改并退出吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onBack()
                }) {
                    Text("放弃更改", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("继续编辑")
                }
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(LocaleStrings.settingsTitle, color = WhiteColor, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (hasChanges) showExitDialog = true
                            else onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = WhiteColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg),
                )
            },
            containerColor = LightGray,
            bottomBar = {
                // 保存按钮（仅在有修改时启用）
                Button(
                    onClick = {
                        scope.launch {
                            settings.setLanguage(editLanguage)
                            settings.setWeightUnit(editWeightUnit)
                            settings.setBodyInfo(
                                gender = editGender.ifEmpty { null },
                                heightCm = editHeight.toFloatOrNull(),
                                // 界面输入为斤，存储为 kg
                                weightKg = editWeight.toFloatOrNull()?.let { it / 2f },
                            )
                        }
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    enabled = hasChanges,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                        disabledContainerColor = MediumGray.copy(alpha = 0.3f),
                    ),
                ) {
                    Text(
                        "应用保存",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (hasChanges) WhiteColor else MediumGray,
                    )
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 语言设置
                SettingsCard(LocaleStrings.language) {
                    SelectableRow(LocaleStrings.langZh, editLanguage == "zh") {
                        editLanguage = "zh"
                    }
                    SelectableRow(LocaleStrings.langEn, editLanguage == "en") {
                        editLanguage = "en"
                    }
                }

                // 重量单位
                SettingsCard(LocaleStrings.weightUnit) {
                    SelectableRow(LocaleStrings.unitKg, editWeightUnit == "kg") {
                        editWeightUnit = "kg"
                    }
                    SelectableRow(LocaleStrings.unitLb, editWeightUnit == "lb") {
                        editWeightUnit = "lb"
                    }
                }

                // 身体数据
                SettingsCard(LocaleStrings.bodyInfo) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(LocaleStrings.gender, fontSize = 14.sp, color = MediumGray, modifier = Modifier.width(64.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BodyChip(LocaleStrings.genderMale, editGender == "male") { editGender = "male" }
                            BodyChip(LocaleStrings.genderFemale, editGender == "female") { editGender = "female" }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(LocaleStrings.height, fontSize = 14.sp, color = MediumGray, modifier = Modifier.width(64.dp))
                        NumberField(editHeight, "cm") { editHeight = it }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(LocaleStrings.weightInfo, fontSize = 14.sp, color = MediumGray, modifier = Modifier.width(64.dp))
                        NumberField(editWeight, LocaleStrings.weightUnitJin) { editWeight = it }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    // 详情入口：体重历史折线图
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showWeightHistory = true }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            LocaleStrings.weightHistoryTitle,
                            fontSize = 13.sp,
                            color = AccentOrange,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MediumGray, modifier = Modifier.size(18.dp))
                    }
                }

                // 功能开关（即时生效，不参与「应用保存」）
                SettingsCard(LocaleStrings.settingsFeatures) {
                    SwitchRow(
                        label = LocaleStrings.settingsShowImageImport,
                        hint = LocaleStrings.settingsShowImageImportHint,
                        checked = savedShowImageImport,
                        onCheckedChange = { enabled ->
                            scope.launch { settings.setShowImageImport(enabled) }
                        },
                    )
                }
            }
        }

        // 体重历史折线图（磨砂玻璃底部滑出）
        if (sheetContentVisible && showWeightHistory) {
            FrostedBottomSheet(
                visible = true,
                onDismiss = { showWeightHistory = false },
            ) {
                WeightHistoryContent(weightPoints)
            }
        }
    } // Box end
}

/** 体重历史数据点（来自训练开始时确认的快照）。 */
private data class WeightPoint(
    val date: String,
    val weight: Float,
)

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkBg)
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SelectableRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) AccentOrange.copy(alpha = 0.08f) else Color.Transparent,
        animationSpec = tween(AnimationConfig.Selector.bgColorMs),
        label = "selBg",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) AccentOrange else DarkBg,
        animationSpec = tween(AnimationConfig.Selector.textColorMs),
        label = "selText",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, color = textColor, modifier = Modifier.weight(1f))
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(AnimationConfig.Selector.checkFadeInMs)) + scaleIn(spring(dampingRatio = AnimationConfig.Press.springDamping, stiffness = AnimationConfig.Press.springStiff)),
            exit = fadeOut(tween(AnimationConfig.Selector.checkFadeOutMs)),
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    hint: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, color = DarkBg)
            Spacer(modifier = Modifier.height(2.dp))
            Text(hint, fontSize = 12.sp, color = MediumGray)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = AccentOrange,
                checkedThumbColor = WhiteColor,
                uncheckedThumbColor = WhiteColor,
            ),
        )
    }
}

@Composable
private fun BodyChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 13.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AccentOrange.copy(alpha = 0.12f),
            selectedLabelColor = AccentOrange,
        ),
        shape = RoundedCornerShape(20.dp),
    )
}

@Composable
private fun NumberField(value: String, unit: String, onValueChanged: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            // 仅允许数字和一个小数点
            onValueChanged(input.filter { it.isDigit() || it == '.' })
        },
        modifier = Modifier.fillMaxWidth(),
        suffix = { Text(unit, fontSize = 12.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = TextStyle(fontSize = 14.sp),
        shape = RoundedCornerShape(10.dp),
    )
}

// ═══════════════════════════════════════════════════════════════
// 体重历史折线图（数据源：训练开始时确认的体重快照）
// ═══════════════════════════════════════════════════════════════

@Composable
private fun WeightHistoryContent(points: List<WeightPoint>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(LocaleStrings.weightHistoryTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkBg)
        Spacer(modifier = Modifier.height(4.dp))
        Text(LocaleStrings.weightHistoryHint, fontSize = 12.sp, color = MediumGray)
        Spacer(modifier = Modifier.height(16.dp))
        when {
            points.size >= 2 -> {
                WeightHistoryChart(points)
                Spacer(modifier = Modifier.height(8.dp))
                val latest = points.last()
                Text(
                    "${LocaleStrings.weightInfo} ${formatNum(latest.weight)} ${LocaleStrings.weightUnitJin} · ${latest.date}",
                    fontSize = 13.sp,
                    color = AccentOrange,
                    fontWeight = FontWeight.Medium,
                )
            }
            points.size == 1 -> {
                val only = points.first()
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "${LocaleStrings.weightInfo} ${formatNum(only.weight)} ${LocaleStrings.weightUnitJin} · ${only.date}",
                        fontSize = 15.sp,
                        color = DarkBg,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Text(LocaleStrings.weightHistoryMoreData, fontSize = 12.sp, color = MediumGray)
            }
            else -> {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text(LocaleStrings.weightHistoryEmpty, fontSize = 13.sp, color = MediumGray)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun WeightHistoryChart(points: List<WeightPoint>) {
    val lineColor = AccentOrange
    val gridColor = Color(0xFFE0E0E0)
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))
    }

    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        if (points.size < 2) return@Canvas
        val values = points.map { it.weight }
        val minV = values.min(); val maxV = values.max()
        val range = if (maxV == minV) 1f else maxV - minV
        val padL = 48f; val padR = 16f; val padT = 12f; val padB = 28f
        val cw = size.width - padL - padR; val ch = size.height - padT - padB

        // 1. 网格 + Y 轴数值
        for (i in 0..3) {
            val frac = i / 3f; val y = padT + (1f - frac) * ch
            drawLine(gridColor, Offset(padL, y), Offset(size.width - padR, y), 1f)
            drawContext.canvas.nativeCanvas.drawText(
                formatNum(minV + frac * range), 4f, y + 4f,
                android.graphics.Paint().apply { color = 0xFF9E9E9E.toInt(); textSize = 20f; isAntiAlias = true },
            )
        }

        // 2. 面积 + 折线 + 圆点（左侧向右渐显动画）
        val clipX = padL + animProgress.value * cw
        drawContext.canvas.save()
        drawContext.canvas.clipRect(0f, 0f, clipX, size.height)

        val areaPath = Path().apply {
            moveTo(padL, padT + ch)
            values.forEachIndexed { i, v ->
                val x = padL + (i.toFloat() / (values.size - 1).coerceAtLeast(1)) * cw
                lineTo(x, padT + (1f - (v - minV) / range) * ch)
            }
            lineTo(padL + cw, padT + ch); close()
        }
        drawPath(areaPath, lineColor.copy(alpha = 0.08f))

        if (values.size >= 2) {
            val lp = Path().apply {
                values.forEachIndexed { i, v ->
                    val x = padL + (i.toFloat() / (values.size - 1)) * cw
                    if (i == 0) moveTo(x, padT + (1f - (v - minV) / range) * ch)
                    else lineTo(x, padT + (1f - (v - minV) / range) * ch)
                }
            }
            drawPath(lp, lineColor, style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        values.forEachIndexed { i, v ->
            val x = padL + (i.toFloat() / (values.size - 1).coerceAtLeast(1)) * cw
            val y = padT + (1f - (v - minV) / range) * ch
            val isLast = i == values.lastIndex
            drawCircle(lineColor.copy(alpha = 0.35f), if (isLast) 11f else 8f, Offset(x, y))
            drawCircle(lineColor, if (isLast) 8f else 5f, Offset(x, y))
            drawCircle(Color.White, if (isLast) 5f else 3f, Offset(x, y))
        }
        drawContext.canvas.restore()

        // 3. X 轴日期
        points.forEachIndexed { i, p ->
            if (points.size <= 6 || i % 2 == 0 || i == points.lastIndex) {
                val x = padL + (i.toFloat() / (points.size - 1).coerceAtLeast(1)) * cw
                drawContext.canvas.nativeCanvas.drawText(
                    p.date.takeLast(5), x - 14f, size.height - 2f,
                    android.graphics.Paint().apply { color = 0xFF9E9E9E.toInt(); textSize = 18f; isAntiAlias = true },
                )
            }
        }
    }
}

private fun formatNum(v: Float): String =
    if (v == v.toInt().toFloat()) v.toInt().toString()
    else java.text.DecimalFormat("#.#").format(v.toDouble())
