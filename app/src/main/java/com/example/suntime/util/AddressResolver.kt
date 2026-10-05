package com.example.suntime.util

import android.content.Context
import android.location.Geocoder
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

/**
 * 逆地理编码（经纬度 → 具体地址），目标：国内 / 全球都能解析。
 *
 * 策略：
 *  - **中国境内**：优先走已配置的国内服务商（高德 → 腾讯 → 百度），请求前用
 *    [CoordTransform] 把 GPS 的 WGS-84 转成对应坐标系，保证精度；
 *  - **中国境外**：直接走系统 Geocoder（底层为平台地理编码后端，海外全球覆盖）；
 *  - 国内服务商未配置 Key 时，同样回退到系统 Geocoder。
 *
 * 设计要点：
 *  - 全部 HTTP 请求在后台线程执行，结果通过主线程回调，避免 ANR；
 *  - 解析失败 / 无网返回 null，由调用方给出兜底文案，不会崩溃；
 *  - 返回结果携带 [AddressResult.provider]，便于界面标明「地址来源：xx」。
 */
data class AddressResult(
    val address: String?,   // 解析到的地址；null 表示未能解析
    val provider: String?,  // "高德" / "腾讯" / "百度" / "系统"
    val error: String? = null
)

object AddressResolver {

    private val handler = Handler(Looper.getMainLooper())

    fun resolve(context: Context, latitude: Double, longitude: Double, callback: (AddressResult) -> Unit) {
        Thread {
            val result = doResolve(context, latitude, longitude)
            handler.post { callback(result) }
        }.start()
    }

    private fun doResolve(context: Context, lat: Double, lng: Double): AddressResult {
        // 优先使用用户在「设置」中填写的 Key；未填写则回退到 MapKeys 内置默认值
        val amap = SettingsStore.getAmapKey(context)
        val tencent = SettingsStore.getTencentKey(context)
        val tencentSk = SettingsStore.getTencentSk(context)
        val baidu = SettingsStore.getBaiduKey(context)
        // 境外：系统 Geocoder 全球覆盖，直接走，跳过国内服务商避免无谓超时
        if (!CoordTransform.isInChina(lng, lat)) {
            tryGeocoder(context, lat, lng)?.let { return AddressResult(it, "系统") }
            return AddressResult(null, null, "系统 Geocoder 不可用或无网络")
        }
        // 境内：优先国内服务商（按已填 Key 决定启用哪些），最后回退系统 Geocoder
        if (amap.isNotBlank()) {
            tryAmap(lat, lng, amap)?.let { return AddressResult(it, "高德") }
        }
        if (tencent.isNotBlank()) {
            tryTencent(lat, lng, tencent, tencentSk)?.let { return AddressResult(it, "腾讯") }
        }
        if (baidu.isNotBlank()) {
            tryBaidu(lat, lng, baidu)?.let { return AddressResult(it, "百度") }
        }
        tryGeocoder(context, lat, lng)?.let { return AddressResult(it, "系统") }
        return AddressResult(null, null, "未配置国内服务 Key 且系统 Geocoder 不可用")
    }

    private fun httpGet(url: String): String? = try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "Suntime/1.6")
        val code = conn.responseCode
        val body = if (code == 200) conn.inputStream.bufferedReader().use { it.readText() } else null
        conn.disconnect()
        body
    } catch (_: Exception) {
        null
    }

    /** 高德：location 需 GCJ-02，格式为「经度,纬度」 */
    private fun tryAmap(lat: Double, lng: Double, key: String): String? {
        val (glng, glat) = CoordTransform.wgs84ToGcj02(lng, lat)
        val url = "https://restapi.amap.com/v3/geocode/regeo?output=JSON" +
                "&location=$glng,$glat&key=$key&radius=1000&extensions=base"
        val body = httpGet(url) ?: return null
        return try {
            val j = JSONObject(body)
            if (j.optString("status") != "1") return null
            j.optJSONObject("regeocode")?.optString("formatted_address")?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    /** 腾讯：location 需 GCJ-02，格式为「纬度,经度」，且必须带签名 sig */
    private fun tryTencent(lat: Double, lng: Double, key: String, sk: String): String? {
        val (tlng, tlat) = CoordTransform.wgs84ToGcj02(lng, lat)
        val params = linkedMapOf(
            "location" to "$tlat,$tlng",
            "key" to key,
            "get_poi" to "0",
            "output" to "json"
        )
        val query = params.map { "${it.key}=${it.value}" }.joinToString("&")
        val sigSource = params.toList().sortedBy { it.first }
            .joinToString("&") { "${it.first}=${it.second}" } + sk
        val url = "https://apis.map.qq.com/ws/geocoder/v1/?$query&sig=${md5(sigSource)}"
        val body = httpGet(url) ?: return null
        return try {
            val j = JSONObject(body)
            if (j.optInt("status") != 0) return null
            val r = j.optJSONObject("result") ?: return null
            val recommend = r.optJSONObject("formatted_addresses")?.optString("recommend")
            (recommend ?: r.optString("address")).takeIf { !it.isNullOrBlank() }
        } catch (_: Exception) {
            null
        }
    }

    /** 百度：直接传 WGS-84（coordtype=wgs84ll 由百度服务端转换），返回含语义描述 */
    private fun tryBaidu(lat: Double, lng: Double, key: String): String? {
        val url = "https://api.map.baidu.com/reverse_geocoding/v3/?ak=$key" +
                "&output=json&coordtype=wgs84ll&location=$lat,$lng&extensions_poi=1"
        val body = httpGet(url) ?: return null
        return try {
            val j = JSONObject(body)
            if (j.optInt("status") != 0) return null
            val r = j.optJSONObject("result") ?: return null
            val base = r.optString("formatted_address")
            val sematic = r.optString("sematic_description")
            buildString {
                if (base.isNotBlank()) append(base)
                if (sematic.isNotBlank()) append("（${sematic}）")
            }.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    /** 系统 Geocoder：海外可用，国内多数 ROM 无后端会抛异常（已捕获） */
    private fun tryGeocoder(context: Context, lat: Double, lng: Double): String? = try {
        val list = Geocoder(context, Locale.CHINA).getFromLocation(lat, lng, 1)
        if (!list.isNullOrEmpty()) {
            val a = list[0]
            listOf(a.countryName, a.adminArea, a.locality, a.subLocality, a.thoroughfare)
                .filterNotNull().filter { it.isNotBlank() }
                .joinToString(" ").takeIf { it.isNotBlank() }
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }

    private fun md5(s: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(s.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
