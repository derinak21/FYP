package com.example.breathein.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class LowMediumHighView2 : View {
    private val linePaint: Paint = Paint()
    private val milestonePaint: Paint = Paint()
    private val markerPaint: Paint = Paint()
    private val edgePaint: Paint = Paint()

    private var markerPosition: Float = 0f
    private var lowMilestonePosition: Float = 0f
    private var normalMilestonePosition: Float = 0f
    private var highMilestonePosition: Float = 0f

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        linePaint.color = Color.parseColor("#C8DBE4")
        linePaint.strokeWidth = 100f

        milestonePaint.color = Color.parseColor("#072B3B")
        milestonePaint.strokeWidth = 10f

        edgePaint.color = Color.parseColor("#072B3B")
        edgePaint.strokeWidth = 5f


        markerPaint.color = Color.RED
        markerPaint.strokeWidth = 20f
        lowMilestonePosition = 200f
        normalMilestonePosition = 300f
        highMilestonePosition = 400f
        markerPosition = 350f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawLine(200f, 300f, 800f, 300f, linePaint)
        canvas.drawLine(200f, 250f, 800f, 250f, edgePaint)
        canvas.drawLine(200f, 350f, 800f, 350f, edgePaint)
        canvas.drawLine(200f, 250f, 200f, 350f, edgePaint)
        canvas.drawLine(400f, 250f, 400f, 350f, edgePaint)
        canvas.drawLine(600f, 250f, 600f, 350f, edgePaint)
        canvas.drawLine(800f, 250f, 800f, 350f, edgePaint)

        when {
            markerPosition < 400f -> markerPaint.color = Color.parseColor("#4CAF50")
            markerPosition < 600f -> markerPaint.color = Color.parseColor("#ECB100")
            else -> markerPaint.color = Color.parseColor("#EC0008")

        }
        canvas.drawPoint(markerPosition, 300f, markerPaint)
    }

    fun updateMarkerPosition(value: Float?, minValue: Float, maxValue: Float) {
        if (value != null) {
            val range = maxValue - minValue
            if (range != 0f) {
                markerPosition = 200f + value * 600f / range
                invalidate()
            }
        }
    }

}
