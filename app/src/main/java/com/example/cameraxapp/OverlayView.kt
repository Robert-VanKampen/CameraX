// OverlayView.kt
package com.example.cameraxapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.util.Log

class OverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val linePaint = Paint().apply {
        color = Color.RED
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val paintCameraCenter = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.FILL
    }

    private val paintViewCenter = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
    }

    private var lines: List<Pair<Float, Float>> = emptyList()
    private var frameWidth = 640f
    private var frameHeight = 480f

    private var cropLeft = 0f
    private var cropTop = 0f
    private var cropWidth = 0f
    private var cropHeight = 0f
    private var bufferWidth = 0f
    private var bufferHeight = 0f

    fun setLines(newLines: List<Pair<Float, Float>>, srcWidth: Int, srcHeight: Int) {
        lines = newLines
        frameWidth = srcWidth.toFloat()
        frameHeight = srcHeight.toFloat()
        invalidate()
    }

    fun setTransformInfo(
        cropLeft: Int, cropTop: Int,
        cropWidth: Int, cropHeight: Int,
        bufferWidth: Int, bufferHeight: Int
    ) {
        this.cropLeft = cropLeft.toFloat()
        this.cropTop = cropTop.toFloat()
        this.cropWidth = cropWidth.toFloat()
        this.cropHeight = cropHeight.toFloat()
        this.bufferWidth = bufferWidth.toFloat()
        this.bufferHeight = bufferHeight.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        Log.d("OverlayDebug", "View size: $width x $height")
        Log.d("OverlayDebug", "Frame size: $frameWidth x $frameHeight")
        Log.d("OverlayDebug", "Crop box: left=$cropLeft, top=$cropTop, w=$cropWidth, h=$cropHeight")

        canvas.drawColor(Color.argb(50, 0, 0, 100)) // translucent blue overlay
        canvas.drawLine(100f, 100f, 800f, 800f, linePaint)

        Log.d("OverlayDebug", "Overlay onDraw tried to draw a line.")

        if (frameWidth == 0f || frameHeight == 0f || cropWidth == 0f || cropHeight == 0f) return
        Log.d("OverlayDebug", "Passed return")

        val scaleX = width / cropWidth
        val scaleY = height / cropHeight

        val scale = minOf(scaleX, scaleY)
        Log.d("OverlayDebug", "Scale factors: scaleX=$scaleX, scaleY=$scaleY, scale=$scale")


        val xOffset = -cropLeft * scale
        val yOffset = -cropTop * scale
        Log.d("OverlayDebug", "Offset: xOffset=$xOffset, yOffset=$yOffset")

        for ((rho, theta) in lines) {
            val a = Math.cos(theta.toDouble())
            val b = Math.sin(theta.toDouble())
            val x0 = a * rho
            val y0 = b * rho
            val x1 = (x0 + 1000 * (-b)).toFloat()
            val y1 = (y0 + 1000 * (a)).toFloat()
            val x2 = (x0 - 1000 * (-b)).toFloat()
            val y2 = (y0 - 1000 * (a)).toFloat()

            val rx1 = (frameHeight - y1) * scale + xOffset
            val ry1 = x1 * scale + yOffset
            val rx2 = (frameHeight - y2) * scale + xOffset
            val ry2 = x2 * scale + yOffset

            Log.d("OverlayDebug", "Drawing line:")
            Log.d("OverlayDebug", "  Original rho=$rho, theta=$theta")
            Log.d("OverlayDebug", "  Points: ($rx1, $ry1) to ($rx2, $ry2)")

            canvas.drawLine(rx1, ry1, rx2, ry2, linePaint)
        }

        val cameraCenterX = frameWidth / 2f
        val cameraCenterY = frameHeight / 2f
        val rotatedCameraX = (frameHeight - cameraCenterY) * scale + xOffset
        val rotatedCameraY = cameraCenterX * scale + yOffset
        canvas.drawCircle(rotatedCameraX, rotatedCameraY, 18f, paintCameraCenter)

        val viewCenterX = width / 2f
        val viewCenterY = height / 2f
        canvas.drawRect(
            viewCenterX - 10f, viewCenterY - 10f,
            viewCenterX + 10f, viewCenterY + 10f,
            paintViewCenter
        )
    }
}
