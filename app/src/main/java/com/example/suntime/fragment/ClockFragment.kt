package com.example.suntime.fragment

import android.app.ActivityOptions
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.suntime.R
import com.example.suntime.StopwatchActivity
import com.example.suntime.TimerActivity
import com.example.suntime.adapter.WorldClockAdapter
import com.example.suntime.databinding.FragmentClockBinding
import com.example.suntime.util.CityData
import com.example.suntime.util.DateCalc
import com.example.suntime.util.WindowSize
import com.example.suntime.util.WindowSizeClass
import com.example.suntime.view.LightDividerDecoration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ClockFragment : Fragment() {

    private var _binding: FragmentClockBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: WorldClockAdapter
    private val favZones = mutableSetOf<String>()
    private val prefsName = "world_clock_fav"

    private val handler = Handler(Looper.getMainLooper())
    private val searchHandler = Handler(Looper.getMainLooper())
    private lateinit var tick: Runnable
    private var pendingSearch: Runnable? = null

    private val baseTimeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    private val resultFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss E", Locale.CHINA)
    private val baseCal = Calendar.getInstance()

    /** 日期文案变化很慢（每天一次），缓存后避免每秒 setText 触发布局。 */
    private var lastDateText = ""

    private var dateA: Triple<Int, Int, Int>? = null
    private var dateB: Triple<Int, Int, Int>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentClockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 默认常用城市
        val sp = requireContext().getSharedPreferences(prefsName, 0)
        favZones.addAll(sp.getStringSet("zones", setOf("Asia/Shanghai", "America/New_York", "Europe/London")) ?: emptySet())

        adapter = WorldClockAdapter(CityData.cities, favZones) { city ->
            if (favZones.contains(city.zoneId)) favZones.remove(city.zoneId)
            else favZones.add(city.zoneId)
            sp.edit().putStringSet("zones", favZones).apply()
            adapter.setFavorites(favZones)
        }
        // 世界时钟列数随窗口尺寸等级自适应：紧凑 1 列 / 中等 2 列 / 展开 3 列
        val worldSpan = when (WindowSize.classify(requireContext())) {
            WindowSizeClass.EXPANDED -> 3
            WindowSizeClass.MEDIUM -> 2
            else -> 1
        }
        binding.rvWorldClock.layoutManager = GridLayoutManager(requireContext(), worldSpan)
        binding.rvWorldClock.adapter = adapter
        binding.rvWorldClock.addItemDecoration(LightDividerDecoration(requireContext()))
        // 整表重绑时不播放条目动画，避免搜索/切页时多余闪烁
        binding.rvWorldClock.itemAnimator = null

        // 搜索防抖：逐字符 notifyDataSetChanged 会在 43 行整表重绑时造成输入卡顿
        binding.etCitySearch.addTextChangedListener { text ->
            pendingSearch?.let { searchHandler.removeCallbacks(it) }
            val keyword = text?.toString() ?: ""
            val runnable = Runnable { adapter.setKeyword(keyword) }
            pendingSearch = runnable
            searchHandler.postDelayed(runnable, 120L)
        }

        tick = Runnable {
            updateTimeNow()
            scheduleNextTick()
        }

        // 基准时间选择
        updateBaseLabel()
        binding.btnPickBase.setOnClickListener { pickBaseTime() }

        binding.btnCalcTime.setOnClickListener { doCalc() }

        // 日期差值
        binding.btnPickDateA.setOnClickListener { pickDate { y, m, d -> dateA = Triple(y, m, d); binding.btnPickDateA.text = "起始：$y-$m-$d"; calcDiff() } }
        binding.btnPickDateB.setOnClickListener { pickDate { y, m, d -> dateB = Triple(y, m, d); binding.btnPickDateB.text = "结束：$y-$m-$d"; calcDiff() } }

        // 右上角秒表入口：弹出小菜单，选择「计时器 / 秒表」进入对应页面
        binding.btnClockTools.setOnClickListener { anchor ->
            val popup = PopupMenu(requireContext(), anchor)
            popup.menuInflater.inflate(R.menu.clock_tools_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_timer -> {
                        openTool(TimerActivity::class.java)
                        true
                    }
                    R.id.menu_stopwatch -> {
                        openTool(StopwatchActivity::class.java)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    /** 二级页面统一使用轻量「右移 + 淡入」转场，避免系统默认的整屏滑动。 */
    private fun openTool(target: Class<*>) {
        val options = ActivityOptions.makeCustomAnimation(
            requireContext(), R.anim.slide_in_right, R.anim.fade_out
        ).toBundle()
        startActivity(Intent(requireContext(), target), options)
    }

    override fun onResume() {
        super.onResume()
        // 只在页面可见时秒级刷新，离屏页不再空转 Handler
        updateTimeNow()
        scheduleNextTick()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
    }

    /** 对齐到下一个整秒，避免 postDelayed(1000) 的累计漂移。 */
    private fun scheduleNextTick() {
        handler.removeCallbacks(tick)
        val delay = 1000L - (System.currentTimeMillis() % 1000L)
        handler.postDelayed(tick, delay)
    }

    private fun updateTimeNow() {
        val now = Calendar.getInstance()
        val time = String.format(
            Locale.CHINA, "%02d:%02d:%02d",
            now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND)
        )
        binding.tvLocalTime.text = time

        val date = String.format(
            Locale.CHINA, "%d年%d月%d日 星期%s",
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH) + 1,
            now.get(Calendar.DAY_OF_MONTH),
            WEEK_LABELS[now.get(Calendar.DAY_OF_WEEK) - 1]
        )
        if (date != lastDateText) {
            lastDateText = date
            binding.tvLocalDate.text = date
        }
    }

    private fun updateBaseLabel() {
        binding.btnPickBase.text = "选择基准时间：" + baseTimeFmt.format(baseCal.time)
    }

    private fun pickBaseTime() {
        val ctx = requireContext()
        DatePickerDialog(ctx, { _, y, m, d ->
            baseCal.set(y, m, d)
            TimePickerDialog(ctx, { _, hh, mm ->
                baseCal.set(Calendar.HOUR_OF_DAY, hh)
                baseCal.set(Calendar.MINUTE, mm)
                updateBaseLabel()
            }, baseCal.get(Calendar.HOUR_OF_DAY), baseCal.get(Calendar.MINUTE), true).show()
        }, baseCal.get(Calendar.YEAR), baseCal.get(Calendar.MONTH), baseCal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun doCalc() {
        val days = binding.etDeltaDays.text.toString().toLongOrNull() ?: 0
        val hours = binding.etDeltaHours.text.toString().toLongOrNull() ?: 0
        val mins = binding.etDeltaMin.text.toString().toLongOrNull() ?: 0
        val sign = if (binding.rbAdd.isChecked) 1 else -1
        val totalMs = sign * ((days * 24 + hours) * 60 + mins) * 60 * 1000L
        val result = Calendar.getInstance().apply { timeInMillis = baseCal.timeInMillis + totalMs }
        binding.tvCalcResult.text = "结果：${resultFmt.format(result.time)}\n（距今基准 ${if (sign > 0) "+" else ""}${days}天${hours}时${mins}分）"
    }

    private fun pickDate(onPick: (Int, Int, Int) -> Unit) {
        val c = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d -> onPick(y, m + 1, d) },
            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun calcDiff() {
        if (dateA == null || dateB == null) return
        val (y1, m1, d1) = dateA!!
        val (y2, m2, d2) = dateB!!
        val diff = DateCalc.diffDays(y1, m1, d1, y2, m2, d2)
        binding.tvDateDiff.text = "相差 ${kotlin.math.abs(diff)} 天（${if (diff >= 0) "B 晚于 A" else "A 晚于 B"}）"
        Toast.makeText(requireContext(), "相差 ${kotlin.math.abs(diff)} 天", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        handler.removeCallbacks(tick)
        pendingSearch?.let { searchHandler.removeCallbacks(it) }
        pendingSearch = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        /** 提到常量，避免每秒 new 一个数组。 */
        val WEEK_LABELS = arrayOf("日", "一", "二", "三", "四", "五", "六")
    }
}
