package com.example.suntime.util

import java.util.Calendar

/**
 * 日期计算辅助：相差天数、加减天数。
 */
object DateCalc {

    /** 两个日期相差的天数（b - a，可为负） */
    fun diffDays(y1: Int, m1: Int, d1: Int, y2: Int, m2: Int, d2: Int): Long {
        val a = Calendar.getInstance().apply { clear(); set(y1, m1 - 1, d1) }
        val b = Calendar.getInstance().apply { clear(); set(y2, m2 - 1, d2) }
        return ((b.timeInMillis - a.timeInMillis) / (24 * 3600 * 1000L))
    }

    /** 在某个日期上加 days 天 */
    fun addDays(y: Int, m: Int, d: Int, days: Int): Triple<Int, Int, Int> {
        val c = Calendar.getInstance().apply { clear(); set(y, m - 1, d); add(Calendar.DAY_OF_MONTH, days) }
        return Triple(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    /** 在某个毫秒时间上加毫秒 */
    fun addMillis(base: Long, addMs: Long): Long = base + addMs
}
