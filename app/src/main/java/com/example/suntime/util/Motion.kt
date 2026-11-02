package com.example.suntime.util

import android.content.Context
import android.provider.Settings

/**
 * 动画开关：遵循系统「动画时长缩放」设置。
 *
 * 当用户在开发者选项或无障碍设置里关闭动画（scale = 0）时，所有自定义转场直接跳过，
 * 既符合系统行为，也避免在低性能设备上为动画付出额外帧开销。
 */
object Motion {

    fun animationsEnabled(context: Context): Boolean {
        val scale = try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
        } catch (_: Exception) {
            1f
        }
        return scale > 0f
    }
}
