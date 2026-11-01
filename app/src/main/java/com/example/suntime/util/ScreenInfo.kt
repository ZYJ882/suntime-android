package com.example.suntime.util

import android.content.Context
import android.util.DisplayMetrics

/**
 * 设备屏幕信息快照：读取真实分辨率与 DPI，并换算为 dp 与窗口尺寸等级。
 *
 * 这是「读取屏幕分辨率与 DPI」的单一事实源，驱动：
 * 1) 设置页「设备信息」只读卡片展示；
 * 2) 代码内依据尺寸等级切换（如世界时钟列数）。
 *
 * 读取的是 [DisplayMetrics]（应用可用显示区域的真实像素/密度），
 * 不修改任何 density——本项目**禁止全局缩放界面**。
 */
data class ScreenInfo(
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
    val density: Float,
    val scaledDensity: Float,
    val widthDp: Int,
    val heightDp: Int,
    val dpiCategory: String,
    val sizeClass: WindowSizeClass
) {
    /** 形如 "1080 × 2400" 的分辨率字符串。 */
    val resolution: String get() = "$widthPx × $heightPx"

    /** 形如 "420 dpi (xxhdpi)" 的密度字符串。 */
    val densityLabel: String get() = "$densityDpi dpi ($dpiCategory)"
}

object ScreenInfoReader {

    fun read(context: Context): ScreenInfo {
        val dm: DisplayMetrics = context.resources.displayMetrics
        val wPx = dm.widthPixels
        val hPx = dm.heightPixels
        val density = dm.density
        val densityDpi = dm.densityDpi
        val widthDp = if (density > 0f) (wPx / density).toInt() else wPx
        val heightDp = if (density > 0f) (hPx / density).toInt() else hPx
        val dpiCategory = when {
            densityDpi >= 640 -> "xxxhdpi"
            densityDpi >= 480 -> "xxhdpi"
            densityDpi >= 320 -> "xhdpi"
            densityDpi >= 240 -> "hdpi"
            densityDpi >= 160 -> "mdpi"
            else -> "ldpi"
        }
        return ScreenInfo(
            widthPx = wPx,
            heightPx = hPx,
            densityDpi = densityDpi,
            density = density,
            scaledDensity = dm.scaledDensity,
            widthDp = widthDp,
            heightDp = heightDp,
            dpiCategory = dpiCategory,
            sizeClass = WindowSize.classify(widthDp)
        )
    }
}
