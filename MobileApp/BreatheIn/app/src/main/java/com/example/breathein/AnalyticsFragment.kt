package com.example.breathein

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.example.breathein.network.MqttHelper
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage
import com.example.breathein.views.LowMediumHighView;
import android.widget.PopupWindow
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.gms.location.*
import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.widget.ToggleButton
import androidx.cardview.widget.CardView
import com.example.breathein.views.LowMediumHighView2
import com.google.gson.Gson
import java.util.ArrayList;

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private lateinit var startbutton: Button


class AnalyticsFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null






    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    private lateinit var mqttHelper: MqttHelper
    private lateinit var sensordata_wrist: sensordata_wrist
    private lateinit var temp: TextView
    private lateinit var hum: TextView
    private lateinit var aqi: TextView
    private lateinit var co2: TextView
    private lateinit var tvoc: TextView
    private lateinit var heartrate: TextView
    private lateinit var respirationrate: TextView
    private lateinit var aqi2: TextView
    private lateinit var pm25: TextView
    private lateinit var pm10: TextView
    private lateinit var ozone: TextView
    private lateinit var nitrogen: TextView
    private lateinit var methane: TextView
    private lateinit var sulfur: TextView
    private lateinit var carbon: TextView
    private lateinit var lineChart: LineChart
    private lateinit var entries: ArrayList<Entry>
    private lateinit var entries2: ArrayList<Entry>
    private lateinit var dataSet: LineDataSet
    private lateinit var dataSet2: LineDataSet
    private var locationCallback: LocationCallback? = null

    private var dataIndex = 0f
    private var dataIndex2 = 0f

    private lateinit var imageView: ImageView
    private lateinit var imageView2: ImageView
    private lateinit var imageView1: ImageView
    private lateinit var popupWindow: PopupWindow
    private lateinit var lowMediumHighView1: LowMediumHighView
    private lateinit var lowMediumHighView2: LowMediumHighView
    private lateinit var lowMediumHighView3: LowMediumHighView
    private lateinit var lowMediumHighView4: LowMediumHighView
    private lateinit var lowMediumHighView5: LowMediumHighView
    private lateinit var lowMediumHighView6: LowMediumHighView
    private lateinit var lowMediumHighView7: LowMediumHighView
    private lateinit var lowMediumHighView8: LowMediumHighView
    private lateinit var lowMediumHighView9: LowMediumHighView
    private lateinit var lowMediumHighView10: LowMediumHighView
    private lateinit var lowMediumHighView11: LowMediumHighView
    private lateinit var lowMediumHighView12: LowMediumHighView
    private lateinit var lowMediumHighView13: LowMediumHighView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var riskdrawing: LowMediumHighView2
    private var isExpanded = false
    private var isExpanded2 = false
    private lateinit var cardview6 : CardView
    private lateinit var cardview5 : CardView
    private lateinit var risk : TextView
    private lateinit var inout : ToggleButton
    private lateinit var rr: TextView
    private lateinit var button: ToggleButton




    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_analytics, container, false)



        temp = view.findViewById(R.id.temperature)
        hum = view.findViewById(R.id.humidity)
        tvoc = view.findViewById(R.id.tvoc)
        co2 = view.findViewById(R.id.co2)
        aqi = view.findViewById(R.id.aqi)
        heartrate = view.findViewById(R.id.heartrate)
        heartrate = view.findViewById(R.id.heartrate)
        aqi2 = view.findViewById(R.id.aqi2)
        ozone = view.findViewById(R.id.ozone)
        nitrogen = view.findViewById(R.id.no2)
        carbon = view.findViewById(R.id.carbonmonoxide)
        sulfur = view.findViewById(R.id.sulfurdioxide)
        pm25 = view.findViewById(R.id.pm25)
        pm10 = view.findViewById(R.id.pm10)
        risk = view.findViewById(R.id.risk)
        lowMediumHighView1 = view.findViewById(R.id.circle1)
        lowMediumHighView2 = view.findViewById(R.id.circle2)
        lowMediumHighView3 = view.findViewById(R.id.circle3)
        lowMediumHighView4 = view.findViewById(R.id.circle4)
        lowMediumHighView5 = view.findViewById(R.id.circle5)
        lowMediumHighView6 = view.findViewById(R.id.circle6)
        lowMediumHighView7 = view.findViewById(R.id.circle7)
        lowMediumHighView9 = view.findViewById(R.id.circle9)
        lowMediumHighView10 = view.findViewById(R.id.circle10)
        lowMediumHighView11 = view.findViewById(R.id.circle11)
        lowMediumHighView12 = view.findViewById(R.id.circle12)
        lowMediumHighView13 = view.findViewById(R.id.circle13)
        cardview6 = view.findViewById(R.id.cardView6)
        cardview5 = view.findViewById(R.id.cardView4)
        imageView = view.findViewById(R.id.imageView2)
        imageView2 = view.findViewById(R.id.imageView3)
        imageView1 = view.findViewById(R.id.imageView1)
        riskdrawing = view.findViewById(R.id.riskdraw)
        inout = view.findViewById(R.id.toggleButton)
        rr = view.findViewById(R.id.rr)
        button = view.findViewById(R.id.toggleButton2)



        inout.setOnClickListener{
            var in_out= "out"
            if (inout.isChecked){
                 in_out = "in"
            }
            else{
                in_out = "out"
            }
            val user = getToken()
            if (user != null) {
                mqttHelper.publish("in_out", user.deviceid+","+in_out, 1, false)
            }
        }

        button.setOnClickListener{
            Log.d("button clicked", "yey we're here")
            if (button.isChecked){

                val user = getToken()


                Log.d("user info gotten", user.toString())
                if (user != null) {
                    mqttHelper.publish("user-triggers", user.deviceid+","+user.restingheartrate+","+user.triggers)
                    mqttHelper.publish(user.deviceid+"/monitor", "Monitor Start")

                }
                else{

                }

                if (ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(),
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
                } else {
                    if (user != null) {
                        startLocationUpdates(user.deviceid)

                    }
                }

            }
            else{
                // End
                val user = getToken()


                Log.d("user info gotten", user.toString())
                if (user != null) {
                    mqttHelper.publish(user.deviceid+"/"+"monitor", "Monitor Stop")

                }
                stopLocationUpdates()
            }
            val user = getToken()
            if (user != null) {
            }
        }

        val allViews = listOf(
            view.findViewById<TextView>(R.id.aqi2),
            view.findViewById<LowMediumHighView>(R.id.circle2),
            view.findViewById<TextView>(R.id.pm25),
            view.findViewById<LowMediumHighView>(R.id.circle3),
            view.findViewById<TextView>(R.id.pm10),
            view.findViewById<LowMediumHighView>(R.id.circle4),
            view.findViewById<TextView>(R.id.ozone),
            view.findViewById<TextView>(R.id.no2),
            view.findViewById<LowMediumHighView>(R.id.circle5),
            view.findViewById<TextView>(R.id.sulfurdioxide),
            view.findViewById<LowMediumHighView>(R.id.circle6),
            view.findViewById<TextView>(R.id.carbonmonoxide),
            view.findViewById<LowMediumHighView>(R.id.circle7),
            view.findViewById<TextView>(R.id.carbonmonoxide),
            view.findViewById<LowMediumHighView>(R.id.circle7),
            view.findViewById<LowMediumHighView>(R.id.View2),
            view.findViewById<LowMediumHighView>(R.id.View)
        )

        val initialViews = listOf(
            view.findViewById<TextView>(R.id.aqi2),
            view.findViewById<LowMediumHighView>(R.id.circle1),
            view.findViewById<LowMediumHighView>(R.id.View),
        )



        allViews.filterNot { initialViews.contains(it) }.forEach { it.visibility = View.GONE }
        imageView2.setImageResource(R.drawable.baseline_keyboard_arrow_down_24)
        cardview6.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        cardview6.requestLayout()

        imageView2.setOnClickListener{
            if (isExpanded) {
                allViews.filterNot { initialViews.contains(it) }.forEach { it.visibility = View.GONE }
                imageView2.setImageResource(R.drawable.baseline_keyboard_arrow_down_24)
                cardview6.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            } else {
                allViews.forEach { it.visibility = View.VISIBLE }
                imageView2.setImageResource(R.drawable.baseline_keyboard_arrow_up_24)
                cardview6.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            cardview6.requestLayout()
            isExpanded = !isExpanded
        }






        imageView.setOnClickListener {
            val inflater = LayoutInflater.from(requireContext())
            val popupView = inflater.inflate(R.layout.popup_layout, null)
            popupWindow = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            popupWindow.isOutsideTouchable = true
            popupWindow.isFocusable = true
            popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
        }



        imageView1.setOnClickListener {
            val inflater = LayoutInflater.from(requireContext())
            val popupView = inflater.inflate(R.layout.popup_layout2, null)
            popupWindow = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            popupWindow.isOutsideTouchable = true
            popupWindow.isFocusable = true
            popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
        }





        val lineChart: LineChart = view.findViewById(R.id.lineChart)

        entries = ArrayList()
        dataSet = LineDataSet(entries, "Label")
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        val lineData = LineData(dataSet)
        lineChart.data = lineData
        dataSet.color = Color.BLACK
        val xAxis: XAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawAxisLine(true)
        xAxis.setDrawGridLines(false)
        lineChart.description.isEnabled = false
        val leftAxis: YAxis = lineChart.axisLeft
        leftAxis.setDrawAxisLine(true)
        leftAxis.setDrawGridLines(false)
        lineChart.axisRight.isEnabled = false
        val legend: Legend = lineChart.legend
        legend.isEnabled = false
        dataSet.setDrawValues(false)

        val lineChart2: LineChart = view.findViewById(R.id.lineChart2)
        entries2 = ArrayList()
        dataSet2 = LineDataSet(entries2, "Label")
        dataSet2.mode = LineDataSet.Mode.CUBIC_BEZIER
        val lineData2 = LineData(dataSet2)
        lineChart2.data = lineData2
        dataSet2.color = Color.BLUE
        val xAxis2: XAxis = lineChart2.xAxis
        xAxis2.position = XAxis.XAxisPosition.BOTTOM
        xAxis2.setDrawAxisLine(true)
        xAxis2.setDrawGridLines(false)
        lineChart2.description.isEnabled = false
        val leftAxis2: YAxis = lineChart2.axisLeft
        leftAxis2.setDrawAxisLine(true)
        leftAxis2.setDrawGridLines(false)
        lineChart2.axisRight.isEnabled = false
        val legend2: Legend = lineChart2.legend
        legend2.isEnabled = false
        dataSet2.setDrawValues(false)



        mqttHelper = MqttHelper("tcp://18.169.68.55:1883", "your_client_id")
        mqttHelper.connect(object : MqttCallbackExtended {
            override fun connectionLost(cause: Throwable?) {
                Log.d("connection", "Connection lost. Attempting to reconnect...")
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val dataString = message?.toString()

                Log.d("package arrived", topic.toString()+dataString.toString())

                if (topic == "sensordata_wrist" && dataString != null){
                    val wristsensorData = parseSensorData_wrist(dataString)
                    if (wristsensorData != null) {
                        temp.text = "Temperature: "+ wristsensorData?.temp+" °C"
                        lowMediumHighView9.updateMarkerPosition(wristsensorData?.temp?.toFloatOrNull(),-10f, 15f, 25f, 80f)

                        hum.text = "Humidity: "+ wristsensorData?.hum+ " %"
                        lowMediumHighView10.updateMarkerPosition(wristsensorData?.hum?.toFloatOrNull(),0f, 30f, 50f, 100f)

                        tvoc.text = "TVOC Levels: "+ wristsensorData?.tvoc
                        lowMediumHighView11.updateMarkerPosition(wristsensorData?.tvoc?.toFloatOrNull(),0f, 200f, 500f, 65000f)

                        co2.text = "CO2 Levels: "+ wristsensorData?.co2
                        lowMediumHighView12.updateMarkerPosition(wristsensorData?.co2?.toFloatOrNull(),400f, 500f, 600f, 1500f)

                        aqi.text = "Air Quality Index: "+ wristsensorData?.aqi
                        lowMediumHighView13.updateMarkerPosition(wristsensorData?.aqi?.toFloatOrNull(),1f, 2f, 3f, 5f)

                        heartrate.text = "Heart Rate: "+ wristsensorData?.bpm
                        wristsensorData?.bpm?.toFloatOrNull()?.let { bpm ->
                            if (entries.size >= 10) {
                                entries.removeAt(0)
                                for (i in entries.indices) {
                                    entries[i].x = i.toFloat()
                                }
                            }
                            dataIndex = entries.size.toFloat()
                            entries.add(Entry(dataIndex, bpm))

                            dataSet.notifyDataSetChanged()
                            lineChart.data.notifyDataChanged()
                            lineChart.notifyDataSetChanged()
                            lineChart.setVisibleXRangeMaximum(10f)
                            lineChart.moveViewToX(dataIndex)
                        }
                    }


                }
                else if(topic == "air_pollution" && dataString != null){
                    val airdata = parseSensorData_air(dataString)
                    aqi2.text = "AQI: "+ airdata?.current_european_aqi
                    lowMediumHighView1.updateMarkerPosition(airdata?.current_european_aqi?.toFloatOrNull(),0f, 40f, 50f, 800f)

                    pm25.text = "PM2.5: " + airdata?.pm2_5_value + " μg/m³"
                    lowMediumHighView2.updateMarkerPosition(airdata?.pm2_5_value?.toFloatOrNull(),0f, 20f, 25f, 800f)

                    pm10.text = "PM10: "+ airdata?.pm10_value+ " μg/m³"
                    lowMediumHighView3.updateMarkerPosition(airdata?.current_european_aqi?.toFloatOrNull(),0f, 40f, 50f, 1200f)

                    ozone.text = "Ozone: "+ airdata?.current_ozone+" μg/m³"
                    lowMediumHighView4.updateMarkerPosition(airdata?.current_ozone?.toFloatOrNull(),0f, 100f, 130f, 800f)

                    nitrogen.text = "Nitrogen Dioxide: "+ airdata?.current_nitrogen_dioxide+" μg/m³"
                    lowMediumHighView5.updateMarkerPosition(airdata?.current_nitrogen_dioxide?.toFloatOrNull(),0f, 90f, 120f, 1000f)

                    sulfur.text = "Sulfur Dioxide: "+ airdata?.current_sulphur_dioxide+" μg/m³"
                    lowMediumHighView6.updateMarkerPosition(airdata?.current_sulphur_dioxide?.toFloatOrNull(),0f, 200f, 350f, 800f)

                    carbon.text = "Carbon Monoxide: "+ airdata?.current_carbon_monoxide+ " μg/m³"
                    lowMediumHighView7.updateMarkerPosition(airdata?.current_carbon_monoxide?.toFloatOrNull(),0f, 10f, 15f, 50f)


                }

                else if(topic == "fuzzyfeedback" && dataString != null) {
//                    val feedbackValue = dataString.toFloatOrNull()
                    val splitValues = dataString.split(",")
                    val feedbackValue = splitValues[0].toFloatOrNull()
                    val feedback = splitValues[1]
                    risk.text = feedbackValue.toString()
                    Log.d("fuzzyfeedback", dataString.toString())
                    riskdrawing.updateMarkerPosition(feedbackValue,0f, 100f)

                    if (feedbackValue != null &&  feedback=="True") {
                        val user = getToken()

                        val alertDialog = AlertDialog.Builder(requireContext())
                            .setTitle("Asthma Attack Alert")
                            .setMessage("You had a high likelihood of an asthma attack. Did you have an asthma attack")
                            .setPositiveButton("Yes") { dialog, _ ->
                                Log.d("User Response", "User chose Yes")
                                if (user != null) {
                                    mqttHelper.publish("attackfeedback", user.deviceid+","+"yes")
                                }
                                dialog.dismiss()
                            }
                            .setNegativeButton("No") { dialog, _ ->
                                Log.d("User Response", "User chose No")
                                if (user != null) {
                                    mqttHelper.publish("attackfeedback", user.deviceid+","+"no")
                                }
                                dialog.dismiss()
                            }
                            .create()

                        alertDialog.setOnShowListener {
                            Log.d("AlertDialog", "Showing AlertDialog")
                        }

                        alertDialog.setOnDismissListener {
                            Log.d("AlertDialog", "Dismissing AlertDialog")
                        }

                        alertDialog.show()

                    }

                }


                else if(topic == "iot" && dataString != null) {
                    val values = dataString.split(",").map { it.trim() }
                    val rr_local = values[1]
                    Log.d("iot- respiration rate", rr_local)
                    rr.text = "Respiration Rate: " + rr_local
                    rr_local?.toFloatOrNull()?.let { rr_local ->
                        if (entries2.size >= 10) {
                            entries2.removeAt(0)
                            for (i in entries2.indices) {
                                entries2[i].x = i.toFloat()
                            }
                        }
                        dataIndex2 = entries2.size.toFloat()
                        entries2.add(Entry(dataIndex2, rr_local))

                        dataSet2.notifyDataSetChanged()
                        lineChart2.data.notifyDataChanged()
                        lineChart2.notifyDataSetChanged()
                        lineChart2.setVisibleXRangeMaximum(10f)
                        lineChart2.moveViewToX(dataIndex2)
                    }

                }


            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
            }

            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                Log.d("connection", "Connection completed.")
                mqttHelper.subscribe("sensordata_wrist", 1)
                mqttHelper.subscribe("air_pollution", 1)
                mqttHelper.subscribe("fuzzyfeedback", 1)
                mqttHelper.subscribe("iot", 1)





            }
        })

        lineChart2.invalidate()

        lineChart.invalidate()

        return view
    }


    private fun startLocationUpdates(deviceid: String) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        val locationRequest = LocationRequest.create().apply {
            interval = 300000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    val locationString = "${location.latitude},${location.longitude}"
                    Log.d("LocationUpdates", "Location updates sent to server " + deviceid+ " "+locationString)

                    mqttHelper.publish("location", deviceid+","+locationString, 1, false)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("LocationUpdates", "Location permissions not granted")
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        Log.d("LocationUpdates", "Location updates requested")

    }


    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            Log.d("LocationUpdates", "Location updates stopped")
        }
    }

    private fun parseSensorData_wrist(dataString: String?): sensordata_wrist? {
        if (dataString.isNullOrEmpty()) {
            return null
        }

        val values = dataString.split(",").map { it.trim() }

        val user = getToken()
        if (user != null) {
            if (values[0]==user.deviceid){
                if (values.size >= 1) {
                    val sensordata_wrist = sensordata_wrist(temp = values[1], hum= values[2], aqi = values[3], tvoc = values[4], co2 = values[5], bpm = values[6])
                    Log.d("package parsed", sensordata_wrist.toString())
                    return sensordata_wrist
                }
            }
        }

        return null
    }

    private fun parseSensorData_air(dataString: String?): airdata? {
        if (dataString.isNullOrEmpty()) {
            return null
        }
        val values = dataString.split(",").map { it.trim() }

        if (values.size >= 1) {
            val airdata = airdata(current_european_aqi = values[0], pm2_5_value= values[1], pm10_value = values[2], current_ozone = values[3], current_nitrogen_dioxide = values[4], current_sulphur_dioxide = values[5], current_carbon_monoxide= values[5])
            Log.d("package parsed", airdata.toString())
            return airdata
        }
        return null
    }

    private fun getToken(): user? {
        val sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val userJson = sharedPreferences.getString("user", null)
        return Gson().fromJson(userJson, user::class.java)
    }


    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AnalyticsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
        private const val REQUEST_LOCATION_PERMISSION = 1001

    }
}

