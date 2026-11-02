package com.example.suntime

import android.os.Bundle
import android.os.SystemClock
import android.view.Choreographer
import androidx.appcompat.app.AppCompatActivity
import com.example.suntime.databinding.ActivityStopwatchBinding
import java.util.Locale

/**
 * 秒表页面：开始/暂停/继续/重置，精确到 0.01 秒。
 * 使用 SystemClock.elapsedRealtime() 计时，避免被系统时间调整影响。
 *
 * 刷新方式（1.11.3）：由固定 31ms 的 Handler 改为 [Choreographer] 逐帧回调。
 * - 回调与屏幕 vsync 对齐，百分秒数字滚动不再抖动、丢帧；
 * - 页面不可见时立即移除回调，不空转。
 */
class StopwatchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStopwatchBinding
    private lateinit var choreographer: Choreographer

    private var running = false
    private var accumulatedMs = 0L
    private var startedAt = 0L

    /** 上次显示的文本，内容未变则不 setText。 */
    private var lastText = ""

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            updateDisplay()
            if (running) choreographer.postFrameCallback(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStopwatchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        choreographer = Choreographer.getInstance()

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
        choreographer.removeFrameCallback(frameCallback)
        choreographer.postFrameCallback(frameCallback)
        updateButton()
    }

    private fun pause() {
        if (!running) return
        accumulatedMs += SystemClock.elapsedRealtime() - startedAt
        running = false
        choreographer.removeFrameCallback(frameCallback)
        updateDisplay()
        updateButton()
    }

    private fun reset() {
        running = false
        choreographer.removeFrameCallback(frameCallback)
        accumulatedMs = 0L
        startedAt = 0L
        lastText = ""
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
        val text = String.format(Locale.CHINA, "%02d:%02d:%02d.%02d", h, m, s, cs)
        if (text != lastText) {
            lastText = text
            binding.tvStopwatch.text = text
        }
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
        choreographer.removeFrameCallback(frameCallback)
        super.onDestroy()
    }

    /** 返回时使用与进入对称的轻量转场。 */
    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right)
    }
}
