package com.example.breathein.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View


class LineChartView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    data class DataPoint(val x: Float, val y: Float)

    private val dataPoints: MutableList<DataPoint> = mutableListOf()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val linePaint = Paint().apply {
            color = Color.BLUE
            strokeWidth = 5f
        }

        val axisPaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 2f
        }

        canvas.drawLine(0f, height.toFloat(), width.toFloat(), height.toFloat(), axisPaint)

        canvas.drawLine(0f, 0f, 0f, height.toFloat(), axisPaint)

        if (dataPoints.size >= 2) {
            for (i in 1 until dataPoints.size) {
                val x1 = dataPoints[i - 1].x
                val y1 = height - dataPoints[i - 1].y
                val x2 = dataPoints[i].x
                val y2 = height - dataPoints[i].y

                canvas.drawLine(x1, y1, x2, y2, linePaint)
            }
        }
    }

    fun setDataPoints(points: List<DataPoint>) {
        dataPoints.clear()
        dataPoints.addAll(points)
        invalidate()
    }
}


