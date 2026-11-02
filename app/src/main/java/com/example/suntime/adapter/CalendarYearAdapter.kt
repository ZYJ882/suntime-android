package com.example.suntime.adapter

import android.view.ViewGroup
import android.widget.LinearLayout
import com.example.suntime.view.MiniMonthView
import java.util.Calendar

/**
 * 年视图：把一年的 12 个月渲染成多列宫格（手机 2 列 / 平板 3 列）。
 *
 * 性能设计：
 * - 每张迷你月历是**单个自绘 View**（[MiniMonthView]），不再为每个月创建 42 个格子控件，
 *   12 个月合计只有 12 个绘制 View，切换年视图时不会出现主线程卡顿；
 * - 用「每行一个横向 LinearLayout + 子项 weight 均分」实现等宽宫格，
 *   避免 GridLayout 的 weight 多遍测量开销；
 * - 今天只取一次系统时间，传给全部卡片复用。
 *
 * 调用方需按年份缓存（[com.example.suntime.fragment.CalendarFragment.renderedYear]），
 * 同一年份不要重复构建。
 */
object CalendarYearAdapter {

    /**
     * @param columns 每行列数（手机 2、平板 3）
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
        val gap = (10f * density).toInt()

        val now = Calendar.getInstance()
        val todayY = now.get(Calendar.YEAR)
        val todayM = now.get(Calendar.MONTH) + 1
        val todayD = now.get(Calendar.DAY_OF_MONTH)

        var row: LinearLayout? = null
        for (m in 1..12) {
            val col = (m - 1) % columns
            if (col == 0) {
                val newRow = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                }
                val rowLp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                if (m > 1) rowLp.topMargin = gap
                parent.addView(newRow, rowLp)
                row = newRow
            }

            val card = MiniMonthView(ctx).apply {
                bind(year, m, todayY, todayM, todayD)
                setOnClickListener { onPickMonth(year, m) }
            }
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            if (col > 0) lp.marginStart = gap

            row?.addView(card, lp)
        }
    }
}
