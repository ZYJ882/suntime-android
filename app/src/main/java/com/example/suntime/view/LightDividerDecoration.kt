package com.example.suntime.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.suntime.R

/**
 * 世界时钟轻量列表的细分割线：
 * - 仅在条目之间绘制 1px 分割线
 * - 左侧留出与城市名对齐的缩进
 * - 颜色走主题 divider token
 */
class LightDividerDecoration(context: Context, private val indentDp: Int = 44) :
    RecyclerView.ItemDecoration() {

    private val paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.divider)
        strokeWidth = context.resources.displayMetrics.density * 1f
    }
    private val indentPx = (indentDp * context.resources.displayMetrics.density).toInt()

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val left = parent.paddingLeft + indentPx
        val right = parent.width - parent.paddingRight
        val count = parent.childCount
        for (i in 0 until count) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams
            val top = child.bottom + params.bottomMargin
            val bottom = top + paint.strokeWidth
            if (child.bottom < parent.height && top > 0) {
                c.drawLine(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
            }
        }
    }
}
