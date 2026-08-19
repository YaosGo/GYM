package com.gym.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DarkBg = Color(0xFF3E2E23)
private val AccentOrange = Color(0xFFF2994A)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(48.dp)
            .shadow(12.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(DarkBg)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        tabs.forEach { tab ->
            val isSelected = selectedRoute == tab.route

            val bgColor by animateColorAsState(
                targetValue = if (isSelected) AccentOrange else Color.Transparent,
                animationSpec = tween(AnimationConfig.NavBar.tabColorMs),
                label = "tabBg",
            )
            val scale by animateFloatAsState(
                targetValue = if (isSelected) AnimationConfig.NavBar.tabSelectedScale else 1f,
                animationSpec = tween(AnimationConfig.NavBar.tabScaleMs),
                label = "tabScale",
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(bgColor)
                    .clickable { onTabSelected(tab.route) },
                contentAlignment = Alignment.Center,
            ) {
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) WhiteColor else MediumGray,
                    animationSpec = tween(AnimationConfig.NavBar.tabTextMs),
                    label = "tabText",
                )
                Text(
                    tab.label,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }

        // 设置按钮
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MediumGray.copy(alpha = 0.2f))
                .clickable { onSettingsClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = LocaleStrings.settingsTitle,
                tint = WhiteColor,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
