package com.example.breathein.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class LowMediumHighView : View {
    private val linePaint: Paint = Paint()
    private val milestonePaint: Paint = Paint()
    private val markerPaint: Paint = Paint()
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
        linePaint.color = Color.parseColor("#072B3B")
        linePaint.strokeWidth = 8f

        milestonePaint.color = Color.parseColor("#346A7D")
        milestonePaint.strokeWidth = 12f

        markerPaint.color = Color.RED
        markerPaint.strokeWidth = 12f
        lowMilestonePosition = 200f
        normalMilestonePosition = 300f
        highMilestonePosition = 400f
        markerPosition = 350f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawLine(200f, 300f, 500f, 300f, linePaint)
        canvas.drawLine(200f, 280f, 200f, 320f, milestonePaint)
        canvas.drawLine(300f, 280f, 300f, 320f, milestonePaint)
        canvas.drawLine(400f, 280f, 400f, 320f, milestonePaint)
        canvas.drawLine(500f, 280f, 500f, 320f, milestonePaint)

        when {
            markerPosition < 300f -> markerPaint.color = Color.parseColor("#4CAF50")
            markerPosition < 400f -> markerPaint.color = Color.parseColor("#ECB100")
            else -> markerPaint.color = Color.parseColor("#EC0008")

        }
        canvas.drawPoint(markerPosition, 300f, markerPaint)
    }

    fun updateMarkerPosition(value: Float?, marker1: Float, marker2: Float, marker3: Float, marker4: Float) {
        if (value != null) {
            if(value<marker2){
                markerPosition = 250f
                invalidate()
            }
            else if(value<marker3){
                markerPosition = 350f
                invalidate()
            }
            else if(marker3<value){
                markerPosition = 450f
                invalidate()
            }

            }
        }
    }


