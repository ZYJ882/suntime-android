package com.example.suntime.adapter

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.suntime.R
import java.util.Calendar

/**
 * 年视图：把一年的 12 个月渲染成多列宫格（手机 2 列 / 平板 3 列）。
 *
 * 排版与性能约定：
 * - 迷你月历每格**只显示公历数字单行**（农历/节气信息保留在月视图与日视图），
 *   这是年视图能紧凑成宫格的关键——每格两层文字会把年视图撑成 12 个完整月历的列表；
 * - 每格是单个 [TextView]（不再是 LinearLayout 嵌两层文字），
 *   12 个月 × 42 格的创建与测量开销大幅降低；
 * - 今天用主色圆点背景高亮，非本月补位日期淡显；
 * - 调用方（[com.example.suntime.fragment.CalendarFragment]）需按年份缓存，
 *   同一年不重复调用，避免每次切换视图都全量重建导致卡顿。
 *
 * 所有程序化尺寸均以 dp/sp 换算，不写死像素。
 */
object CalendarYearAdapter {

    /**
     * @param columns 宫格列数（手机 2、平板 3）
     * @param onPickMonth 点击某月卡片回调，跳转月视图
     */
    fun buildInto(
        parent: ViewGroup,
        year: Int,
        columns: Int = 2,
        onPickMonth: (year: Int, month: Int) -> Unit
    ) {
        parent.removeAllViews()
        val ctx = parent.context
        val density = ctx.resources.displayMetrics.density
        fun dp(v: Float): Int = (v * density).toInt()

        val now = Calendar.getInstance()
        val todayY = now.get(Calendar.YEAR)
        val todayM = now.get(Calendar.MONTH) + 1
        val todayD = now.get(Calendar.DAY_OF_MONTH)

        for (m in 1..12) {
            val item = LayoutInflater.from(ctx)
                .inflate(R.layout.item_calendar_month_mini, parent, false)
            item.findViewById<TextView>(R.id.tvMiniMonth).text = "${m}月"

            val grid = item.findViewById<GridLayout>(R.id.gridMini)

            val first = Calendar.getInstance().apply { clear(); set(year, m - 1, 1) }
            val firstWd = first.get(Calendar.DAY_OF_WEEK) - 1   // 0=周日
            val days = first.getActualMaximum(Calendar.DAY_OF_MONTH)

            // 上月补位
            val prev = Calendar.getInstance().apply { clear(); timeInMillis = first.timeInMillis; add(Calendar.DAY_OF_MONTH, -firstWd) }
            repeat(firstWd) {
                addCell(ctx, grid, prev.get(Calendar.DAY_OF_MONTH), muted = true, isToday = false, dp = ::dp)
                prev.add(Calendar.DAY_OF_MONTH, 1)
            }
            // 本月
            for (d in 1..days) {
                val isToday = year == todayY && m == todayM && d == todayD
                addCell(ctx, grid, d, muted = false, isToday = isToday, dp = ::dp)
            }
            // 下月补位到 42 格（固定 6 行，保证 12 张卡片行高一致）
            val next = Calendar.getInstance().apply { clear(); timeInMillis = first.timeInMillis; add(Calendar.DAY_OF_MONTH, days) }
            while (grid.childCount < 42) {
                addCell(ctx, grid, next.get(Calendar.DAY_OF_MONTH), muted = true, isToday = false, dp = ::dp)
                next.add(Calendar.DAY_OF_MONTH, 1)
            }

            item.setOnClickListener { onPickMonth(year, m) }

            val lp = GridLayout.LayoutParams()
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            lp.rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
            lp.width = 0
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            lp.setMargins(0, dp(3f), 0, dp(3f))
            parent.addView(item, lp)
        }
    }

    /** 单行数字格子：today 实心高亮、muted（非本月补位）淡显。 */
    private fun addCell(
        ctx: Context,
        grid: GridLayout,
        day: Int,
        muted: Boolean,
        isToday: Boolean,
        dp: (Float) -> Int
    ) {
        val cell = TextView(ctx).apply {
            text = day.toString()
            gravity = Gravity.CENTER
            includeFontPadding = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            if (isToday) {
                setBackgroundResource(R.drawable.bg_day_today)
                setTextColor(ContextCompat.getColor(ctx, R.color.on_primary))
            } else {
                setTextColor(
                    ContextCompat.getColor(
                        ctx,
                        if (muted) R.color.text_tertiary else R.color.text_primary
                    )
                )
                if (muted) alpha = 0.5f
            }
            setPadding(0, dp(3f), 0, dp(3f))
        }

        val lp = GridLayout.LayoutParams()
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        lp.rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
        lp.width = 0
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
        grid.addView(cell, lp)
    }
}
