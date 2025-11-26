package com.example.robotcontroller.ui.theme


import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class DrawingOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val boxPaint = Paint().apply {
        color = 0x55FF0000
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val boxes = mutableListOf<RectF>()

    fun setBoxes(list: List<RectF>) {
        boxes.clear()
        boxes.addAll(list)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (r in boxes) {
            canvas.drawRect(r, boxPaint)
        }
    }
}