package com.example.suntime.adapter

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
    private val cells: MutableList<DayCell>,
    private val onPick: (DayCell) -> Unit
) : RecyclerView.Adapter<CalendarDayAdapter.VH>() {

    fun update(list: List<DayCell>) {
        cells.clear()
        cells.addAll(list)
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val bg: View = v.findViewById(R.id.tvDayBg)
        val num: TextView = v.findViewById(R.id.tvDayNum)
        val lunar: TextView = v.findViewById(R.id.tvLunarNum)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
        return VH(v)
    }

    override fun getItemCount() = cells.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ctx = holder.itemView.context
        val c = cells[position]
        val l = LunarCalendar.solarToLunar(c.year, c.month, c.day)
        holder.num.text = c.day.toString()
        holder.lunar.text = l.term ?: l.dayCn

        val onPrimary = ContextCompat.getColor(ctx, R.color.on_primary)
        val textPrimary = ContextCompat.getColor(ctx, R.color.text_primary)
        val textTertiary = ContextCompat.getColor(ctx, R.color.text_tertiary)
        val primary = ContextCompat.getColor(ctx, R.color.primary)

        // 数字颜色：选中 -> onPrimary；非本月 -> 弱化；节日 -> Primary
        holder.num.setTextColor(
            when {
                c.isSelected -> onPrimary
                !c.inMonth -> textTertiary
                else -> textPrimary
            }
        )
        // 农历小字：选中 -> 半透明白；节气 -> Primary；其余弱化
        holder.lunar.setTextColor(
            when {
                c.isSelected -> onPrimary
                l.term != null -> primary
                else -> textTertiary
            }
        )
        holder.lunar.alpha = if (c.isSelected) 0.85f else 1f

        // 背景：选中 -> 实心圆（Primary）；今天 -> 浅色圆（primary_container）；其余无
        when {
            c.isSelected -> {
                holder.bg.setBackgroundResource(R.drawable.bg_day_circle)
                holder.bg.visibility = View.VISIBLE
            }
            c.isToday -> {
                holder.bg.setBackgroundResource(R.drawable.bg_day_today)
                holder.bg.visibility = View.VISIBLE
            }
            else -> holder.bg.visibility = View.INVISIBLE
        }

        holder.itemView.setOnClickListener { onPick(c) }
    }
}
