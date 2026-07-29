package com.gym.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DarkBg = Color(0xFF1A1A2E)
private val AccentBlue = Color(0xFF2196F3)
private val MediumGray = Color(0xFF9E9E9E)
private val WhiteColor = Color.White

data class BottomTab(
    val label: String,
    val route: String,
)

@Composable
fun GymBottomNavBar(
    tabs: List<BottomTab>,
    selectedRoute: String?,
    onTabSelected: (String) -> Unit,
    onSettingsClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .shadow(10.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(DarkBg)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            // 完美圆形 Tab（等宽高）
            tabs.forEach { tab ->
                val isSelected = selectedRoute == tab.route
                Box(
                    modifier = Modifier
                        .size(44.dp) // 固定等宽高 = 完美圆形
                        .clip(CircleShape)
                        .background(if (isSelected) AccentBlue else Color.Transparent)
                        .clickable { onTabSelected(tab.route) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        tab.label,
                        color = if (isSelected) WhiteColor else MediumGray,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }

            // 右侧圆形设置按钮
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MediumGray.copy(alpha = 0.2f))
                    .clickable { onSettingsClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = LocaleStrings.settingsTitle,
                    tint = WhiteColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
