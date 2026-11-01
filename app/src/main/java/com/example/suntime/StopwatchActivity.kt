package com.example.suntime

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.suntime.databinding.ActivityStopwatchBinding
import com.example.suntime.util.SettingsStore
import java.util.Locale

/**
 * 秒表页面：开始/暂停/继续/重置，精确到 0.01 秒。
 * 使用 SystemClock.elapsedRealtime() 计时，避免被系统时间调整影响。
 */
class StopwatchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStopwatchBinding

    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var accumulatedMs = 0L
    private var startedAt = 0L

    private val tick = object : Runnable {
        override fun run() {
            updateDisplay()
            if (running) handler.postDelayed(this, 31L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(
            SettingsStore.toNightMode(SettingsStore.getThemeMode(this))
        )
        super.onCreate(savedInstanceState)
        binding = ActivityStopwatchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnStartPause.setOnClickListener {
            if (running) pause() else start()
        }
        binding.btnReset.setOnClickListener { reset() }

        updateDisplay()
        updateButton()
    }

    private fun start() {
        if (running) return
        startedAt = SystemClock.elapsedRealtime()
        running = true
        handler.post(tick)
        updateButton()
    }

    private fun pause() {
        if (!running) return
        accumulatedMs += SystemClock.elapsedRealtime() - startedAt
        running = false
        handler.removeCallbacks(tick)
        updateDisplay()
        updateButton()
    }

    private fun reset() {
        running = false
        handler.removeCallbacks(tick)
        accumulatedMs = 0L
        startedAt = 0L
        updateDisplay()
        updateButton()
    }

    private fun elapsedMs(): Long =
        accumulatedMs + if (running) SystemClock.elapsedRealtime() - startedAt else 0L

    private fun updateDisplay() {
        val ms = elapsedMs()
        val totalSec = ms / 1000L
        val h = totalSec / 3600L
        val m = (totalSec % 3600L) / 60L
        val s = totalSec % 60L
        val cs = (ms % 1000L) / 10L
        binding.tvStopwatch.text = String.format(Locale.CHINA, "%02d:%02d:%02d.%02d", h, m, s, cs)
    }

    private fun updateButton() {
        val textRes = when {
            running -> R.string.stopwatch_pause
            accumulatedMs > 0L -> R.string.stopwatch_resume
            else -> R.string.stopwatch_start
        }
        binding.btnStartPause.setText(textRes)
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }
}
