package com.example.suntime.fragment

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.suntime.adapter.WorldClockAdapter
import com.example.suntime.databinding.FragmentClockBinding
import com.example.suntime.util.CityData
import com.example.suntime.util.DateCalc
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
    private lateinit var tick: Runnable

    private val baseTimeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    private val baseCal = Calendar.getInstance()

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
        binding.rvWorldClock.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWorldClock.adapter = adapter
        binding.rvWorldClock.addItemDecoration(LightDividerDecoration(requireContext()))

        binding.etCitySearch.addTextChangedListener { adapter.setKeyword(it?.toString() ?: "") }

        // 本地时间每秒刷新
        tick = Runnable {
            val now = Calendar.getInstance()
            binding.tvLocalTime.text = String.format(
                Locale.CHINA, "%02d:%02d:%02d",
                now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND)
            )
            val w = arrayOf("日", "一", "二", "三", "四", "五", "六")[now.get(Calendar.DAY_OF_WEEK) - 1]
            binding.tvLocalDate.text = String.format(
                Locale.CHINA, "%d年%d月%d日 星期%s",
                now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH), w
            )
            handler.postDelayed(tick, 1000)
        }
        handler.post(tick)

        // 基准时间选择
        updateBaseLabel()
        binding.btnPickBase.setOnClickListener { pickBaseTime() }

        binding.btnCalcTime.setOnClickListener { doCalc() }

        // 日期差值
        binding.btnPickDateA.setOnClickListener { pickDate { y, m, d -> dateA = Triple(y, m, d); binding.btnPickDateA.text = "起始：$y-$m-$d"; calcDiff() } }
        binding.btnPickDateB.setOnClickListener { pickDate { y, m, d -> dateB = Triple(y, m, d); binding.btnPickDateB.text = "结束：$y-$m-$d"; calcDiff() } }
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
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss E", Locale.CHINA)
        binding.tvCalcResult.text = "结果：${fmt.format(result.time)}\n（距今基准 ${if (sign > 0) "+" else ""}${days}天${hours}时${mins}分）"
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
        _binding = null
        super.onDestroyView()
    }
}
