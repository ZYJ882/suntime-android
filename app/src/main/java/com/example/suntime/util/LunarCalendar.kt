package com.example.suntime.util

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/**
 * 公历 <-> 农历 转换 + 黄历（干支、生肖、节气、星座、建除宜忌）
 * 农历数据覆盖 1900-2100 年。
 * 说明：建除十二神与节气采用了简化算法，宜忌为参考值，并非专业择日。
 */
object LunarCalendar {

    // 1900-2100 每年农历信息（末4位为闰月，0x10000 位表示闰月大小，其余位表示12个月大小）
    private val lunarInfo = intArrayOf(
        0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
        0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
        0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
        0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
        0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
        0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5b0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0,
        0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
        0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b6a0, 0x195a6,
        0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
        0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x055c0, 0x0ab60, 0x096d5, 0x092e0,
        0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
        0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
        0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
        0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
        0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,
        0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0,
        0x0a2e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4,
        0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0,
        0x0b273, 0x0a930, 0x07950, 0x06b55, 0x0ad50, 0x05b52, 0x04b60, 0x0a570, 0x0a6e6, 0x07530,
        0x0d260, 0x0ea65, 0x0d530, 0x05aa0, 0x056d0, 0x0a6e3, 0x0a4d0, 0x0d150, 0x0f252
    )

