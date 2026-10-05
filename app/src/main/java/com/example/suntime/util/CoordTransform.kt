package com.example.suntime.util

import kotlin.math.sqrt

/**
 * 坐标转换工具：GPS 原始坐标(WGS-84) 与国内地图服务坐标系互转。
 *
 *  - 高德 / 腾讯 使用 GCJ-02（俗称「火星坐标」）
 *  - 百度       使用 BD-09
 *
 * 国内地图服务都基于偏移后的坐标，若直接把 GPS 的 WGS-84 经纬度喂给它们，
 * 会有几十~几百米的偏差。本工具把 WGS-84 转换成对应服务所需坐标系后再请求。
 *
 * 算法为公开的标准近似实现（与高德/腾讯/百度官方 demo 一致），精度满足日常定位。
 */
object CoordTransform {

    private const val A = 6378245.0
    private const val EE = 0.00669342162296594323
    private const val X_PI = Math.PI * 3000.0 / 180.0
    private const val PI = Math.PI

    /** WGS-84 → GCJ-02（高德 / 腾讯） */
    fun wgs84ToGcj02(lng: Double, lat: Double): DoubleArray {
        if (outOfChina(lng, lat)) return doubleArrayOf(lng, lat)
        val d = delta(lng, lat)
        return doubleArrayOf(lng + d[0], lat + d[1])
    }

    /** GCJ-02 → WGS-84（逆向，存在米级误差） */
    fun gcj02ToWgs84(lng: Double, lat: Double): DoubleArray {
        if (outOfChina(lng, lat)) return doubleArrayOf(lng, lat)
        val d = delta(lng, lat)
        return doubleArrayOf(lng - d[0], lat - d[1])
    }

    /** GCJ-02 → BD-09（百度） */
    fun gcj02ToBd09(lng: Double, lat: Double): DoubleArray {
        val z = sqrt(lng * lng + lat * lat) + 0.00002 * Math.sin(lat * X_PI)
        val theta = Math.atan2(lat, lng) + 0.000003 * Math.cos(lng * X_PI)
        return doubleArrayOf(z * Math.cos(theta) + 0.0065, z * Math.sin(theta) + 0.006)
    }

    /** WGS-84 → BD-09（百度） */
    fun wgs84ToBd09(lng: Double, lat: Double): DoubleArray {
        val gcj = wgs84ToGcj02(lng, lat)
        return gcj02ToBd09(gcj[0], gcj[1])
    }

    /** BD-09 → GCJ-02 */
    fun bd09ToGcj02(lng: Double, lat: Double): DoubleArray {
        val x = lng - 0.0065
        val y = lat - 0.006
        val z = sqrt(x * x + y * y) - 0.00002 * Math.sin(y * X_PI)
        val theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * X_PI)
        return doubleArrayOf(z * Math.cos(theta), z * Math.sin(theta))
    }

    /** BD-09 → WGS-84 */
    fun bd09ToWgs84(lng: Double, lat: Double): DoubleArray {
        val gcj = bd09ToGcj02(lng, lat)
        return gcj02ToWgs84(gcj[0], gcj[1])
    }

    /** 判断是否在中国境内（粗略包围盒），用于决定走国内服务商还是系统 Geocoder */
    fun isInChina(lng: Double, lat: Double): Boolean {
        return !(lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271)
    }

    private fun delta(lng: Double, lat: Double): DoubleArray {
        var dLat = transformLat(lng - 105.0, lat - 35.0)
        var dLng = transformLng(lng - 105.0, lat - 35.0)
        val radLat = lat / 180.0 * PI
        var magic = Math.sin(radLat)
        magic = 1 - EE * magic * magic
        val sqrtMagic = sqrt(magic)
        dLat = dLat * 180.0 / (A * (1 - EE) / (magic * sqrtMagic) * PI)
        dLng = dLng * 180.0 / (A / sqrtMagic * Math.cos(radLat) * PI)
        return doubleArrayOf(dLng, dLat)
    }

    private fun transformLat(x: Double, y: Double): Double {
        var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(Math.abs(x))
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0
        ret += (160.0 * Math.sin(y / 12.0 * PI) + 320 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0
        return ret
    }

    private fun transformLng(x: Double, y: Double): Double {
        var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(Math.abs(x))
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0
        ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0
        return ret
    }

    private fun outOfChina(lng: Double, lat: Double): Boolean {
        return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271
    }
}
