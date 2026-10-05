package com.example.suntime.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * 全局设置持久化（SharedPreferences）。
 *
 * 两类设置：
 *  - **逆地理 API Key**（高德 / 腾讯 / 腾讯签名 SK / 百度）：用户在「设置」页填写后保存在本机，
 *    未填写时回退到 [MapKeys] 的内置默认值（默认均为空，即走系统 Geocoder 兜底）。
 *  - **主题模式**：0=跟随系统 / 1=浅色 / 2=深色，启动时由 [MainActivity] 应用到全局。
 *
 * 所有 Key 仅存于本机 SharedPreferences，**不会上传**。
 */
object SettingsStore {

    private const val PREFS = "suntime_settings"
    private const val K_AMAP = "amap_key"
    private const val K_TENCENT = "tencent_key"
    private const val K_TENCENT_SK = "tencent_sk"
    private const val K_BAIDU = "baidu_key"
    private const val K_THEME = "theme_mode"

    /** 主题模式常量 */
    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---------- 逆地理 Key（带 MapKeys 默认值回退，供 AddressResolver 使用）----------

    fun getAmapKey(ctx: Context): String =
        prefs(ctx).getString(K_AMAP, null)?.takeIf { it.isNotBlank() } ?: MapKeys.AMAP_KEY

    fun getTencentKey(ctx: Context): String =
        prefs(ctx).getString(K_TENCENT, null)?.takeIf { it.isNotBlank() } ?: MapKeys.TENCENT_KEY

    fun getTencentSk(ctx: Context): String =
        prefs(ctx).getString(K_TENCENT_SK, null)?.takeIf { it.isNotBlank() } ?: MapKeys.TENCENT_SK

    fun getBaiduKey(ctx: Context): String =
        prefs(ctx).getString(K_BAIDU, null)?.takeIf { it.isNotBlank() } ?: MapKeys.BAIDU_KEY

    // ---------- 逆地理 Key（原始用户输入，供设置页回显）----------

    fun getRawAmapKey(ctx: Context): String = prefs(ctx).getString(K_AMAP, "") ?: ""
    fun getRawTencentKey(ctx: Context): String = prefs(ctx).getString(K_TENCENT, "") ?: ""
    fun getRawTencentSk(ctx: Context): String = prefs(ctx).getString(K_TENCENT_SK, "") ?: ""
    fun getRawBaiduKey(ctx: Context): String = prefs(ctx).getString(K_BAIDU, "") ?: ""

    // ---------- 主题 ----------

    fun getThemeMode(ctx: Context): Int = prefs(ctx).getInt(K_THEME, THEME_SYSTEM)

    // ---------- 写入 ----------

    fun setAmapKey(ctx: Context, v: String) = prefs(ctx).edit().putString(K_AMAP, v).apply()
    fun setTencentKey(ctx: Context, v: String) = prefs(ctx).edit().putString(K_TENCENT, v).apply()
    fun setTencentSk(ctx: Context, v: String) = prefs(ctx).edit().putString(K_TENCENT_SK, v).apply()
    fun setBaiduKey(ctx: Context, v: String) = prefs(ctx).edit().putString(K_BAIDU, v).apply()
    fun setThemeMode(ctx: Context, mode: Int) = prefs(ctx).edit().putInt(K_THEME, mode).apply()

    /** 将存储的主题模式映射为 AppCompatDelegate 的 night mode */
    fun toNightMode(mode: Int): Int = when (mode) {
        THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
}