    private val monthCn = arrayOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊")
    private val dayCn = arrayOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )
    private val stems = arrayOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    private val branches = arrayOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    private val animals = arrayOf("鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪")
    private val zodiacCn = arrayOf("鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪")

    private val solarTermNames = arrayOf(
        "小寒", "大寒", "立春", "雨水", "惊蛰", "春分", "清明", "谷雨", "立夏", "小满",
        "芒种", "夏至", "小暑", "大暑", "立秋", "处暑", "白露", "秋分", "寒露", "霜降",
        "立冬", "小雪", "大雪", "冬至"
    )
    private val sTermInfo = intArrayOf(
        0, 21208, 42467, 63836, 85337, 107014, 128867, 150921, 173149, 195551,
        218072, 240693, 263343, 285989, 308563, 331033, 353350, 375494, 397447, 419210,
        440795, 462224, 483532, 504758
    )

    // 建除十二神
    private val jianChu = arrayOf("建", "除", "满", "平", "定", "执", "破", "危", "成", "收", "开", "闭")
    // 简化宜忌（建除 -> Pair(宜, 忌)）
    private val yiJi = arrayOf(
        Pair("出行、上任、会友、动土、嫁娶", "安葬、开仓、诉讼、乘船"),
        Pair("沐浴、扫舍、出行、解除", "移徙、入宅、安门"),
        Pair("祭祀、开市、交易、纳财、出行", "动土、安葬、造船"),
        Pair("修造、安床、出行、栽种", "嫁娶、移徙、诉讼"),
        Pair("祭祀、祈福、嫁娶、订盟", "出行、词讼、开仓"),
        Pair("捕捉、纳畜、修造、安床", "开市、出行、移徙"),
        Pair("破屋、坏垣、求医、解除", "嫁娶、出行、签约"),
        Pair("安床、祭祀、出行、纳财", "动工、迁徙、词讼"),
        Pair("嫁娶、开市、入学、出行、纳财", "诉讼、安葬、乘船"),
        Pair("纳财、收购、修仓、栽种", "出行、嫁娶、开市"),
        Pair("开市、求医、祭祀、动土", "安葬、移徙、安门"),
        Pair("筑堤、安葬、闭仓、修补", "出行、开市、嫁娶")
    )

    // ---------- 农历内部计算 ----------
    private fun lYearDays(y: Int): Int {
        var sum = 348
        var i = 0x8000
        while (i > 0x8) {
            if ((lunarInfo[y - 1900] and i) != 0) sum += 1
            i = i shr 1
        }
        return sum + leapDays(y)
    }

    private fun leapDays(y: Int): Int {
        return if (leapMonth(y) != 0) {
            if ((lunarInfo[y - 1900] and 0x10000) != 0) 30 else 29
        } else 0
    }

    private fun leapMonth(y: Int): Int = lunarInfo[y - 1900] and 0xf

    private fun monthDays(y: Int, m: Int): Int =
        if ((lunarInfo[y - 1900] and (0x10000 shr m)) != 0) 30 else 29

    // 节气所在日期（日，1-31）
    private fun getTermDay(y: Int, n: Int): Int {
        val base = Date.UTC(1900, 0, 6, 2, 5, 0)
        val ms = (31556925974.7 * (y - 1900) + sTermInfo[n] * 60000 + base).toLong()
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = ms
        return cal.get(Calendar.DAY_OF_MONTH)
    }

    fun getSolarTerm(y: Int, m: Int, d: Int): String? {
        val n1 = (m - 1) * 2
        if (getTermDay(y, n1) == d) return solarTermNames[n1]
        if (getTermDay(y, n1 + 1) == d) return solarTermNames[n1 + 1]
        return null
    }

    // 儒略日（正午）
    private fun jdn(y: Int, m: Int, d: Int): Int {
        val a = (14 - m) / 12
        val yy = y + 4800 - a
        val mm = m + 12 * a - 3
        return d + (153 * mm + 2) / 5 + 365 * yy + yy / 4 - yy / 100 + yy / 400 - 32045
    }

    private fun ganZhiDay(y: Int, m: Int, d: Int): String {
        val idx = (jdn(y, m, d) + 49) % 60
        return stems[idx % 10] + branches[idx % 12]
    }

    data class Lunar(
        val lunarYear: Int,
        val lunarMonth: Int,   // 1-12
        val lunarDay: Int,     // 1-30
        val isLeap: Boolean,
        val ganzhiYear: String,
        val ganzhiMonth: String,
        val ganzhiDay: String,
        val zodiac: String,
        val monthCn: String,
        val dayCn: String,
        val term: String?,
        val weekday: String,
        val constellation: String,
        val yi: String,
        val ji: String
    )

    fun solarToLunar(year: Int, month: Int, day: Int): Lunar {
        val base = Calendar.getInstance().apply {
            clear(); set(1900, 0, 31, 0, 0, 0); set(Calendar.MILLISECOND, 0)
        }
        val obj = Calendar.getInstance().apply {
            clear(); set(year, month - 1, day, 0, 0, 0); set(Calendar.MILLISECOND, 0)
        }
        var offset = ((obj.timeInMillis - base.timeInMillis) / 86400000).toInt()

        var y = 1900
        var temp = 0
        while (y <= 2100) {
            temp = lYearDays(y)
            if (offset < temp) break
            offset -= temp
            y++
        }
        val lunarYear = y

        val leap = leapMonth(lunarYear)
        var isLeap = false
        var m = 1
        var lunarMonth = 1
        var isLeapMonth = false
        while (m <= 12) {
            val days: Int
            if (leap > 0 && m == leap + 1 && !isLeap) {
                days = leapDays(lunarYear)
                isLeap = true
            } else {
                days = monthDays(lunarYear, m)
                isLeap = false
            }
            if (offset < days) {
                lunarMonth = if (isLeap) leap else m
                isLeapMonth = isLeap
                break
            }
            offset -= days
            m++
        }
        val lunarDay = offset + 1

        val yGan = ((lunarYear - 4) % 10 + 10) % 10
        val yZhi = ((lunarYear - 4) % 12 + 12) % 12
        val ganzhiYear = stems[yGan] + branches[yZhi]
        val zodiac = zodiacCn[yZhi]

        // 月干支（简化：以正月为寅）
        val mGan = ((yGan % 5) * 2 + 2 + (lunarMonth - 1)) % 10
        val mZhi = (lunarMonth + 1) % 12
        val ganzhiMonth = stems[mGan] + branches[mZhi]

        val gzDay = ganZhiDay(year, month, day)

        val monthName = (if (isLeapMonth) "闰" else "") + monthCn[lunarMonth - 1]
        val dayName = dayCn[lunarDay - 1]

        // 建除
        val monthBranch = (lunarMonth + 1) % 12
        val dayZhi = ((jdn(year, month, day) + 49) % 60) % 12
        val jcIdx = ((dayZhi - monthBranch) % 12 + 12) % 12
        val (yi, ji) = yiJi[jcIdx]

        val weekday = arrayOf("日", "一", "二", "三", "四", "五", "六")[obj.get(Calendar.DAY_OF_WEEK) - 1]
        val constellation = getConstellation(month, day)
        val term = getSolarTerm(year, month, day)

        return Lunar(
            lunarYear, lunarMonth, lunarDay, isLeapMonth,
            ganzhiYear, ganzhiMonth, gzDay, zodiac,
            monthName, dayName, term, "星期$weekday", constellation, yi, ji
        )
    }

    fun getConstellation(month: Int, day: Int): String {
        val edges = intArrayOf(20, 19, 21, 20, 21, 22, 23, 23, 23, 24, 23, 22)
        val names = arrayOf(
            "摩羯", "水瓶", "双鱼", "白羊", "金牛", "双子",
            "巨蟹", "狮子", "处女", "天秤", "天蝎", "射手", "摩羯"
        )
        val idx = if (day < edges[month - 1]) month - 1 else month
        return names[idx] + "座"
    }

    // 农历字符串，如 "农历 闰二月初五"
    fun lunarString(l: Lunar): String =
        "农历 " + l.monthCn + "月" + l.dayCn
}
