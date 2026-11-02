package com.termkey.ime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class SwipeTrailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val path = Path()
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 6f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = 0xFF00FF66.toInt()
    }

    private var hasPoints = false

    fun setTrailColor(color: Int) {
        paint.color = color
        invalidate()
    }

    fun startPath(x: Float, y: Float) {
        path.reset()
        path.moveTo(x, y)
        hasPoints = true
        invalidate()
    }

    fun addPoint(x: Float, y: Float) {
        if (!hasPoints) {
            startPath(x, y)
            return
        }
        path.lineTo(x, y)
        hasPoints = true
        invalidate()
    }

    fun clearPath() {
        path.reset()
        hasPoints = false
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (hasPoints) {
            canvas.drawPath(path, paint)
        }
    }
}
