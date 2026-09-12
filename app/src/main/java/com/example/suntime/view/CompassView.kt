package com.example.suntime.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * 简易指南针 View，setAzimuth(度) 控制指针旋转（0=北）。
 */
class CompassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var azimuth = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

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
        val r = min(w, h) / 2 - 12f

        // 表盘
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#1565C0")
        canvas.drawCircle(cx, cy, r, paint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.WHITE
        paint.strokeWidth = 3f
        canvas.drawCircle(cx, cy, r, paint)

        // 旋转坐标系（指针指向北）
        canvas.save()
        canvas.rotate(-azimuth, cx, cy)

        // 指针：红针指北，白针指南
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#E53935")
        canvas.drawTriangleWithCenter(cx, cy - r * 0.78f, cx - 12f, cy, cx + 12f, cy)
        paint.color = Color.WHITE
        canvas.drawTriangleWithCenter(cx, cy + r * 0.78f, cx - 12f, cy, cx + 12f, cy)

        // 方位文字
        paint.color = Color.WHITE
        paint.textSize = r * 0.16f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText("N", cx, cy - r * 0.5f, paint)
        canvas.drawText("S", cx, cy + r * 0.62f, paint)
        canvas.drawText("E", cx + r * 0.55f, cy + r * 0.06f, paint)
        canvas.drawText("W", cx - r * 0.55f, cy + r * 0.06f, paint)

        canvas.restore()

        // 中心圆点
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawCircle(cx, cy, r * 0.06f, paint)
    }

    private fun Canvas.drawTriangleWithCenter(
        topX: Float, topY: Float, leftX: Float, leftY: Float, rightX: Float, rightY: Float
    ) {
        val path = android.graphics.Path()
        path.moveTo(topX, topY)
        path.lineTo(leftX, leftY)
        path.lineTo(rightX, rightY)
        path.close()
        drawPath(path, paint)
    }
}
