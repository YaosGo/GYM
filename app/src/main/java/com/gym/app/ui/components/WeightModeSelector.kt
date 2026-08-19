package com.gym.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.gym.app.data.entity.Exercise

private val WeightModeOrange = Color(0xFFFF7043)

/**
 * 重量记录方式三态切换（普通/助力/负重）。
 * 用于编辑/创建计划页的动作行；动作库不展示（标记入口已收敛到计划编辑流程）。
 */
@Composable
fun WeightModeSelectorRow(
    currentMode: Int,
    onModeChanged: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        WeightModeDot(Exercise.WEIGHT_MODE_NORMAL, LocaleStrings.weightModeNormal, currentMode, onModeChanged)
        WeightModeDot(Exercise.WEIGHT_MODE_ASSISTED, LocaleStrings.weightModeAssisted, currentMode, onModeChanged)
        WeightModeDot(Exercise.WEIGHT_MODE_WEIGHTED, LocaleStrings.weightModeWeighted, currentMode, onModeChanged)
    }
}

/** 助力/负重只读标签（查看模式、训练完成页等场景）。 */
@Composable
fun WeightModeBadge(weightMode: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(WeightModeOrange.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
        Text(
            if (weightMode == Exercise.WEIGHT_MODE_ASSISTED) LocaleStrings.weightModeAssisted
            else LocaleStrings.weightModeWeighted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = WeightModeOrange,
        )
    }
}

@Composable
private fun WeightModeDot(
    mode: Int,
    label: String,
    currentMode: Int,
    onModeChanged: (Int) -> Unit,
) {
    val selected = currentMode == mode
    val modeColor = if (mode == Exercise.WEIGHT_MODE_NORMAL) Color(0xFF9E9E9E) else WeightModeOrange
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) modeColor.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onModeChanged(mode) }
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) modeColor else Color(0xFF9E9E9E).copy(alpha = 0.7f),
        )
    }
}
