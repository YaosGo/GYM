package com.gym.app.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val DarkBg = Color(0xFF1A1A2E)
private val AccentBlue = Color(0xFF2196F3)
private val AccentGreen = Color(0xFF4CAF50)
private val LightGray = Color(0xFFF5F5F5)
private val MediumGray = Color(0xFF9E9E9E)
private val WhiteColor = Color.White

private val dayHeaders = listOf("一", "二", "三", "四", "五", "六", "日")

@Composable
fun WorkoutCalendar(
    year: Int,
    month: Int, // 0-based (Calendar.JANUARY = 0)
    workoutCounts: Map<String, Int>,
    selectedDate: String?,
    onDateSelected: (String) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val monthNames = listOf(
        "1月", "2月", "3月", "4月", "5月", "6月",
        "7月", "8月", "9月", "10月", "11月", "12月"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WhiteColor)
            .padding(16.dp),
    ) {
        // 月份头部
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "上个月",
                    tint = DarkBg,
                )
            }
            Text(
                "${monthNames[month]} $year",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkBg,
            )
            IconButton(onClick = onNextMonth) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "下个月",
                    tint = DarkBg,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 星期头
        Row(modifier = Modifier.fillMaxWidth()) {
            dayHeaders.forEach { day ->
                Text(
                    day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MediumGray,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 日期网格
        val cal = Calendar.getInstance()
        cal.set(year, month, 1)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sunday=1
        // 调整为周一=1
        val startOffset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = Calendar.getInstance()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(today.time)

        val totalCells = startOffset + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - startOffset + 1

                    if (dayNum in 1..daysInMonth) {
                        val dateStr = "${year}-${(month + 1).toString().padStart(2, '0')}-${dayNum.toString().padStart(2, '0')}"
                        val workoutCount = workoutCounts[dateStr] ?: 0
                        val isToday = dateStr == todayStr
                        val isSelected = dateStr == selectedDate

                        CalendarDay(
                            day = dayNum,
                            workoutCount = workoutCount,
                            isToday = isToday,
                            isSelected = isSelected,
                            onClick = { onDateSelected(dateStr) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        // 空白占位
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            if (row < rows - 1) Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun CalendarDay(
    day: Int,
    workoutCount: Int,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val outerSize = 40
    val innerSize = 30
    val dotColor = Color(0xFF4CAF50)

    Box(
        modifier = modifier
            .size(outerSize.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (workoutCount > 0) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val ringRadius = size.width / 2f - 2.dp.toPx()
                val dotRadius = 2.5.dp.toPx()
                // 最多 12 个点，从 6 点钟方向逆时针排列，每个点位于整点时刻
                val count = workoutCount.coerceAtMost(12)
                val angleStep = (2.0 * Math.PI) / 12.0
                for (i in 0 until count) {
                    // 6 点钟 = -PI/2，逆时针 = 角度增加（6→7→8→...→5）
                    val angle = -Math.PI / 2 + angleStep * i
                    val dx = cx + ringRadius * cos(angle).toFloat()
                    val dy = cy + ringRadius * sin(angle).toFloat()
                    drawCircle(dotColor, dotRadius, Offset(dx, dy))
                }
            }
        }

        // 蓝色/数字圆
        Box(
            modifier = Modifier
                .size(innerSize.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isSelected -> AccentBlue
                        isToday -> AccentBlue.copy(alpha = 0.15f)
                        else -> Color.Transparent
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$day",
                fontSize = 13.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> WhiteColor
                    isToday -> AccentBlue
                    else -> DarkBg
                },
            )
        }
    }
}
