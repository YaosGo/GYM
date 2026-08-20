package com.gym.app.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

/**
 * Catmull-Rom 转三次贝塞尔：把折线点串画成圆滑曲线。
 * 曲线严格经过每个原始点（不裁角），拐角圆滑，健康类 App 走势图风格。
 * 控制点 y 轴钳制在数据范围内，避免曲线超出图表上下边界。
 */
fun smoothLinePath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    if (points.size == 1) return path
    if (points.size == 2) {
        path.lineTo(points[1].x, points[1].y)
        return path
    }
    val minY = points.minOf { it.y }
    val maxY = points.maxOf { it.y }
    for (i in 0 until points.size - 1) {
        val p0 = points.getOrElse(i - 1) { points[i] }
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points.getOrElse(i + 2) { points[i + 1] }
        val c1 = Offset(
            p1.x + (p2.x - p0.x) / 6f,
            (p1.y + (p2.y - p0.y) / 6f).coerceIn(minY, maxY),
        )
        val c2 = Offset(
            p2.x - (p3.x - p1.x) / 6f,
            (p2.y - (p3.y - p1.y) / 6f).coerceIn(minY, maxY),
        )
        path.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
    }
    return path
}
