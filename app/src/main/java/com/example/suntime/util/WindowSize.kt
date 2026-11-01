package com.example.suntime.util

import android.content.Context
import android.content.res.Configuration

/**
 * 窗口尺寸等级（Android 官方 Window Size Class 规范）。
 *
 * 依据「窗口宽度（dp）」分类，与物理屏幕尺寸、像素分辨率解耦——
 * 同一套逻辑在手机竖屏 / 手机横屏 / 折叠屏 / 平板 / 桌面都通用。
 *
 * - [COMPACT]   宽度 < 600dp   （手机竖屏，绝大多数场景）
 * - [MEDIUM]    600–839dp      （手机横屏 / 小平板 / 折叠屏展开）
 * - [EXPANDED]  ≥ 840dp        （平板 / 桌面 / ChromeOS）
 *
 * 注意：本项目**禁止全局缩放界面**，所有适配都基于 dp/sp + 资源限定符 +
 * 本分类在代码里动态切换列数/布局，绝不修改 density。
 */
enum class WindowSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED;

    /** 中文展示名，用于「设备信息」卡片。 */
    val label: String
        get() = when (this) {
            COMPACT -> "紧凑"
            MEDIUM -> "中等"
            EXPANDED -> "展开"
        }
}

object WindowSize {

    /** 当前窗口宽度（dp）。读 configuration.screenWidthDp，免额外依赖、跨 SDK 通用。 */
    fun widthDp(context: Context): Int =
        context.resources.configuration.screenWidthDp

    /** 依据宽度 dp 分类。断点：600 / 840。 */
    fun classify(widthDp: Int): WindowSizeClass = when {
        widthDp >= 840 -> WindowSizeClass.EXPANDED
        widthDp >= 600 -> WindowSizeClass.MEDIUM
        else -> WindowSizeClass.COMPACT
    }

    /** 便捷：直接对 Context 分类。 */
    fun classify(context: Context): WindowSizeClass = classify(widthDp(context))

    fun isAtLeastMedium(context: Context): Boolean = widthDp(context) >= 600
    fun isExpanded(context: Context): Boolean = widthDp(context) >= 840

    /** 供调试/日志使用的断点常量（与上方保持一致，避免散落魔法数）。 */
    const val MEDIUM_THRESHOLD = 600
    const val EXPANDED_THRESHOLD = 840
}
