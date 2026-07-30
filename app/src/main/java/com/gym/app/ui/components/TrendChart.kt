package com.gym.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DarkBg = Color(0xFF1A1A2E)
private val AccentBlue = Color(0xFF2196F3)
private val AccentGreen = Color(0xFF4CAF50)
private val OrangeColor = Color(0xFFFF7043)
private val MediumGray = Color(0xFF9E9E9E)
private val WhiteColor = Color.White

@Composable
fun TrendChartCard(
    exerciseName: String,
    trendPoints: List<TrendPoint>,
    currentPoint: TrendPoint?,
    progressResult: ProgressResult?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 标题行
            Text(
                exerciseName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DarkBg,
            )

            // 进步/退步指示器
            if (progressResult != null && !progressResult.isFirstTime) {
                Spacer(modifier = Modifier.height(8.dp))
                ProgressIndicatorRow(progressResult)
            }

            // 趋势图
            if (trendPoints.size >= 2) {
                Spacer(modifier = Modifier.height(10.dp))
                WeightTrendLineChart(
                    points = trendPoints,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                )
            } else if (trendPoints.size == 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Complete another workout to see trends",
                    fontSize = 12.sp,
                    color = MediumGray,
                )
            }

            // 当前数据摘要
            if (currentPoint != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    DataChip("Max", "${currentPoint.maxWeight.toInt()}kg", AccentBlue)
                    DataChip("Reps", "${currentPoint.maxReps}", AccentGreen)
                    DataChip("Volume", "${currentPoint.totalVolume.toInt()}kg", OrangeColor)
                }
            }
        }
    }
}

@Composable
private fun ProgressIndicatorRow(result: ProgressResult) {
    Row(modifier = Modifier.fillMaxWidth()) {
        if (result.isFirstTime) {
            Text("🎉 First time!", fontSize = 13.sp, color = AccentGreen, fontWeight = FontWeight.Bold)
        } else {
            TrendBadge("Weight", result.weightTrend)
            TrendBadge("Reps", result.repsTrend)
            TrendBadge("Volume", result.volumeTrend)
        }
    }
}

@Composable
private fun TrendBadge(label: String, trend: Int) {
    val (symbol, color) = when {
        trend > 0 -> "↑" to AccentGreen
        trend < 0 -> "↓" to OrangeColor
        else -> "→" to MediumGray
    }

    Box(
        modifier = Modifier
            .padding(end = 12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            "$symbol $label",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = color,
        )
    }
}

@Composable
private fun DataChip(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier.padding(end = 16.dp),
    ) {
        Text(label, fontSize = 11.sp, color = MediumGray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun WeightTrendLineChart(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier,
) {
    val lineColor = AccentBlue
    val dotColor = AccentBlue
    val gridColor = Color(0xFFE0E0E0)

    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas

        val values = points.map { it.maxWeight.toFloat() }
        val minVal = values.min()
        val maxVal = values.max()
        val range = if (maxVal == minVal) 1f else maxVal - minVal

        val paddingLeft = 40f
        val paddingRight = 16f
        val paddingTop = 12f
        val paddingBottom = 24f

        val chartWidth = size.width - paddingLeft - paddingRight
        val chartHeight = size.height - paddingTop - paddingBottom

        // 计算每个点的坐标
        val pointOffsets = values.mapIndexed { index, value ->
            val x = paddingLeft + (index.toFloat() / (values.size - 1)) * chartWidth
            val y = paddingTop + (1f - (value - minVal) / range) * chartHeight
            Offset(x, y)
        }

        // 绘制网格线 + Y轴标签
        val gridLines = 4
        for (i in 0..gridLines) {
            val fraction = i.toFloat() / gridLines
            val y = paddingTop + (1f - fraction) * chartHeight
            val labelValue = minVal + fraction * range

            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(size.width - paddingRight, y),
                strokeWidth = 1f,
            )

            // Y轴标签
            drawContext.canvas.nativeCanvas.drawText(
                labelValue.toInt().toString(),
                4f,
                y + 4f,
                android.graphics.Paint().apply {
                    color = 0xFF9E9E9E.toInt()
                    textSize = 22f
                    isAntiAlias = true
                },
            )
        }

        // 绘制折线
        if (pointOffsets.size >= 2) {
            val linePath = Path().apply {
                moveTo(pointOffsets.first().x, pointOffsets.first().y)
                for (i in 1 until pointOffsets.size) {
                    lineTo(pointOffsets[i].x, pointOffsets[i].y)
                }
            }
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(
                    width = 3f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }

        // 绘制数据点
        for (offset in pointOffsets) {
            drawCircle(
                color = dotColor,
                radius = 5f,
                center = offset,
            )
            drawCircle(
                color = Color.White,
                radius = 3f,
                center = offset,
            )
        }
    }
}
