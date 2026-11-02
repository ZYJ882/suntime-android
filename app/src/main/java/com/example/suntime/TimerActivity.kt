package com.example.suntime

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.suntime.databinding.ActivityTimerBinding
import java.util.Locale

/**
 * 计时器（倒计时）页面：时/分/秒输入 → 开始/暂停/继续/重置。
 * 从时钟页右上角秒表图标弹出的菜单进入。
 */
class TimerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTimerBinding

    private var timer: CountDownTimer? = null
    private var remainingMs = 0L
    private var running = false

    /** 上次显示的整秒，避免同一秒内重复 setText。 */
    private var lastShownSec = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnStartPause.setOnClickListener {
            if (running) pause() else start()
        }
        binding.btnReset.setOnClickListener { reset() }

        updateDisplay(remainingMs)
        updateButton()
    }

    private fun readInputMs(): Long {
        val h = binding.etHours.text.toString().toLongOrNull() ?: 0L
        val m = binding.etMinutes.text.toString().toLongOrNull() ?: 0L
        val s = binding.etSeconds.text.toString().toLongOrNull() ?: 0L
        return ((h * 3600 + m * 60 + s) * 1000L).coerceAtLeast(0L)
    }

    private fun start() {
        // 首次开始读取输入；暂停后继续则沿用剩余时间
        if (remainingMs <= 0L) remainingMs = readInputMs()
        if (remainingMs <= 0L) {
            Toast.makeText(this, R.string.timer_input_hint, Toast.LENGTH_SHORT).show()
            return
        }

        // 立即刷新，避免首个 tick（1s）前仍显示旧值
        updateDisplay(remainingMs)

        timer?.cancel()
        // 倒计时只显示到秒，200ms 刷新一次是 5 倍冗余的 UI 更新；改为 1s
        timer = object : CountDownTimer(remainingMs, 1000L) {
            override fun onTick(msUntilFinished: Long) {
                remainingMs = msUntilFinished
                updateDisplay(msUntilFinished)
            }

            override fun onFinish() {
                running = false
                remainingMs = 0L
                updateDisplay(0L)
                updateButton()
                Toast.makeText(this@TimerActivity, R.string.timer_finished, Toast.LENGTH_LONG).show()
            }
        }.start()

        running = true
        updateButton()
    }

    private fun pause() {
        timer?.cancel()
        timer = null
        running = false
        updateDisplay(remainingMs)
        updateButton()
    }

    private fun reset() {
        timer?.cancel()
        timer = null
        running = false
        remainingMs = 0L
        updateDisplay(0L)
        updateButton()
    }

    private fun updateDisplay(ms: Long) {
        // 向上取整，避免开始瞬间显示成少 1 秒
        val totalSec = (ms + 999L) / 1000L
        if (totalSec == lastShownSec) return
        lastShownSec = totalSec
        val h = totalSec / 3600L
        val m = (totalSec % 3600L) / 60L
        val s = totalSec % 60L
        binding.tvTimer.text = String.format(Locale.CHINA, "%02d:%02d:%02d", h, m, s)
    }

    private fun updateButton() {
        val textRes = when {
            running -> R.string.timer_pause
            remainingMs > 0L -> R.string.timer_resume
            else -> R.string.timer_start
        }
        binding.btnStartPause.setText(textRes)
    }

    override fun onDestroy() {
        timer?.cancel()
        timer = null
        super.onDestroy()
    }

    /** 返回时使用与进入对称的轻量转场。 */
    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right)
    }
}
