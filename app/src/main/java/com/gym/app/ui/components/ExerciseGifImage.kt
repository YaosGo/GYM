package com.gym.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gym.app.data.entity.Exercise

private val PlaceholderBg = Color(0xFFEEEEEE)
private val PlaceholderIcon = Color(0xFFBDBDBD)

/**
 * 动作 GIF 展示组件。
 * 自定义动作（gifPath 为空）显示灰底哑铃占位图标，替代 GIF 加载。
 */
@Composable
fun ExerciseGifImage(
    exercise: Exercise,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderIconSize: Dp = 24.dp,
) {
    if (exercise.gifPath.isBlank()) {
        Box(
            modifier = modifier.background(PlaceholderBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.FitnessCenter,
                contentDescription = exercise.displayName,
                tint = PlaceholderIcon,
                modifier = Modifier.size(placeholderIconSize),
            )
        }
    } else {
        AsyncImage(
            model = "file:///android_asset/${exercise.gifPath}",
            contentDescription = exercise.displayName,
            modifier = modifier,
            contentScale = contentScale,
        )
    }
}
