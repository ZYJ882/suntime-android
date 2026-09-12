package com.example.suntime.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
        val num: TextView = v.findViewById(R.id.tvDayNum)
        val lunar: TextView = v.findViewById(R.id.tvLunarNum)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
        return VH(v)
    }

    override fun getItemCount() = cells.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = cells[position]
        val l = LunarCalendar.solarToLunar(c.year, c.month, c.day)
        holder.num.text = c.day.toString()
        holder.lunar.text = l.term ?: l.dayCn

        val color = when {
            c.isSelected -> android.graphics.Color.WHITE
            !c.inMonth -> android.graphics.Color.LTGRAY
            else -> android.graphics.Color.BLACK
        }
        holder.num.setTextColor(color)
        holder.lunar.setTextColor(if (c.isSelected) android.graphics.Color.WHITE else android.graphics.Color.GRAY)

        // 背景
        val bg = when {
            c.isSelected -> android.graphics.Color.parseColor("#1565C0")
            c.isToday -> android.graphics.Color.parseColor("#E3F2FD")
            else -> android.graphics.Color.TRANSPARENT
        }
        holder.itemView.setBackgroundColor(bg)

        holder.itemView.setOnClickListener { onPick(c) }
    }
}
