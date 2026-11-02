package com.example.suntime.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.example.suntime.R
import java.util.Calendar

/**
 * 年视图中的迷你月历卡片：整月用 Canvas 一次性绘制，**一个卡片只对应一个 View**。
 *
 * 为什么不用 42 个 TextView：
 * 年视图需要在切换瞬间渲染 12 个月，若每月用 42 个格子控件，会创建 500+ 个 View 并触发
 * 多次测量/布局，主线程必然掉帧。这里改为单个自绘 View，12 个月共 12 个 View，
 * 首帧只需十几次 `drawText`，切换无可感知延迟。
 *
 * 视觉：月份标题 + 星期表头 + 6 行日期；今天主色实心圆高亮，非本月补位日期淡显。
 * 所有尺寸走 dp/sp 换算，颜色取自主题色（浅色/深色自动适配）。
 */
class MiniMonthView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Float = v * density
    private fun sp(v: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)

    private val cTextPrimary = ContextCompat.getColor(context, R.color.text_primary)
    private val cTextTertiary = ContextCompat.getColor(context, R.color.text_tertiary)
    private val cPrimary = ContextCompat.getColor(context, R.color.primary)
    private val cOnPrimary = ContextCompat.getColor(context, R.color.on_primary)

    private val bold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    private val normal = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

    // 卡片内边距与行高（留白是年视图可读性的关键）
    private val padH = dp(13f)
    private val padV = dp(12f)
    private val titleH = dp(26f)
    private val weekH = dp(18f)
    private val rowH = dp(22f)

    private var year = 2000
    private var month = 1
    private var todayY = 0
    private var todayM = 0
    private var todayD = 0

    init {
        setBackgroundResource(R.drawable.bg_mini_month)
        isClickable = true
        isFocusable = true
    }

    /** 绑定要显示的月份；today* 由调用方统一传入，避免每张卡片重复取系统时间。 */
    fun bind(year: Int, month: Int, todayY: Int, todayM: Int, todayD: Int) {
        this.year = year
        this.month = month
        this.todayY = todayY
        this.todayM = todayM
        this.todayD = todayD
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val mode = MeasureSpec.getMode(widthMeasureSpec)
        val w = if (mode == MeasureSpec.UNSPECIFIED) suggestedMinimumWidth
        else MeasureSpec.getSize(widthMeasureSpec)
        val h = (padV * 2 + titleH + weekH + rowH * ROWS).toInt()
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val contentW = width - padH * 2
        if (contentW <= 0f) return
        val cellW = contentW / 7f

        // ---------- 月份标题 ----------
        paint.typeface = bold
        paint.textSize = sp(15f)
        paint.color = cTextPrimary
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(
            "${month}月",
            padH,
            baselineFor(padV + titleH / 2f),
            paint
        )

        // ---------- 星期表头 ----------
        paint.typeface = normal
        paint.textSize = sp(10f)
        paint.color = cTextTertiary
        paint.textAlign = Paint.Align.CENTER
        val weekCenterY = padV + titleH + weekH / 2f
        for (i in 0 until 7) {
            canvas.drawText(WEEK_LABELS[i], padH + cellW * (i + 0.5f), baselineFor(weekCenterY), paint)
        }

        // ---------- 日期网格 ----------
        val first = Calendar.getInstance().apply { clear(); set(year, month - 1, 1) }
        val firstWd = first.get(Calendar.DAY_OF_WEEK) - 1        // 0 = 周日
        val days = first.getActualMaximum(Calendar.DAY_OF_MONTH)
        val prevMonthDays = Calendar.getInstance().apply {
            clear(); timeInMillis = first.timeInMillis
        }.let { c -> c.add(Calendar.DAY_OF_MONTH, -1); c.get(Calendar.DAY_OF_MONTH) }

        val gridTop = padV + titleH + weekH
        paint.textSize = sp(12.5f)

        for (index in 0 until ROWS * 7) {
            val col = index % 7
            val row = index / 7

            val day: Int
            val inMonth: Boolean
            when {
                index < firstWd -> {
                    day = prevMonthDays - firstWd + index + 1
                    inMonth = false
                }
                index < firstWd + days -> {
                    day = index - firstWd + 1
                    inMonth = true
                }
                else -> {
                    day = index - firstWd - days + 1
                    inMonth = false
                }
            }

            val cx = padH + cellW * (col + 0.5f)
            val cellCenterY = gridTop + row * rowH + rowH / 2f
            val isToday = inMonth && year == todayY && month == todayM && day == todayD

            if (isToday) {
                paint.style = Paint.Style.FILL
                paint.color = cPrimary
                canvas.drawCircle(cx, cellCenterY, minOf(cellW, rowH) * 0.46f, paint)
                paint.typeface = bold
                paint.color = cOnPrimary
            } else {
                paint.typeface = normal
                if (inMonth) {
                    paint.color = cTextPrimary
                    paint.alpha = 255
                } else {
                    paint.color = cTextTertiary
                    paint.alpha = 90          // 补位日期淡显，降低视觉噪音
                }
            }

            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(day.toString(), cx, baselineFor(cellCenterY), paint)

            paint.alpha = 255                 // 恢复，避免影响后续绘制
        }
    }

    /** 文字在给定中心线上垂直居中所需的 baseline。 */
    private fun baselineFor(centerY: Float): Float {
        val fm = paint.fontMetrics
        return centerY - (fm.ascent + fm.descent) / 2f
    }

    private companion object {
        const val ROWS = 6
        val WEEK_LABELS = arrayOf("日", "一", "二", "三", "四", "五", "六")
    }
}
