package com.gym.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.sqrt

private val DarkBg = Color(0xFF1A1A2E)
private val AccentBlue = Color(0xFF2196F3)
private val AccentGreen = Color(0xFF4CAF50)
private val OrangeColor = Color(0xFFFF7043)
private val MediumGray = Color(0xFF9E9E9E)
private val WhiteColor = Color.White

private val LineVolume = AccentBlue
private val LineSets = AccentGreen
private val LineReps = OrangeColor

// ═══════════════════════════════════════════════════════════════
// 训练完成页：每个动作一张图，三条线可切换
// ═══════════════════════════════════════════════════════════════

@Composable
fun ExerciseTrendCard(
    exerciseName: String,
    trendPoints: List<TrendPoint>,
    currentVolume: Float = 0f,
    currentSets: Int = 0,
    currentReps: Int = 0,
) {
    val points = trendPoints.takeLast(5)
    var showVolume by remember { mutableStateOf(true) }
    var showSets by remember { mutableStateOf(true) }
    var showReps by remember { mutableStateOf(true) }
    // 选中的折点索引，默认最后一个（本次）
    var selectedIdx by remember { mutableIntStateOf((points.size - 1).coerceAtLeast(0)) }
    // 同步：如果 points 变化，重置选中为最后一个
    if (selectedIdx >= points.size) selectedIdx = (points.size - 1).coerceAtLeast(0)

    val sel = points.getOrNull(selectedIdx)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(exerciseName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkBg)

            if (points.size >= 2) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ToggleChip("总重量", LineVolume, showVolume) { showVolume = !showVolume }
                    ToggleChip("组数", LineSets, showSets) { showSets = !showSets }
                    ToggleChip("次数", LineReps, showReps) { showReps = !showReps }
                }
                Spacer(modifier = Modifier.height(8.dp))
                MultiLineChart(
                    points = points,
                    showVolume = showVolume,
                    showSets = showSets,
                    showReps = showReps,
                    selectedIndex = selectedIdx,
                    onPointClick = { selectedIdx = it },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                // 选中点日期
                if (sel != null) {
                    Text(sel.date, fontSize = 11.sp, color = MediumGray)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DataChip("总重量", formatVolume(sel?.totalVolume ?: currentVolume), LineVolume)
                DataChip("组数", "${sel?.sets ?: currentSets}", LineSets)
                DataChip("次数", "${sel?.maxReps ?: currentReps}", LineReps)
            }
        }
    }
}

@Composable
private fun ToggleChip(label: String, color: Color, active: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) color.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (active) color else Color.LightGray))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = if (active) color else MediumGray)
    }
}

