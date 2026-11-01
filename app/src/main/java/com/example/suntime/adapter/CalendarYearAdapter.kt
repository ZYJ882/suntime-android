package com.example.suntime.adapter

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.suntime.R
import com.example.suntime.util.LunarCalendar
import java.util.Calendar

/**
 * 年视图：把某一年的 12 个月渲染成 12 个迷你月历卡片，纵向排列。
 * 不依赖 RecyclerView（避免 ScrollView 内嵌 RV 的高度/滚动冲突），直接 build 进父容器。
 */
object CalendarYearAdapter {

    fun buildInto(parent: ViewGroup, year: Int, onPickMonth: (year: Int, month: Int) -> Unit) {
        parent.removeAllViews()
        val ctx = parent.context

        val now = Calendar.getInstance()
        val todayY = now.get(Calendar.YEAR)
        val todayM = now.get(Calendar.MONTH) + 1
        val todayD = now.get(Calendar.DAY_OF_MONTH)

        for (m in 1..12) {
            val item = LayoutInflater.from(ctx).inflate(R.layout.item_calendar_month_mini, parent, false)
            item.findViewById<TextView>(R.id.tvMiniMonth).text = String.format("%d月", m)

            val grid = item.findViewById<GridLayout>(R.id.gridMini)

            val first = Calendar.getInstance().apply { clear(); set(year, m - 1, 1) }
            val firstWd = first.get(Calendar.DAY_OF_WEEK) - 1
            val days = first.getActualMaximum(Calendar.DAY_OF_MONTH)

            // 上月补位
            val prev = Calendar.getInstance().apply { clear(); set(year, m - 1, 1); add(Calendar.DAY_OF_MONTH, -firstWd) }
            for (i in 0 until firstWd) {
                addCell(ctx, grid, prev.get(Calendar.DAY_OF_MONTH), null, false, false)
                prev.add(Calendar.DAY_OF_MONTH, 1)
            }
            // 本月
            for (d in 1..days) {
                val l = LunarCalendar.solarToLunar(year, m, d)
                val isToday = (year == todayY && m == todayM && d == todayD)
                addCell(ctx, grid, d, l.term ?: l.dayCn, l.term != null, isToday)
            }
            // 下月补位到 42
            var next = Calendar.getInstance().apply { clear(); set(year, m - 1, days); add(Calendar.DAY_OF_MONTH, 1) }
            while (grid.childCount < 42) {
                addCell(ctx, grid, next.get(Calendar.DAY_OF_MONTH), null, false, false)
                next.add(Calendar.DAY_OF_MONTH, 1)
            }

            item.setOnClickListener { onPickMonth(year, m) }
            parent.addView(item)
        }
    }

    private fun addCell(
        ctx: Context,
        grid: GridLayout,
        day: Int,
        lunarText: String?,
        isTerm: Boolean,
        isToday: Boolean
    ) {
        val cell = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            if (isToday) setBackgroundResource(R.drawable.bg_day_today)
            setPadding(0, 4, 0, 4)
        }
        val num = TextView(ctx).apply {
            text = day.toString()
            textSize = 11f
            includeFontPadding = false
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        }
        val lun = TextView(ctx).apply {
            text = lunarText ?: ""
            textSize = 9f
            includeFontPadding = false
            setTextColor(
                ContextCompat.getColor(
                    ctx,
                    if (isTerm) R.color.primary else R.color.text_tertiary
                )
            )
        }
        cell.addView(num)
        cell.addView(lun)

        val lp = GridLayout.LayoutParams().apply {
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        }
        grid.addView(cell, lp)
    }
}
