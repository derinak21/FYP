package com.example.breathein.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.speech.tts.TextToSpeech
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.Toast
import java.util.*


class ExpandingCirclesView(context: Context, attrs: AttributeSet? = null) : View(context, attrs), TextToSpeech.OnInitListener {
    private val minRadius = 150f
    private val maxRadius = 350f
    private var timer = 0
    private var lastDisplayedSecond = -1
    private var holdit = false
    private var holdit2 = false

    private var textToSpeech: TextToSpeech? = null
    private var radius1 = 150f
    private var radius2 = 150f
    private var isExpanding1 = false
    private var isExpanding2 = true
    private var isHoldingBreath = false
    private var isCompressing1 = false
    private var isCompressing2 = false


    private var compressionSpeed = 0.5f
    private var holdingTimeMillis = 7000
    private var expansionSpeed = 1f
    private var previousExpansionSpeed1 = 2.5f
    private var previousExpansionSpeed2 = 2f
    private var prevstate : String = ""
    private var lastUpdateTime: Long = 0
    private var last: Long = 0
    private var isHoldingBreath2 = false
    init {
        textToSpeech = TextToSpeech(context, this)
    }

    private enum class CircleState {
        EXPANDING, HOLDING, COMPRESSING
    }
        private var circle1State = CircleState.HOLDING

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(context, "TTS language not supported", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "TTS initialization failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }


    private val circlePaint1 = Paint().apply {
        color = Color.parseColor("#EF3B2E")
        alpha = 100 // Moderate transparency
        style = Paint.Style.FILL
    }

    private val circlePaint2 = Paint().apply {
        color = Color.parseColor("#346A7D")
        alpha = 100 // Moderate transparency
        style = Paint.Style.FILL
    }

    private val startCirclePaint = Paint().apply {
        color = Color.parseColor("#072B3B")
        style = Paint.Style.FILL
    }

    private val endCirclePaint = Paint().apply {
        color = Color.parseColor("#346A7D")
        alpha = 30 // Very transparent
        style = Paint.Style.FILL
    }

    private val edgePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2f


    }




    fun setBreathingExerciseType(exerciseType: String) {
        when (exerciseType) {
//            "4-7-8" -> {
//                expansionSpeed = 0.825f
//                holdingTimeMillis = 7000
//                compressionSpeed = 0.412f
//            }
            "Box Breathing/4-4-4" -> {
                expansionSpeed = 0.800f
                holdingTimeMillis = 4000
                compressionSpeed = 0.800f
            }
        }
    }

    fun startCompressing() {
        isCompressing1 = true
        isExpanding1 = false
        Log.d("ExpandingCirclesView", "isExpanding1 set to false: $isExpanding1")
        holdit = false
        circle1State = CircleState.COMPRESSING

        invalidate()

    }

    fun startExpanding() {
        isExpanding1 = true
        isCompressing1 = false
        Log.d("ExpandingCirclesView", "isExpanding1 set to true: $isExpanding1")
        holdit2 = false
        circle1State = CircleState.EXPANDING

        invalidate()


    }

//    fun startHoldingBreath() {
//        isHoldingBreath2 = true
//        circle1State = CircleState.HOLDING
////        if (isExpanding1){
////            prevstate = "expanding"
////        }
////        else if (isCompressing1){
////            prevstate = "compressing"
////        }
////        isExpanding1 = false
////        isCompressing1 = false
////        Log.d("startholdingbreath", "starting holding")
//        invalidate()
//
//    }

//    fun stopHoldingBreath() {
//        isHoldingBreath2 = false
////        if (prevstate == "expanding"){
////            isExpanding1 = true
////        }
////        else if (prevstate == "compressing"){
////            isCompressing1 = true
////        }
////        expansionSpeed1 = previousExpansionSpeed1
//        invalidate()
//
//    }

    private var customText = ""

    fun setCustomText(text: String) {
        customText = text
        invalidate()
    }



    private var lastSpokenText = ""