@Composable
private fun MultiLineChart(
    points: List<TrendPoint>,
    showVolume: Boolean,
    showSets: Boolean,
    showReps: Boolean,
    selectedIndex: Int = points.lastIndex,
    onPointClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val gridColor = Color(0xFFE0E0E0)
    val valuesVol = points.map { it.totalVolume }
    val valuesSets = points.map { it.sets.toFloat() }
    val valuesReps = points.map { it.maxReps.toFloat() }

    Canvas(
        modifier = modifier.pointerInput(points) {
            detectTapGestures { tapOffset ->
                val allVals = mutableListOf<Float>()
                if (showVolume) allVals.addAll(valuesVol)
                if (showSets) allVals.addAll(valuesSets)
                if (showReps) allVals.addAll(valuesReps)
                if (allVals.isEmpty() || points.size < 2) return@detectTapGestures
                val padL = 40f; val padR = 16f
                val cw = size.width - padL - padR
                var closest = -1; var closestDist = Float.MAX_VALUE
                for (i in points.indices) {
                    val x = padL + (i.toFloat() / (points.size - 1).coerceAtLeast(1)) * cw
                    val dist = abs(tapOffset.x - x)
                    if (dist < closestDist && dist < 48f) { closest = i; closestDist = dist }
                }
                if (closest >= 0) onPointClick(closest)
            }
        }
    ) {
        if (points.size < 2) return@Canvas

        val allValues = mutableListOf<Float>()
        if (showVolume) allValues.addAll(valuesVol)
        if (showSets) allValues.addAll(valuesSets)
        if (showReps) allValues.addAll(valuesReps)
        if (allValues.isEmpty()) return@Canvas

        val minV = allValues.min(); val maxV = allValues.max()
        val range = if (maxV == minV) 1f else maxV - minV

        val padL = 40f; val padR = 16f; val padT = 12f; val padB = 24f
        val cw = size.width - padL - padR; val ch = size.height - padT - padB

        // grid
        for (i in 0..3) {
            val frac = i / 3f; val y = padT + (1f - frac) * ch
            drawLine(gridColor, Offset(padL, y), Offset(size.width - padR, y), 1f)
            drawContext.canvas.nativeCanvas.drawText(
                (minV + frac * range).toInt().toString(), 4f, y + 4f,
                android.graphics.Paint().apply { color = 0xFF9E9E9E.toInt(); textSize = 20f; isAntiAlias = true },
            )
        }

        fun drawLineV(values: List<Float>, color: Color) {
            if (values.size < 2) return
            val path = Path()
            values.forEachIndexed { i, v ->
                val x = padL + (i.toFloat() / (values.size - 1)) * cw
                val y = padT + (1f - (v - minV) / range) * ch
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            // 粗线
            drawPath(path, color, style = Stroke(4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            // 折点
            values.forEachIndexed { i, v ->
                val x = padL + (i.toFloat() / (values.size - 1)) * cw
                val y = padT + (1f - (v - minV) / range) * ch
                val isLast = i == values.lastIndex
                val isSel = i == selectedIndex
                // 选中光晕
                if (isSel) drawCircle(color.copy(alpha = 0.25f), if (isLast) 16f else 12f, Offset(x, y))
                // 外圈
                drawCircle(color.copy(alpha = 0.35f), if (isLast) 11f else 8f, Offset(x, y))
                // 实心
                drawCircle(color, if (isLast) 8f else if (isSel) 6f else 5f, Offset(x, y))
                // 白芯
                drawCircle(Color.White, if (isLast) 5f else 3f, Offset(x, y))
            }
        }

        if (showVolume) drawLineV(valuesVol, LineVolume)
        if (showSets) drawLineV(valuesSets, LineSets)
        if (showReps) drawLineV(valuesReps, LineReps)

        // X axis
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

// ═══════════════════════════════════════════════════════════════
// 查看计划页：单线总重量折线 + 点击切换百分比
// ═══════════════════════════════════════════════════════════════

@Composable
fun PlanTrendCard(trendPoints: List<TrendPoint>) {
    val points = trendPoints.takeLast(5)
    var selectedIndex by remember { mutableIntStateOf(points.lastIndex.coerceAtLeast(0)) }
    if (selectedIndex >= points.size) selectedIndex = (points.size - 1).coerceAtLeast(0)

    val changePercent = if (points.size >= 2 && selectedIndex > 0 && selectedIndex < points.size) {
        val prev = points[selectedIndex - 1].totalVolume
        val curr = points[selectedIndex].totalVolume
        if (prev > 0) ((curr - prev) / prev * 100f).toInt() else 0
    } else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("训练成果趋势", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkBg)

            if (points.size >= 2) {
                Spacer(modifier = Modifier.height(6.dp))
                val (symbol, pctColor) = when {
                    changePercent > 0 -> "↑" to AccentGreen
                    changePercent < 0 -> "↓" to OrangeColor
                    else -> "→" to MediumGray
                }
                val selDate = points[selectedIndex].date.takeLast(5)
                Text(
                    "$selDate  $symbol${abs(changePercent)}%",
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = pctColor,
                )
            }

            if (points.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                var tapInfo by remember { mutableStateOf("") }
                // 可点击的 Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .pointerInput(points) {
                            detectTapGestures { tapOffset ->
                                val values = points.map { it.totalVolume }
                                val minV = values.min(); val maxV = values.max()
                                val range = if (maxV == minV) 1f else maxV - minV
                                val padL = 44f; val padR = 16f
                                val cw = size.width - padL - padR
                                var closest = selectedIndex
                                var closestDist = Float.MAX_VALUE
                                values.forEachIndexed { i, _ ->
                                    val x = padL + (i.toFloat() / (values.size - 1).coerceAtLeast(1)) * cw
                                    val dist = abs(tapOffset.x - x)
                                    if (dist < closestDist && dist < 40f) { closest = i; closestDist = dist }
                                }
                                if (closestDist < 40f) selectedIndex = closest
                            }
                        }
                ) {
                    drawPlanLine(points, selectedIndex)
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Text("暂无训练数据", fontSize = 12.sp, color = MediumGray)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Canvas 绘制函数（PlanTrendCard 用）
// ═══════════════════════════════════════════════════════════════

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPlanLine(
    points: List<TrendPoint>,
    selectedIndex: Int,
) {
    if (points.isEmpty()) return
    val lineColor = AccentBlue
    val gridColor = Color(0xFFE0E0E0)

    val values = points.map { it.totalVolume }
    val minV = values.min(); val maxV = values.max()
    val range = if (maxV == minV) 1f else maxV - minV

    val padL = 44f; val padR = 16f; val padT = 12f; val padB = 28f
    val cw = size.width - padL - padR; val ch = size.height - padT - padB

    // grid
    for (i in 0..3) {
        val frac = i / 3f; val y = padT + (1f - frac) * ch
        drawLine(gridColor, Offset(padL, y), Offset(size.width - padR, y), 1f)
        drawContext.canvas.nativeCanvas.drawText(
            (minV + frac * range).toInt().toString(), 4f, y + 4f,
            android.graphics.Paint().apply { color = 0xFF9E9E9E.toInt(); textSize = 20f; isAntiAlias = true },
        )
    }

    // area fill
    val areaPath = Path().apply {
        moveTo(padL, padT + ch)
        values.forEachIndexed { i, v ->
            val x = padL + (i.toFloat() / (values.size - 1).coerceAtLeast(1)) * cw
            lineTo(x, padT + (1f - (v - minV) / range) * ch)
        }
        lineTo(padL + cw, padT + ch); close()
    }
    drawPath(areaPath, lineColor.copy(alpha = 0.08f))

    // line
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

    // points — 最后一个最大，选中有光晕
    values.forEachIndexed { i, v ->
        val x = padL + (i.toFloat() / (values.size - 1).coerceAtLeast(1)) * cw
        val y = padT + (1f - (v - minV) / range) * ch
        val isLast = i == values.lastIndex
        val isSel = i == selectedIndex
        // 选中光晕
        if (isSel) drawCircle(lineColor.copy(alpha = 0.25f), if (isLast) 18f else 14f, Offset(x, y))
        // 外圈
        drawCircle(lineColor.copy(alpha = 0.35f), if (isLast) 12f else 8f, Offset(x, y))
        // 实心
        drawCircle(lineColor, if (isLast) 8f else if (isSel) 6f else 5f, Offset(x, y))
        // 白芯
        drawCircle(Color.White, if (isLast) 5f else 3f, Offset(x, y))
    }

    // X axis dates
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

// ═══════════════════════════════════════════════════════════════
// 工具
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DataChip(label: String, value: String, color: Color) {
    Column(modifier = Modifier.padding(end = 8.dp)) {
        Text(label, fontSize = 10.sp, color = MediumGray)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

private fun formatVolume(v: Float): String {
    val d = v.toDouble()
    return if (d < 1000) d.toInt().toString() else "${"%.1f".format(d / 1000)}k"
}
