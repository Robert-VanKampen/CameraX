// OverlayView.kt
package com.example.cameraxapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.util.Log
import kotlin.math.cos
import kotlin.math.sin

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

    private val paintCropBox = Paint().apply {
        color = Color.GREEN
        strokeWidth = 3f
        style = Paint.Style.STROKE
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
    private var rotationDegrees = 0

    fun setLines(newLines: List<Pair<Float, Float>>, srcWidth: Int, srcHeight: Int) {
        lines = newLines
        frameWidth = srcWidth.toFloat()
        frameHeight = srcHeight.toFloat()
        invalidate()
    }

    fun setTransformInfo(
        cropLeft: Int, cropTop: Int,
        cropWidth: Int, cropHeight: Int,
        bufferWidth: Int, bufferHeight: Int,
        rotationDegrees: Int
    ) {
        this.cropLeft = cropLeft.toFloat()
        this.cropTop = cropTop.toFloat()
        this.cropWidth = cropWidth.toFloat()
        this.cropHeight = cropHeight.toFloat()
        Log.d("setTransformInfo", "cropLeft: ${this.cropLeft}, cropTop: ${this.cropTop}, cropWidth: ${this.cropWidth}, cropHeight: ${this.cropHeight}")
        this.bufferWidth = bufferWidth.toFloat()
        this.bufferHeight = bufferHeight.toFloat()
        this.rotationDegrees = rotationDegrees
        Log.d("setTransformInfo", "bufferWidth: ${this.bufferWidth}, bufferHeight: ${this.bufferHeight}, rotationDegrees: ${this.rotationDegrees}")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(Color.argb(100, 0, 0, 255)) // translucent blue overlay

        if (frameWidth == 0f || frameHeight == 0f || cropWidth == 0f || cropHeight == 0f) return

        val scaleX = width / cropWidth
        val scaleY = height / cropHeight
        val scale = minOf(scaleX, scaleY)

        val xOffset = -cropLeft * scale
        val yOffset = -cropTop * scale

        fun rotateAndScale(x: Float, y: Float): Pair<Float, Float> {
            val (rx, ry) = when (rotationDegrees) {
                0 -> x to y
                90 -> y to (frameHeight - x)
                180 -> (frameWidth - x) to (frameHeight - y)
                270 -> (frameWidth - y) to x
                else -> x to y
            }
            return rx * scale + xOffset to ry * scale + yOffset
        }

        // Draw debug green crop box
        val topLeft = rotateAndScale(0f, 0f)
        val bottomRight = rotateAndScale(frameWidth, frameHeight)
        canvas.drawRect(
            topLeft.first, topLeft.second,
            bottomRight.first, bottomRight.second,
            paintCropBox
        )

        // Draw detected lines
        for ((rho, theta) in lines) {
            val a = cos(theta.toDouble())
            val b = sin(theta.toDouble())
            val x0 = a * rho
            val y0 = b * rho
            val x1 = (x0 + 1000 * (-b)).toFloat()
            val y1 = (y0 + 1000 * (a)).toFloat()
            val x2 = (x0 - 1000 * (-b)).toFloat()
            val y2 = (y0 - 1000 * (a)).toFloat()

            val (rx1, ry1) = rotateAndScale(x1, y1)
            val (rx2, ry2) = rotateAndScale(x2, y2)
            canvas.drawLine(rx1, ry1, rx2, ry2, linePaint)
        }

        // Draw camera center
        val cameraCenterX = frameWidth / 2f
        val cameraCenterY = frameHeight / 2f
        val (camX, camY) = rotateAndScale(cameraCenterX, cameraCenterY)
        canvas.drawCircle(camX, camY, 18f, paintCameraCenter)

        // Draw view center cross
        val viewCenterX = width / 2f
        val viewCenterY = height / 2f
        val crossSize = 20f
        canvas.drawLine(
            viewCenterX - crossSize, viewCenterY - crossSize,
            viewCenterX + crossSize, viewCenterY + crossSize,
            paintViewCenter
        )
        canvas.drawLine(
            viewCenterX - crossSize, viewCenterY + crossSize,
            viewCenterX + crossSize, viewCenterY - crossSize,
            paintViewCenter
        )
    }
}
