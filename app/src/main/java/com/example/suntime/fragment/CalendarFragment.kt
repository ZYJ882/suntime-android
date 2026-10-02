package com.example.suntime.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.suntime.R
import com.example.suntime.adapter.CalendarDayAdapter
import com.example.suntime.adapter.DayCell
import com.example.suntime.databinding.FragmentCalendarBinding
import com.example.suntime.util.DateCalc
import com.example.suntime.util.LunarCalendar
import java.util.Calendar

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private var curYear = 0
    private var curMonth = 0
    private var selYear = 0
    private var selMonth = 0
    private var selDay = 0

    private lateinit var dayAdapter: CalendarDayAdapter

    private var dateC: Triple<Int, Int, Int>? = null
    private var dateD: Triple<Int, Int, Int>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val now = Calendar.getInstance()
        curYear = now.get(Calendar.YEAR); curMonth = now.get(Calendar.MONTH) + 1
        selYear = curYear; selMonth = curMonth; selDay = now.get(Calendar.DAY_OF_MONTH)

        dayAdapter = CalendarDayAdapter(mutableListOf()) { cell ->
            selYear = cell.year; selMonth = cell.month; selDay = cell.day
            renderMonth()
            updateDetail()
        }
        binding.rvCalendar.layoutManager = GridLayoutManager(requireContext(), 7)
        binding.rvCalendar.adapter = dayAdapter

        binding.btnPrevMonth.setOnClickListener { addMonth(-1) }
        binding.btnNextMonth.setOnClickListener { addMonth(1) }
        binding.btnToday.setOnClickListener {
            curYear = now.get(Calendar.YEAR); curMonth = now.get(Calendar.MONTH) + 1
            selYear = curYear; selMonth = curMonth; selDay = now.get(Calendar.DAY_OF_MONTH)
            renderMonth(); updateDetail()
        }

        binding.btnJump.setOnClickListener {
            val text = binding.etJump.text.toString().trim()
            val re = Regex("""(\d{4})[-/](\d{1,2})[-/](\d{1,2})""")
            val m = re.matchEntire(text)
            if (m != null) {
                val y = m.groupValues[1].toInt()
                val mo = m.groupValues[2].toInt()
                val d = m.groupValues[3].toInt()
                if (mo in 1..12 && d in 1..31) {
                    curYear = y; curMonth = mo; selYear = y; selMonth = mo; selDay = d
                    renderMonth(); updateDetail()
                } else Toast.makeText(requireContext(), "日期不合法", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(requireContext(), "格式应为 YYYY-MM-DD", Toast.LENGTH_SHORT).show()
        }

        binding.btnPickC.setOnClickListener { pickDate { y, m, d -> dateC = Triple(y, m, d); binding.btnPickC.text = "起始：$y-$m-$d"; calcDiff2() } }
        binding.btnPickD.setOnClickListener { pickDate { y, m, d -> dateD = Triple(y, m, d); binding.btnPickD.text = "结束：$y-$m-$d"; calcDiff2() } }

        renderMonth()
        updateDetail()
    }

    private fun addMonth(delta: Int) {
        var m = curMonth + delta
        var y = curYear
        while (m < 1) { m += 12; y-- }
        while (m > 12) { m -= 12; y++ }
        curYear = y; curMonth = m
        renderMonth()
    }

    private fun renderMonth() {
        binding.tvMonthTitle.text = String.format("%d年%d月", curYear, curMonth)
        val cal = Calendar.getInstance().apply { clear(); set(curYear, curMonth - 1, 1) }
        val firstWeekday = cal.get(Calendar.DAY_OF_WEEK) - 1  // 0=周日
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = Calendar.getInstance()

        val list = mutableListOf<DayCell>()
        // 上月补位
        val prev = Calendar.getInstance().apply { clear(); set(curYear, curMonth - 1, 1); add(Calendar.DAY_OF_MONTH, -firstWeekday) }
        for (i in 0 until firstWeekday) {
            list.add(DayCell(prev.get(Calendar.YEAR), prev.get(Calendar.MONTH) + 1, prev.get(Calendar.DAY_OF_MONTH), false, false, false))
            prev.add(Calendar.DAY_OF_MONTH, 1)
        }
        // 本月
        for (d in 1..daysInMonth) {
            val isToday = (curYear == today.get(Calendar.YEAR) && curMonth == today.get(Calendar.MONTH) + 1 && d == today.get(Calendar.DAY_OF_MONTH))
            val isSel = (curYear == selYear && curMonth == selMonth && d == selDay)
            list.add(DayCell(curYear, curMonth, d, true, isToday, isSel))
        }
        // 下月补位到 42
        var next = Calendar.getInstance().apply { clear(); set(curYear, curMonth - 1, daysInMonth); add(Calendar.DAY_OF_MONTH, 1) }
        while (list.size < 42) {
            list.add(DayCell(next.get(Calendar.YEAR), next.get(Calendar.MONTH) + 1, next.get(Calendar.DAY_OF_MONTH), false, false, false))
            next.add(Calendar.DAY_OF_MONTH, 1)
        }
        dayAdapter.update(list)
    }

    private fun updateDetail() {
        val l = LunarCalendar.solarToLunar(selYear, selMonth, selDay)

        // 主信息：大号日期数字（视觉中心）
        binding.tvDayBig.text = selDay.toString()

        // 次信息：公历全称
        binding.tvGregorian.text = String.format("%d年%d月%d日", selYear, selMonth, selDay)

        // 次信息：农历 · 星期（合并为一行，避免过宽）；节气用品牌蓝突出
        val base = "农历${l.monthCn}月${l.dayCn} · 星期${l.weekday}"
        if (l.term != null) {
            val full = "$base · ${l.term}"
            val sp = SpannableString(full)
            sp.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.primary)),
                base.length + 3, full.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            binding.tvLunarLine.text = sp
        } else {
            binding.tvLunarLine.text = base
        }

        // 辅助信息：干支 / 生肖 / 星座
        binding.tvGanZhi.text = "${l.ganzhiYear}年 · ${l.ganzhiMonth}月 · ${l.ganzhiDay}日 · ${l.zodiac}年 · ${l.constellation}"
    }

    private fun pickDate(onPick: (Int, Int, Int) -> Unit) {
        val c = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d -> onPick(y, m + 1, d) },
            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun calcDiff2() {
        if (dateC == null || dateD == null) return
        val (y1, m1, d1) = dateC!!
        val (y2, m2, d2) = dateD!!
        val diff = DateCalc.diffDays(y1, m1, d1, y2, m2, d2)
        binding.tvDiff2.text = "相差 ${kotlin.math.abs(diff)} 天（${if (diff >= 0) "D 晚于 C" else "C 晚于 D"}）"
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
