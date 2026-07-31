package com.gym.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 磨砂玻璃质感底部滑出面板（iPhone 风格拖动条 + 中间阈值关闭）。
 *
 * - 顶部拖动条可按住上下拖拽
 * - 下拉超过屏幕中间 → 松手关闭
 * - 下拉未过中间或滑回中间以上 → 弹回展开
 * - 点击遮罩关闭
 */
@Composable
fun FrostedBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    sheetHeightFraction: Float = 0.82f,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
    val dismissThresholdPx = screenHeightPx * 0.50f  // 屏幕中间
    val dragOffset = remember { Animatable(0f) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(AnimationConfig.Frosted.scrimFadeInMs)),
        exit = fadeOut(tween(AnimationConfig.Frosted.scrimFadeOutMs)),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // ── 磨砂玻璃遮罩 ──
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = AnimationConfig.Frosted.scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )

            // ── 底部面板 ──
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    animationSpec = tween(durationMillis = AnimationConfig.Frosted.sheetSlideInMs, delayMillis = AnimationConfig.Frosted.sheetSlideInDelayMs),
                    initialOffsetY = { it },
                ),
                exit = slideOutVertically(
                    animationSpec = tween(AnimationConfig.Frosted.sheetSlideOutMs),
                    targetOffsetY = { it },
                ),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                val cornerRadius = AnimationConfig.Frosted.cornerRadiusDp.dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxSize(fraction = sheetHeightFraction)
                        .offset { IntOffset(0, dragOffset.value.roundToInt().coerceAtLeast(0)) }
                        .shadow(AnimationConfig.Frosted.shadowElevationDp.dp,
                            RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius))
                        .clip(RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius))
                        .background(Color.White)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    scope.launch {
                                        if (dragOffset.value > dismissThresholdPx) {
                                            onDismiss()
                                        } else {
                                            dragOffset.animateTo(0f, tween(250))
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch { dragOffset.animateTo(0f, tween(250)) }
                                },
                                onVerticalDrag = { _, dragAmount ->
                                    scope.launch {
                                        val newValue = dragOffset.value + dragAmount
                                        // 只允许向下拖（正值），但不限制回弹
                                        dragOffset.snapTo(newValue.coerceAtLeast(0f))
                                    }
                                },
                            )
                        },
                ) {
                    // ── iPhone 风格拖动条 ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFFD0D0D0)),
                        )
                    }
                    // ── 内容 ──
                    content()
                }
            }
        }
    }
}
