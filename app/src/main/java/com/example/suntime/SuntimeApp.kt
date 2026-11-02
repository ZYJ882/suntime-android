package com.example.suntime

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.suntime.util.SettingsStore

/**
 * 应用入口：只在这里应用一次夜间模式。
 *
 * 原先每个 Activity 都在 `onCreate` 里调用 `AppCompatDelegate.setDefaultNightMode`，
 * 每次进入页面都会重复做一次主题解析与潜在的重建检查。放到 Application 后：
 * - 主题在**任何 Activity 创建之前**就已确定，冷启动少一次主题切换；
 * - 计时器 / 秒表等二级页面不再重复设置。
 */
class SuntimeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(
            SettingsStore.toNightMode(SettingsStore.getThemeMode(this))
        )
    }
}