    fun resetView() {
        radius1 = 150f
        radius2 = 150f
        isExpanding1 = false
        isExpanding2 = true
        isHoldingBreath = false
        isCompressing1 = false
        isCompressing2 = false
        compressionSpeed = 0.5f
        holdingTimeMillis = 7000
        expansionSpeed = 1f
        previousExpansionSpeed1 = 2.5f
        previousExpansionSpeed2 = 2f
        prevstate = ""
        lastUpdateTime = 0
        last = 0
        isHoldingBreath2 = false
        customText = ""
        timer = 0
        lastSpokenText = ""
        holdit = false
        holdit2 = false

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f



        canvas.drawCircle(centerX, centerY, 150f, startCirclePaint)
        canvas.drawCircle(centerX, centerY, 150f, edgePaint)

        canvas.drawCircle(centerX, centerY, radius1, circlePaint1)
        canvas.drawCircle(centerX, centerY, radius2, circlePaint2)

        canvas.drawCircle(centerX, centerY, 350f, endCirclePaint)
        canvas.drawCircle(centerX, centerY, 350f, edgePaint)
//        if(isHoldingBreath2) {
//        }else if (holdit) {
//        }else if(holdit2){
//        } else if (isExpanding1) {
//            radius1 += expansionSpeed
//        } else if (isCompressing1 ){
//            radius1 -= compressionSpeed
//        }
//        else{
//        }

        when (circle1State) {
            CircleState.EXPANDING -> {
                radius1 += expansionSpeed
                if (radius1 >= 350f) {
                    radius1 = 350f
                    circle1State = CircleState.HOLDING
                }
            }
            CircleState.HOLDING -> {
            }
            CircleState.COMPRESSING -> {
                radius1 -= compressionSpeed
                if (radius1 <= 150f) {
                    radius1 = 150f
                    circle1State = CircleState.HOLDING
                }
            }
        }




        if (isExpanding2 && radius2 >= maxRadius) {
            isExpanding2 = false
            isCompressing2 = true
            lastUpdateTime = System.currentTimeMillis()
            resetTimer()
        }
//        else if (isHoldingBreath && System.currentTimeMillis() - lastUpdateTime >= holdingTimeMillis) {
//            isHoldingBreath = false
//            isCompressing2 = true
//            lastUpdateTime = System.currentTimeMillis()
//            resetTimer()
//        }
        else if (isCompressing2 && radius2 <= minRadius) {
            isCompressing2 = false
            isExpanding2 = true
            lastUpdateTime = System.currentTimeMillis()
            resetTimer()
        }

        if (isExpanding2) {
            radius2 += expansionSpeed
        }
//        else if (isHoldingBreath) {
//
//        }
        else if (isCompressing2) {
            radius2 -= compressionSpeed
        }

        if (radius1 >= 350f ) {
            holdit = true
        }else if(radius1 <=150f){
            holdit2 = true

        }
        else{
            holdit = false
        }

        if (isSecondElapsed()) {
            timer++
        }

        val text = when {
            isExpanding2 -> "Breathe In"
            isHoldingBreath -> "Hold Breath"
            isCompressing2 -> "Breathe Out"
            else -> "Unknown"
        }
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 40f
            textAlign = Paint.Align.CENTER
        }

        val textPaint2 = Paint().apply {
            color = Color.BLACK
            textSize = 30f
            textAlign = Paint.Align.CENTER
        }

        if (text != lastSpokenText) {
            lastSpokenText = text
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }


        canvas.drawText(text, centerX, centerY, textPaint)
        isSecondElapsed()
        canvas.drawText(timer.toString(), centerX, centerY + 50f, textPaint)
        canvas.drawText(customText, centerX, centerY + 400f, textPaint2)

        invalidate()
    }

    private fun resetTimer() {
        timer = 0
        last = 0
    }

    private fun isSecondElapsed(): Boolean {
        val currentTime = System.currentTimeMillis()

        val elapsed = currentTime - last
        if (elapsed >= 1000){
            last = currentTime
        }
        return elapsed >= 1000
    }


}


