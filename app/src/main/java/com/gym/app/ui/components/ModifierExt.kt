package com.gym.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput

/**
 * 可复用的 Compose Modifier 扩展 ── 微交互效果。
 */

/**
 * 按压缩放效果：按下时缩小到 [pressedScale]，松手时弹性恢复。
 * 常用于 Card、Button、IconButton 等可点击组件。
 *
 * @param pressedScale 按下时的缩放比例，默认 0.97f
 */
fun Modifier.pressEffect(pressedScale: Float = AnimationConfig.Press.scale): Modifier = composed {
    var isPressed by remember { mutableFloatStateOf(0f) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed > 0f) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = AnimationConfig.Press.springDamping,
            stiffness = AnimationConfig.Press.springStiff,
        ),
        label = "pressScale",
    )
    this
        .scale(scale)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = 1f
                    tryAwaitRelease()
                    isPressed = 0f
                },
            )
        }
}

/**
 * 按压缩放效果（无手势消费版本），不消费手势事件，允许外层同时处理点击。
 * 用于需要 pressEffect + clickable 同时在父组件的情况。
 */
fun Modifier.pressEffectNoGesture(
    pressedScale: Float = AnimationConfig.Press.scale,
    isPressed: Boolean,
): Modifier = composed {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = AnimationConfig.Press.springDamping,
            stiffness = AnimationConfig.Press.springStiff,
        ),
        label = "pressScaleNG",
    )
    this.scale(scale)
}
