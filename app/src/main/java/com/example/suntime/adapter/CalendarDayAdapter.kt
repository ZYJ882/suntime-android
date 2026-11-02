package com.example.suntime.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.suntime.R
import com.example.suntime.util.LunarCalendar

data class DayCell(
    val year: Int,
    val month: Int,
    val day: Int,
    val inMonth: Boolean,
    val isToday: Boolean,
    var isSelected: Boolean
)

class CalendarDayAdapter(
    context: Context,
    private val cells: MutableList<DayCell>,
    private val onPick: (DayCell) -> Unit
) : RecyclerView.Adapter<CalendarDayAdapter.VH>() {

    // 颜色只在构造时解析一次（原实现每次 bind 解析 4 次 × 42 格）
    private val onPrimary = ContextCompat.getColor(context, R.color.on_primary)
    private val textPrimary = ContextCompat.getColor(context, R.color.text_primary)
    private val textTertiary = ContextCompat.getColor(context, R.color.text_tertiary)
    private val primary = ContextCompat.getColor(context, R.color.primary)

    fun update(list: List<DayCell>) {
        cells.clear()
        cells.addAll(list)
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val bg: View = v.findViewById(R.id.tvDayBg)
        val num: TextView = v.findViewById(R.id.tvDayNum)
        val lunar: TextView = v.findViewById(R.id.tvLunarNum)

        /** 记录背景状态，避免每次 bind 都重新加载 drawable。 */
        var bgState = STATE_NONE

        companion object {
            const val STATE_NONE = 0
            const val STATE_TODAY = 1
            const val STATE_SELECTED = 2
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
        val holder = VH(v)
        // 点击监听只创建一次，按当前位置取数据，避免每次 bind 新建 Lambda
        v.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION && pos < cells.size) onPick(cells[pos])
        }
        return holder
    }

    override fun getItemCount() = cells.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = cells[position]
        val l = LunarCalendar.solarToLunar(c.year, c.month, c.day)
        holder.num.text = c.day.toString()
        holder.lunar.text = l.term ?: l.dayCn

        // 数字颜色：选中 -> onPrimary；非本月 -> 弱化；其余正常
        holder.num.setTextColor(
            when {
                c.isSelected -> onPrimary
                !c.inMonth -> textTertiary
                else -> textPrimary
            }
        )
        // 农历小字：选中 -> onPrimary；节气 -> Primary；其余弱化
        holder.lunar.setTextColor(
            when {
                c.isSelected -> onPrimary
                l.term != null -> primary
                else -> textTertiary
            }
        )
        holder.lunar.alpha = if (c.isSelected) 0.85f else 1f

        // 背景：选中 -> 实心圆；今天 -> 浅色圆；其余隐藏
        val state = when {
            c.isSelected -> VH.STATE_SELECTED
            c.isToday -> VH.STATE_TODAY
            else -> VH.STATE_NONE
        }
        if (holder.bgState != state) {
            holder.bgState = state
            when (state) {
                VH.STATE_SELECTED -> {
                    holder.bg.setBackgroundResource(R.drawable.bg_day_circle)
                    holder.bg.visibility = View.VISIBLE
                }
                VH.STATE_TODAY -> {
                    holder.bg.setBackgroundResource(R.drawable.bg_day_today)
                    holder.bg.visibility = View.VISIBLE
                }
                else -> holder.bg.visibility = View.INVISIBLE
            }
        }
    }
}
