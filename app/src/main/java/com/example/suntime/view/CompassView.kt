package com.example.suntime.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.suntime.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 现代化指南针表盘：
 * - 浅色底 + 白色/浅灰表盘（深色模式下自动切换）
 * - 刻度环 + 方位文字（N/E/S/W 为重点，NE/SE/SW/NW 为弱化）
 * - 指针：北针 Primary 蓝、南针红色，中心圆点
 * 仅重绘视觉，setAzimuth 的接口与旋转语义保持不变。
 */
class CompassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var azimuth = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    private val cDial = ContextCompat.getColor(context, R.color.compass_dial)
    private val cRing = ContextCompat.getColor(context, R.color.compass_ring)
    private val cTick = ContextCompat.getColor(context, R.color.compass_tick)
    private val cTickMajor = ContextCompat.getColor(context, R.color.compass_tick_major)
    private val cLabel = ContextCompat.getColor(context, R.color.compass_label)
    private val cCardinal = ContextCompat.getColor(context, R.color.compass_cardinal)
    private val cNeedleN = ContextCompat.getColor(context, R.color.compass_needle_n)
    private val cNeedleS = ContextCompat.getColor(context, R.color.compass_needle_s)
    private val cCenter = ContextCompat.getColor(context, R.color.compass_center)
    private val cCenterRing = ContextCompat.getColor(context, R.color.compass_center_ring)

    fun setAzimuth(deg: Float) {
        azimuth = deg
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2
        val cy = h / 2
        val r = min(w, h) / 2 - 4f

        // 1. 表盘底色（浅色/深色自适应）
        paint.style = Paint.Style.FILL
        paint.color = cDial
        canvas.drawCircle(cx, cy, r, paint)

        // 2. 外圈细环
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = cRing
        canvas.drawCircle(cx, cy, r - 1f, paint)

        // 3. 内圈淡环，增加层次（不加阴影）
        paint.strokeWidth = 1f
        paint.color = cRing
        canvas.drawCircle(cx, cy, r * 0.82f, paint)

        // 4. 刻度（每 15° 一根，主轴略粗）
        for (deg in 0 until 360 step 15) {
            val major = deg % 90 == 0
            val rad = Math.toRadians(deg.toDouble())
            val outer = r - 12f
            val len = if (major) 12f else if (deg % 45 == 0) 9f else 5f
            val inner = outer - len
            paint.strokeWidth = if (major) 2.2f else 1.4f
            paint.color = if (major) cTickMajor else cTick
            canvas.drawLine(
                cx + (outer * sin(rad)).toFloat(), cy - (outer * cos(rad)).toFloat(),
                cx + (inner * sin(rad)).toFloat(), cy - (inner * cos(rad)).toFloat(),
                paint
            )
        }

        // 5. 方位文字（固定不动：表盘静止，仅指针旋转，读数指向顶部三角）
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = r * 0.17f

        val cardinals = mapOf("N" to 0, "E" to 90, "S" to 180, "W" to 270)
        for ((label, deg) in cardinals) {
            val rad = Math.toRadians(deg.toDouble())
            val rr = r * 0.66f
            val tx = cx + (rr * sin(rad)).toFloat()
            val ty = cy - (rr * cos(rad)).toFloat() + paint.textSize * 0.35f
            paint.color = if (label == "N") cNeedleN else cCardinal
            canvas.drawText(label, tx, ty, paint)
        }

        // 次级方位（弱化）
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = r * 0.085f
        paint.color = cLabel
        val minors = mapOf("NE" to 45, "SE" to 135, "SW" to 225, "NW" to 315)
        for ((label, deg) in minors) {
            val rad = Math.toRadians(deg.toDouble())
            val rr = r * 0.66f
            val tx = cx + (rr * sin(rad)).toFloat()
            val ty = cy - (rr * cos(rad)).toFloat() + paint.textSize * 0.35f
            canvas.drawText(label, tx, ty, paint)
        }

        // 6. 指针（随方位反向旋转；北针蓝、南针红）
        canvas.save()
        canvas.rotate(-azimuth, cx, cy)
        drawNeedle(canvas, cx, cy, r, isNorth = true, color = cNeedleN)
        drawNeedle(canvas, cx, cy, r, isNorth = false, color = cNeedleS)
        canvas.restore()

        // 7. 顶部指向标记（固定，提示读数基准方向）
        paint.style = Paint.Style.FILL
        paint.color = cNeedleN
        path.reset()
        path.moveTo(cx, cy - r + 5f)
        path.lineTo(cx - 7f, cy - r + 15f)
        path.lineTo(cx + 7f, cy - r + 15f)
        path.close()
        canvas.drawPath(path, paint)

        // 7. 中心圆点
        paint.style = Paint.Style.FILL
        paint.color = cCenter
        canvas.drawCircle(cx, cy, r * 0.072f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = cCenterRing
        canvas.drawCircle(cx, cy, r * 0.072f, paint)
    }

    /** 指针：从中心到指定方向的细长三角 */
    private fun drawNeedle(canvas: Canvas, cx: Float, cy: Float, r: Float, isNorth: Boolean, color: Int) {
        val tipR = r * 0.70f
        val baseR = r * 0.10f
        val halfW = r * 0.055f
        val dir = if (isNorth) -1f else 1f   // 北：向上；南：向下

        val tipY = cy + dir * tipR
        val baseY = cy + dir * baseR

        paint.style = Paint.Style.FILL
        paint.color = color
        path.reset()
        path.moveTo(cx, tipY)
        path.lineTo(cx - halfW, baseY)
        path.lineTo(cx + halfW, baseY)
        path.close()
        canvas.drawPath(path, paint)
    }
}
