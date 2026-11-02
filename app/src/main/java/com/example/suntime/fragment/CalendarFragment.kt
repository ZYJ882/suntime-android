package com.example.suntime.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.suntime.R
import com.example.suntime.adapter.CalendarDayAdapter
import com.example.suntime.adapter.CalendarYearAdapter
import com.example.suntime.adapter.DayCell
import com.example.suntime.databinding.FragmentCalendarBinding
import com.example.suntime.util.DateCalc
import com.example.suntime.util.LunarCalendar
import com.example.suntime.util.Motion
import java.util.Calendar

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private enum class CalView { YEAR, MONTH, DAY }

    private var viewMode = CalView.MONTH

    private var curYear = 0
    private var curMonth = 0
    private var selYear = 0
    private var selMonth = 0
    private var selDay = 0

    private lateinit var dayAdapter: CalendarDayAdapter

    /** 已构建过年视图的年份；同一年切换视图不重建，避免重复构建造成卡顿。 */
    private var renderedYear = 0

    /** 视图切换动画的延迟任务，连续点击时先取消，避免状态错乱。 */
    private var pendingViewSwap: Runnable? = null

    private var dateC: Triple<Int, Int, Int>? = null
    private var dateD: Triple<Int, Int, Int>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 恢复状态（进程被回收 / 页面重建后不重置到「今天」）
        val now = Calendar.getInstance()
        curYear = savedInstanceState?.getInt(KEY_YEAR, now.get(Calendar.YEAR)) ?: now.get(Calendar.YEAR)
        curMonth = savedInstanceState?.getInt(KEY_MONTH, now.get(Calendar.MONTH) + 1) ?: (now.get(Calendar.MONTH) + 1)
        selYear = savedInstanceState?.getInt(KEY_SEL_YEAR, curYear) ?: curYear
        selMonth = savedInstanceState?.getInt(KEY_SEL_MONTH, curMonth) ?: curMonth
        selDay = savedInstanceState?.getInt(KEY_SEL_DAY, now.get(Calendar.DAY_OF_MONTH)) ?: now.get(Calendar.DAY_OF_MONTH)
        viewMode = savedInstanceState?.getString(KEY_VIEW)
            ?.let { runCatching { CalView.valueOf(it) }.getOrNull() } ?: CalView.MONTH

        dayAdapter = CalendarDayAdapter(requireContext(), mutableListOf()) { cell ->
            selYear = cell.year; selMonth = cell.month; selDay = cell.day
            renderMonth()
            updateDetail()
        }
        binding.rvCalendar.layoutManager = GridLayoutManager(requireContext(), 7)
        binding.rvCalendar.adapter = dayAdapter
        binding.rvCalendar.itemAnimator = null

        binding.btnPrevMonth.setOnClickListener { addMonth(-1) }
        binding.btnNextMonth.setOnClickListener { addMonth(1) }
        binding.btnToday.setOnClickListener {
            val t = Calendar.getInstance()
            curYear = t.get(Calendar.YEAR); curMonth = t.get(Calendar.MONTH) + 1
            selYear = curYear; selMonth = curMonth; selDay = t.get(Calendar.DAY_OF_MONTH)
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

        setupViewSwitcher()

        renderMonth()
        applyView(animate = false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_YEAR, curYear)
        outState.putInt(KEY_MONTH, curMonth)
        outState.putInt(KEY_SEL_YEAR, selYear)
        outState.putInt(KEY_SEL_MONTH, selMonth)
        outState.putInt(KEY_SEL_DAY, selDay)
        outState.putString(KEY_VIEW, viewMode.name)
    }

    // ============ 年 / 月 / 日 视图切换 ============
    private fun setupViewSwitcher() {
        binding.chipYear.setOnClickListener { switchMode(CalView.YEAR) }
        binding.chipMonth.setOnClickListener { switchMode(CalView.MONTH) }
        binding.chipDay.setOnClickListener { switchMode(CalView.DAY) }

        binding.btnPrevYear.setOnClickListener { curYear--; renderYear() }
        binding.btnNextYear.setOnClickListener { curYear++; renderYear() }
        binding.btnThisYear.setOnClickListener { curYear = Calendar.getInstance().get(Calendar.YEAR); renderYear() }

        binding.btnPrevDay.setOnClickListener { addDay(-1) }
        binding.btnNextDay.setOnClickListener { addDay(1) }
    }

    private fun switchMode(mode: CalView) {
        if (viewMode == mode) return
        viewMode = mode
        applyView(animate = true)
    }

    /**
     * 应用当前视图模式。
     *
     * @param animate true 时使用「旧内容淡出 → 切换可见性 → 新内容淡入」的轻量转场。
     *                只用 alpha/translationY，不触发布局动画；系统关闭动画时自动退化为瞬切。
     */
    private fun applyView(animate: Boolean = false) {
        val m = viewMode == CalView.MONTH
        val y = viewMode == CalView.YEAR
        val d = viewMode == CalView.DAY

        // 先把内容准备好，再切换可见性，避免出现空白帧
        when (viewMode) {
            CalView.YEAR -> renderYear()
            CalView.DAY -> { updateDetail(); renderDayTitle() }
            CalView.MONTH -> updateDetail()
        }

        styleChip(binding.chipYear, y)
        styleChip(binding.chipMonth, m)
        styleChip(binding.chipDay, d)

        val targets = arrayOf(
            binding.monthHeader to m,
            binding.monthContent to m,
            binding.yearHeader to y,
            binding.yearContent to y,
            binding.dayHeader to d,
            binding.detailPanel to (m || d)
        )

        // 取消上一次未完成的切换与所有在途动画，避免 alpha 停在中间值
        pendingViewSwap?.let { binding.root.removeCallbacks(it) }
        pendingViewSwap = null
        for ((v, _) in targets) v.animate().cancel()

        if (!animate || !Motion.animationsEnabled(requireContext())) {
            for ((v, show) in targets) {
                v.alpha = 1f
                v.translationY = 0f
                v.visibility = vis(show)
            }
            return
        }

        val toHide = targets.filter { !it.second && it.first.visibility == View.VISIBLE }.map { it.first }
        val toShow = targets.filter {
            it.second && (it.first.visibility != View.VISIBLE || it.first.alpha < 1f)
        }.map { it.first }

        if (toHide.isEmpty()) {
            fadeIn(toShow)
            return
        }

        for (v in toHide) {
            v.animate().cancel()
            v.animate().alpha(0f).setDuration(100L).start()
        }

        val swap = Runnable {
            for (v in toHide) {
                v.visibility = View.GONE
                v.alpha = 1f
                v.translationY = 0f
            }
            fadeIn(toShow)
            pendingViewSwap = null
        }
        pendingViewSwap = swap
        binding.root.postDelayed(swap, 100L)
    }

    private fun fadeIn(views: List<View>) {
        if (views.isEmpty()) return
        val offset = 6f * resources.displayMetrics.density
        for (v in views) {
            v.animate().cancel()
            // 已经可见、只是上次淡出被打断的，从当前 alpha 继续回到 1，避免二次闪一下
            if (v.visibility != View.VISIBLE) {
                v.visibility = View.VISIBLE
                v.alpha = 0f
                v.translationY = offset
            }
            v.animate().alpha(1f).translationY(0f).setDuration(160L).start()
        }
    }

    private fun vis(show: Boolean) = if (show) View.VISIBLE else View.GONE

    private fun styleChip(chip: TextView, selected: Boolean) {
        if (selected) {
            chip.setBackgroundResource(R.drawable.bg_segment_sel)
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_primary))
        } else {
            chip.background = null
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }
    }

    private fun renderYear() {
        binding.tvYearTitle.text = String.format("%d年", curYear)
        if (renderedYear == curYear) return   // 同一年已构建，直接复用
        val columns = if (resources.configuration.screenWidthDp >= 840) 3 else 2
        CalendarYearAdapter.buildInto(binding.llYear, curYear, columns) { y, m ->
            curYear = y; curMonth = m; selYear = y; selMonth = m; selDay = 1
            binding.tvMonthTitle.text = String.format("%d年%d月", curYear, curMonth)
            viewMode = CalView.MONTH
            renderMonth()
            applyView(animate = true)
        }
        renderedYear = curYear

        // 翻年时给 12 宫格一个很短的淡入，掩盖同步重建的瞬间
        if (binding.yearContent.visibility == View.VISIBLE && Motion.animationsEnabled(requireContext())) {
            binding.llYear.animate().cancel()
            binding.llYear.alpha = 0.35f
            binding.llYear.animate().alpha(1f).setDuration(140L).start()
        }
    }

    private fun addDay(delta: Int) {
        val c = Calendar.getInstance().apply {
            clear(); set(selYear, selMonth - 1, selDay); add(Calendar.DAY_OF_MONTH, delta)
        }
        selYear = c.get(Calendar.YEAR); selMonth = c.get(Calendar.MONTH) + 1; selDay = c.get(Calendar.DAY_OF_MONTH)
        updateDetail(); renderDayTitle()
    }

    private fun renderDayTitle() {
        val l = LunarCalendar.solarToLunar(selYear, selMonth, selDay)
        binding.tvDayTitle.text = l.weekday
    }

    // ============ 月视图 ============
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
        val todayY = today.get(Calendar.YEAR)
        val todayM = today.get(Calendar.MONTH) + 1
        val todayD = today.get(Calendar.DAY_OF_MONTH)

        // 单个 Calendar 游标走完 42 格，避免原实现每个循环段各建一个 Calendar
        val cursor = Calendar.getInstance().apply {
            clear(); set(curYear, curMonth - 1, 1); add(Calendar.DAY_OF_MONTH, -firstWeekday)
        }

        val list = ArrayList<DayCell>(42)
        for (i in 0 until 42) {
            val y = cursor.get(Calendar.YEAR)
            val m = cursor.get(Calendar.MONTH) + 1
            val d = cursor.get(Calendar.DAY_OF_MONTH)
            val inMonth = i >= firstWeekday && i < firstWeekday + daysInMonth
            list.add(
                DayCell(
                    y, m, d,
                    inMonth = inMonth,
                    isToday = inMonth && y == todayY && m == todayM && d == todayD,
                    isSelected = inMonth && y == selYear && m == selMonth && d == selDay
                )
            )
            cursor.add(Calendar.DAY_OF_MONTH, 1)
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
        pendingViewSwap?.let { binding.root.removeCallbacks(it) }
        pendingViewSwap = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val KEY_YEAR = "cal_year"
        const val KEY_MONTH = "cal_month"
        const val KEY_SEL_YEAR = "cal_sel_year"
        const val KEY_SEL_MONTH = "cal_sel_month"
        const val KEY_SEL_DAY = "cal_sel_day"
        const val KEY_VIEW = "cal_view"
    }
}
