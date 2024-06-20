package com.example.breathein.network

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.os.Parcelable
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.breathein.R
import com.example.breathein.TrainingFragment
import com.example.breathein.views.ExpandingCirclesView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage



class SensorService : Service() {

    private lateinit var mqttHelper: MqttHelper
    private var trainingFragment: TrainingFragment? = null
    private var circle: ExpandingCirclesView? = null

    companion object {
        private var instance: SensorService? = null

        fun getInstance(): SensorService {
            return instance ?: SensorService().also { instance = it }
        }
    }

    override fun onCreate() {
        super.onCreate()

        mqttHelper = MqttHelper("tcp://18.169.68.55:1883", "your_client_id")

        mqttHelper.connect(object : MqttCallbackExtended {
            override fun connectionLost(cause: Throwable?) {
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val dataString = message?.toString()
                val sensorData = parseSensorData(dataString)

                Log.d("package arrived", dataString.toString())

                if (sensorData != null) {
                    handleSensorData(sensorData)
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
            }

            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                mqttHelper.subscribe("iot", 1)
            }
        })
    }

    fun setCircle(circlepassed: ExpandingCirclesView) {
        circle = circlepassed
    }

    private fun handleSensorData(sensorData: String) {
        val intent = Intent("sensor-data-event")
        intent.action = "sensor-data-event" // Set the action to the desired filter
        intent.putExtra("sensorData", sensorData)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }


    override fun onDestroy() {
        super.onDestroy()

        mqttHelper.disconnect()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun setTrainingFragment(fragment: TrainingFragment) {
        this.trainingFragment = fragment
    }


    private fun parseSensorData(dataString: String?): String? {
        if (dataString.isNullOrEmpty()) {
            return null
        }

        val values = dataString.split(" - ").map { it.trim() }

        if (values.size >= 1) {
            val maximaMinima = values[0]
            Log.d("package parsed", maximaMinima)

            return maximaMinima
        }

        return null
    }


}

