package com.example.suntime.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
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
 * 旋转式指南针表盘（与系统指南针一致的表现方式）：
 *
 * - **表盘随方位反向旋转**：N/E/S/W 与刻度始终指向真实地理方向（北永远在真正的北方）；
 * - **顶部固定指针**：代表「手机正面朝向」，即当前读数所指的方向；
 * - 刻度：每 2° 细刻度、每 10° 中刻度、每 30° 粗刻度并标注度数；
 * - 方位文字：N/E/S/W 加重（N 用暖红），NE/SE/SW/NW 弱化。
 *
 * 性能设计（1.11.3）：
 * 表盘（底色、圆环、180 根刻度、度数、方位文字、北向三角）是**静态内容**，
 * 每次方位变化只是整体旋转。原实现每帧重新绘制约 200 条线/文字，传感器 50Hz 下
 * 主线程压力很大。现在把静态表盘渲染进一张 [Bitmap] 缓存，onDraw 只做一次
 * 「旋转画布 + 贴图」，单帧绘制成本从 ~200 次 draw 降到 1 次。
 *
 * 所有绘制尺寸均由 dp 换算为像素，不使用硬编码像素值。
 */
class CompassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var azimuth = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
    /** 旋转贴图时开启双线性过滤，避免表盘刻度出现锯齿。 */
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    private val density: Float = context.resources.displayMetrics.density
    private fun dp(v: Float): Float = v * density

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

    private val bold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    private val normal = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

    // ---------- 静态表盘位图缓存 ----------
    private var dial: Bitmap? = null
    private var dialW = 0
    private var dialH = 0

    fun setAzimuth(deg: Float) {
        val d = ((deg % 360f) + 360f) % 360f
        if (d == azimuth) return
        azimuth = d
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) recycleDial()
    }

    override fun onDetachedFromWindow() {
        recycleDial()
        super.onDetachedFromWindow()
    }

    private fun recycleDial() {
        dial?.recycle()
        dial = null
        dialW = 0
        dialH = 0
    }

    /** 尺寸变化时重建静态表盘位图；正常帧直接复用。 */
    private fun ensureDial(): Bitmap? {
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return null
        dial?.let { if (dialW == w && dialH == h) return it }

        recycleDial()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        drawDial(Canvas(bmp), w / 2f, h / 2f, min(w, h) / 2f - dp(2f))
        dial = bmp
        dialW = w
        dialH = h
        return bmp
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) / 2f - dp(2f)
        if (r <= dp(8f)) return

        // 静态表盘整体反向旋转：N 始终指向真北
        val bmp = ensureDial() ?: return
        canvas.save()
        canvas.rotate(-azimuth, cx, cy)
        canvas.drawBitmap(bmp, 0f, 0f, bitmapPaint)
        canvas.restore()

        // 8. 顶部固定指针：代表「手机正面朝向」，读数以它对准的表盘刻度为准
        paint.style = Paint.Style.FILL
        paint.color = cNeedleN
        path.reset()
        val tipY = cy - r + dp(1f)
        val baseY = cy - r + dp(12f)
        val halfW = dp(5f)
        path.moveTo(cx, tipY)
        path.lineTo(cx - halfW, baseY)
        path.lineTo(cx + halfW, baseY)
        path.close()
        canvas.drawPath(path, paint)

        // 指针延长细线，强化「指向中心」的视觉引导
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(1.2f)
        paint.color = cCenterRing
        canvas.drawLine(cx, baseY, cx, cy - r * 0.70f, paint)

        // 9. 中心圆：填充 + 淡内圈 + 主色外环（固定，不随表盘旋转）
        val centerR = r * 0.055f
        paint.style = Paint.Style.FILL
        paint.color = cCenter
        canvas.drawCircle(cx, cy, centerR, paint)
        paint.color = cNeedleS
        canvas.drawCircle(cx, cy, centerR * 0.32f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(1.4f)
        paint.color = cCenterRing
        canvas.drawCircle(cx, cy, centerR, paint)
    }

    /** 绘制一次静态表盘：底色 + 圆环 + 刻度 + 度数 + 方位文字 + 北向三角。 */
    private fun drawDial(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        // 1. 表盘底色
        paint.style = Paint.Style.FILL
        paint.color = cDial
        canvas.drawCircle(cx, cy, r, paint)

        // 2. 外圈细环 + 内侧淡环
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(1.2f)
        paint.color = cRing
        canvas.drawCircle(cx, cy, r - dp(0.8f), paint)
        paint.strokeWidth = dp(0.8f)
        canvas.drawCircle(cx, cy, r * 0.80f, paint)

        // 3. 刻度环：2° 细 / 10° 中 / 30° 粗
        val tickOuter = r - dp(5f)
        for (deg in 0 until 360 step 2) {
            val isMajor = deg % 30 == 0
            val isMid = deg % 10 == 0
            val rad = Math.toRadians(deg.toDouble())
            val s = sin(rad).toFloat()
            val c = cos(rad).toFloat()
            val len = when {
                isMajor -> dp(11f)
                isMid -> dp(6.5f)
                else -> dp(3.2f)
            }
            val inner = tickOuter - len
            paint.strokeWidth = when {
                isMajor -> dp(1.6f)
                isMid -> dp(1.0f)
                else -> dp(0.7f)
            }
            paint.color = when {
                isMajor -> cTickMajor
                isMid -> cTick
                else -> cRing
            }
            canvas.drawLine(
                cx + tickOuter * s, cy - tickOuter * c,
                cx + inner * s, cy - inner * c,
                paint
            )
        }

        // 4. 度数标注（每 30°；90 的倍数改用方位字母）
        paint.typeface = normal
        paint.textSize = r * 0.070f
        paint.textAlign = Paint.Align.CENTER
        paint.color = cLabel
        for (deg in 0 until 360 step 30) {
            if (deg % 90 == 0) continue
            val rad = Math.toRadians(deg.toDouble())
            val rr = r * 0.735f
            val tx = cx + rr * sin(rad).toFloat()
            val ty = cy - rr * cos(rad).toFloat() + paint.textSize * 0.35f
            canvas.drawText(deg.toString(), tx, ty, paint)
        }

        // 5. 主方位 N/E/S/W（N 用暖红强调）
        paint.typeface = bold
        paint.textSize = r * 0.112f
        for ((label, deg) in CARDINALS) {
            val rad = Math.toRadians(deg.toDouble())
            val rr = r * 0.60f
            val tx = cx + rr * sin(rad).toFloat()
            val ty = cy - rr * cos(rad).toFloat() + paint.textSize * 0.35f
            paint.color = if (label == "N") cNeedleN else cCardinal
            canvas.drawText(label, tx, ty, paint)
        }

        // 6. 斜方位 NE/SE/SW/NW（弱化一级）
        paint.typeface = normal
        paint.textSize = r * 0.058f
        paint.color = cLabel
        for ((label, deg) in MINORS) {
            val rad = Math.toRadians(deg.toDouble())
            val rr = r * 0.60f
            val tx = cx + rr * sin(rad).toFloat()
            val ty = cy - rr * cos(rad).toFloat() + paint.textSize * 0.32f
            canvas.drawText(label, tx, ty, paint)
        }

        // 7. 北向指示：N 外侧的小三角（随表盘旋转，始终指向真北）
        val nOuter = r - dp(6f)
        val nInner = r - dp(14f)
        val nHalf = dp(3.6f)
        paint.style = Paint.Style.FILL
        paint.color = cNeedleN
        path.reset()
        path.moveTo(cx, cy - nInner)
        path.lineTo(cx - nHalf, cy - nOuter)
        path.lineTo(cx + nHalf, cy - nOuter)
        path.close()
        canvas.drawPath(path, paint)
    }

    private companion object {
        val CARDINALS = listOf("N" to 0, "E" to 90, "S" to 180, "W" to 270)
        val MINORS = listOf("NE" to 45, "SE" to 135, "SW" to 225, "NW" to 315)
    }
}
