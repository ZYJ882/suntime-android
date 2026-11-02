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
 * 性能设计（1.11.3 进一步优化）：
 * - 月份网格（首日星期、天数、42 格日号、今天位置）在 [bind] 时**预计算一次**，
 *   `onDraw` 只做绘制；原实现每次重绘都要创建 2~3 个 Calendar 并重新推算，
 *   12 张卡片在滑动/重绘时会产生上百次临时对象分配；
 * - 复用单个 [Paint.FontMetrics] 对象，避免每段文字都分配一个 FontMetrics；
 * - 颜色、字号在构造/绑定阶段确定，绘制过程零查找。
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
    private val fm = Paint.FontMetrics()

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

    private var month = 1

    /** 42 格预计算数据：日号 + 是否本月 + 今天所在格（-1 表示不在本月）。 */
    private val dayNumbers = IntArray(ROWS * 7)
    private val inMonth = BooleanArray(ROWS * 7)
    private var todayIndex = -1

    init {
        setBackgroundResource(R.drawable.bg_mini_month)
        isClickable = true
        isFocusable = true
    }

    /** 绑定要显示的月份；today* 由调用方统一传入，避免每张卡片重复取系统时间。 */
    fun bind(year: Int, month: Int, todayY: Int, todayM: Int, todayD: Int) {
        this.month = month

        val first = Calendar.getInstance().apply { clear(); set(year, month - 1, 1) }
        val firstWd = first.get(Calendar.DAY_OF_WEEK) - 1          // 0 = 周日
        val days = first.getActualMaximum(Calendar.DAY_OF_MONTH)
        // 上月最后一天：把日期设为 0 即可回退到上月月末
        val prevMonthDays = Calendar.getInstance().apply {
            clear(); set(year, month - 1, 0)
        }.get(Calendar.DAY_OF_MONTH)

        for (i in dayNumbers.indices) {
            when {
                i < firstWd -> {
                    dayNumbers[i] = prevMonthDays - firstWd + i + 1
                    inMonth[i] = false
                }
                i < firstWd + days -> {
                    dayNumbers[i] = i - firstWd + 1
                    inMonth[i] = true
                }
                else -> {
                    dayNumbers[i] = i - firstWd - days + 1
                    inMonth[i] = false
                }
            }
        }
        todayIndex = if (year == todayY && month == todayM) firstWd + todayD - 1 else -1

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
        canvas.drawText("${month}月", padH, baselineFor(padV + titleH / 2f), paint)

        // ---------- 星期表头 ----------
        paint.typeface = normal
        paint.textSize = sp(10f)
        paint.color = cTextTertiary
        paint.textAlign = Paint.Align.CENTER
        val weekCenterY = padV + titleH + weekH / 2f
        for (i in 0 until 7) {
            canvas.drawText(WEEK_LABELS[i], padH + cellW * (i + 0.5f), baselineFor(weekCenterY), paint)
        }

        // ---------- 日期网格（数据已预计算，纯绘制） ----------
        val gridTop = padV + titleH + weekH
        paint.textSize = sp(12.5f)

        for (index in 0 until ROWS * 7) {
            val col = index % 7
            val row = index / 7
            val cx = padH + cellW * (col + 0.5f)
            val cellCenterY = gridTop + row * rowH + rowH / 2f

            if (index == todayIndex) {
                paint.style = Paint.Style.FILL
                paint.color = cPrimary
                canvas.drawCircle(cx, cellCenterY, minOf(cellW, rowH) * 0.46f, paint)
                paint.typeface = bold
                paint.color = cOnPrimary
                paint.alpha = 255
            } else {
                paint.typeface = normal
                if (inMonth[index]) {
                    paint.color = cTextPrimary
                    paint.alpha = 255
                } else {
                    paint.color = cTextTertiary
                    paint.alpha = 90          // 补位日期淡显，降低视觉噪音
                }
            }

            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(dayNumbers[index].toString(), cx, baselineFor(cellCenterY), paint)

            paint.alpha = 255                 // 恢复，避免影响后续绘制
        }
    }

    /** 文字在给定中心线上垂直居中所需的 baseline（复用 FontMetrics，零分配）。 */
    private fun baselineFor(centerY: Float): Float {
        paint.getFontMetrics(fm)
        return centerY - (fm.ascent + fm.descent) / 2f
    }

    private companion object {
        const val ROWS = 6
        val WEEK_LABELS = arrayOf("日", "一", "二", "三", "四", "五", "六")
    }
}
