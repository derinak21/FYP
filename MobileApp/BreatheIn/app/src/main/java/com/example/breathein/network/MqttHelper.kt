package com.example.breathein.network

import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.util.Log
import info.mqtt.android.service.Ack
import info.mqtt.android.service.MqttAndroidClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import java.lang.reflect.Field


class MqttHelper(
    private val serverUri: String,
    private val clientId: String
) {

    private lateinit var mqttAndroidClient: MqttAndroidClient

    fun connect(callback: MqttCallbackExtended) {
        mqttAndroidClient = MqttAndroidClient(AppContextProvider.getAppContext(), serverUri, clientId, Ack.AUTO_ACK)

        mqttAndroidClient.setCallback(callback)

        val options = MqttConnectOptions()
        options.isAutomaticReconnect = true

        GlobalScope.launch(Dispatchers.IO) {
            try {
                mqttAndroidClient.connect(options, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {

                        Log.d("Connection", "Connection successful")
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.e("MqttHelper", "Connection failure: ${exception?.message}", exception)
                    }
                })
            } catch (e: Exception) {
                Log.e("MqttHelper", "Exception during connection: ${e.message}", e)
            }
        }
    }



    fun subscribe(topic: String, qos: Int) {
        mqttAndroidClient.subscribe(topic, qos)
    }

    fun disconnect() {
        mqttAndroidClient?.disconnect()
    }

    interface SensorDataListener {
        fun onSensorDataUpdated(lineChartId: Int)
    }

    private var sensorDataListener: SensorDataListener? = null

    fun setSensorDataListener(listener: SensorDataListener) {
        this.sensorDataListener = listener
    }

    private fun updateUI(lineChartId: Int) {
        sensorDataListener?.onSensorDataUpdated(lineChartId)
    }


    fun publish(topic: String, payload: String, qos: Int = 1, retained: Boolean = false) {
        mqttAndroidClient.publish(topic, payload.toByteArray(), qos, retained)
    }




}

object AppContextProvider {
    private lateinit var context: Context

    fun initialize(context: Context) {
        this.context = context
    }

    fun getAppContext(): Context {
        return context
    }
}
