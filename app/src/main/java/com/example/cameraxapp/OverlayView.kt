package com.example.cameraxapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class OverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.RED
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private var lines: List<Pair<Float, Float>> = emptyList()

    fun setLines(newLines: List<Pair<Float, Float>>) {
        lines = newLines
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for ((rho, theta) in lines) {
            val a = Math.cos(theta.toDouble())
            val b = Math.sin(theta.toDouble())
            val x0 = a * rho
            val y0 = b * rho
            val x1 = (x0 + 1000 * (-b)).toFloat()
            val y1 = (y0 + 1000 * (a)).toFloat()
            val x2 = (x0 - 1000 * (-b)).toFloat()
            val y2 = (y0 - 1000 * (a)).toFloat()

            canvas.drawLine(x1, y1, x2, y2, paint)
        }
    }
}
