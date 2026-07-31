package com.gym.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * ## 全局动画配置表
 *
 * 应用内所有可调节的动画参数集中在此文件。**只修改数值即可全局生效**，无需改动业务代码。
 *
 * ### 快速索引
 * | 对象 | 控制范围 |
 * |------|---------|
 * | [Page] | 页面导航 (NavHost push/pop) |
 * | [Frosted] | 磨砂玻璃底部滑出面板 |
 * | [NavBar] | 底部导航栏指示器 |
 * | [ContentHide] | 主内容区隐藏动画（面板出现时） |
 * | [Stat] | 统计数字滚动 |
 * | [CheckMark] | 训练组完成勾号 |
 * | [Selector] | 设置页选项切换 |
 * | [Stagger] | 列表项错位出现 |
 * | [StatusBar] | 状态栏颜色过渡 |
 * | [Press] | 按压微交互 |
 * | [Springs] | 弹簧物理参数 |
 * | [Durations] | 通用时长 |
 */
object AnimationConfig {

    // ═══════════════════════════════════════════════════════════════
    // 页面导航
    // ═══════════════════════════════════════════════════════════════
    object Page {
        const val tabFadeMs = 300           // 主 Tab 间切换淡入淡出
        const val pushSlideMs = 350         // 页面推入滑动
        const val pushFadeMs = 250          // 页面推入伴随淡入

        val easing = FastOutSlowInEasing
    }

    // ═══════════════════════════════════════════════════════════════
    // 磨砂玻璃底部滑出面板
    // ═══════════════════════════════════════════════════════════════
    object Frosted {
        const val scrimFadeInMs = 300       // 遮罩淡入
        const val scrimFadeOutMs = 250      // 遮罩淡出
        const val sheetSlideInMs = 350      // 面板滑入
        const val sheetSlideInDelayMs = 150 // 面板滑入延迟（等待导航栏先滑出）
        const val sheetSlideOutMs = 300     // 面板滑出
        const val scrimAlpha = 0.45f        // 遮罩透明度 (0~1)
        const val defaultHeightFraction = 0.88f  // 面板占屏默认比例
        const val compactHeightFraction = 0.85f  // 面板占屏紧凑比例
        const val cornerRadiusDp = 20       // 面板顶部圆角 (dp)
        const val shadowElevationDp = 24    // 面板阴影 (dp)
    }

    // ═══════════════════════════════════════════════════════════════
    // 底部导航栏
    // ═══════════════════════════════════════════════════════════════
    object NavBar {
        const val tabColorMs = 250          // Tab 背景色过渡
        const val tabScaleMs = 250          // Tab 缩放过渡
        const val tabTextMs = 250           // Tab 文字色过渡
        const val barSlideMs = 300          // 底部栏整体滑入/滑出
        const val tabSelectedScale = 1.08f  // 选中 Tab 放大比例
    }

    // ═══════════════════════════════════════════════════════════════
    // 主内容区隐藏（面板出现时，Scaffold 向上滑出）
    // ═══════════════════════════════════════════════════════════════
    object ContentHide {
        const val slideMs = 250             // 主内容向上滑出/滑入
    }

    // ═══════════════════════════════════════════════════════════════
    // 统计数字滚动 (HistoryScreen)
    // ═══════════════════════════════════════════════════════════════
    object Stat {
        const val countUpMs = 600           // 数字从 0 滚动到目标值
    }

    // ═══════════════════════════════════════════════════════════════
    // 训练组完成勾号
    // ═══════════════════════════════════════════════════════════════
    object CheckMark {
        const val scaleDamping = Spring.DampingRatioMediumBouncy
        const val scaleStiffness = Spring.StiffnessMedium
        const val bgColorMs = 200           // 勾号背景色过渡
    }

    // ═══════════════════════════════════════════════════════════════
    // 设置页选项切换
    // ═══════════════════════════════════════════════════════════════
    object Selector {
        const val bgColorMs = 200           // 选中行背景色
        const val textColorMs = 200         // 选中行文字色
        const val checkFadeInMs = 200       // 勾号弹入
        const val checkFadeOutMs = 150      // 勾号消失
    }

    // ═══════════════════════════════════════════════════════════════
    // 列表项错位出现
    // ═══════════════════════════════════════════════════════════════
    object Stagger {
        const val itemDurationMs = 300      // 单项动画时长
        const val detailStepDelayMs = 50    // 详情页步进延迟 (ms/项)
        const val sheetStepDelayMs = 40     // Sheet 页步进延迟 (ms/项)
    }

    // ═══════════════════════════════════════════════════════════════
    // 状态栏颜色过渡
    // ═══════════════════════════════════════════════════════════════
    object StatusBar {
        const val colorMs = 400             // 状态栏颜色过渡
    }

    // ═══════════════════════════════════════════════════════════════
    // 按压微交互
    // ═══════════════════════════════════════════════════════════════
    object Press {
        const val scale = 0.97f             // 按下时缩小比例
        const val springDamping = Spring.DampingRatioMediumBouncy
        const val springStiff = Spring.StiffnessMedium
    }

    // ═══════════════════════════════════════════════════════════════
    // 弹簧物理参数
    // ═══════════════════════════════════════════════════════════════
    object Springs {
        fun bouncy() = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        fun gentle() = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
        fun default() = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    }

    // ═══════════════════════════════════════════════════════════════
    // 通用时长
    // ═══════════════════════════════════════════════════════════════
    object Durations {
        const val fast = 150
        const val normal = 300
        const val medium = 400
        const val slow = 500
    }
}
