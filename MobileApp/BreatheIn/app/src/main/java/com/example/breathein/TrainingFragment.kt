package com.example.breathein

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import android.widget.Spinner
import android.widget.ProgressBar
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.util.Log
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.breathein.network.ApiResponse
import com.example.breathein.network.ApiService
import com.example.breathein.network.MqttHelper
import com.example.breathein.network.RetrofitClient
import com.example.breathein.network.SensorData
import com.example.breathein.network.SensorService
import com.example.breathein.network.controlsignal
import com.example.breathein.views.LineChartView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage

class TrainingFragment : Fragment() {

    private lateinit var page11Text: TextView
    private lateinit var page12Text: TextView
    private lateinit var page13Text: TextView
    private lateinit var page14Text: TextView
    private lateinit var page2Text: TextView
    private lateinit var spinner: Spinner
    private lateinit var page3Text: TextView
    private lateinit var page4Text: TextView
    private lateinit var progress: ProgressBar
    private lateinit var nextButton: Button
    private lateinit var backButton: Button
    private lateinit var endButton: Button
    private lateinit var startagainButton: Button
    private lateinit var circle: com.example.breathein.views.ExpandingCirclesView
    private lateinit var handler: Handler
    private lateinit var trainingFragment: TrainingFragment
    var currentPage = 1
    private lateinit var lineChartMap: MutableMap<Int, LineChart>
    private lateinit var mqttHelper: MqttHelper
    private lateinit var card: CardView


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_training, container, false)
        mqttHelper = MqttHelper("tcp://18.169.68.55:1883", "your_client_id")
        mqttHelper.connect(object : MqttCallbackExtended {
            override fun connectionLost(cause: Throwable?) {
                Log.d("connection", "Connection lost. Attempting to reconnect...")
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val dataString = message?.toString()
                val sensorData = parseSensorData(dataString)

                Log.d("package arrived", dataString.toString())

                if (topic == "iot" && sensorData != null){
                    handleIncomingData(sensorData)
                }
                else if (topic == "feedback" && sensorData != null){
                    handlefeedback(sensorData)
                }

            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
            }

            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                mqttHelper.subscribe("iot", 1)
                mqttHelper.subscribe("feedback", 1)
            }
        })

        trainingFragment = this
        page11Text = view.findViewById(R.id.page11Text)
        page12Text = view.findViewById(R.id.page12Text)
        page13Text = view.findViewById(R.id.page13Text)
        page14Text = view.findViewById(R.id.page14Text)
        page2Text = view.findViewById(R.id.page2Text)
        spinner = view.findViewById(R.id.spinner)
        page3Text = view.findViewById(R.id.page3Text)
        page4Text = view.findViewById(R.id.page4Text)
        progress = view.findViewById(R.id.progressBar)
        nextButton = view.findViewById(R.id.nextButton)
        backButton = view.findViewById(R.id.backButton)
        startagainButton = view.findViewById(R.id.startagainButton)
        circle = view.findViewById(R.id.circle)
        handler = Handler(Looper.getMainLooper())
        endButton = view.findViewById(R.id.endButton)
        card = view.findViewById(R.id.cardView)
        updatePageVisibility()
        handler = Handler(Looper.getMainLooper())
        nextButton.setOnClickListener {

            if(currentPage == 3){
                sendstartsignal()
            }
            currentPage = currentPage % 5 + 1
            updatePageVisibility()
        }

        backButton.setOnClickListener {
            if (currentPage > 1) {
                currentPage = currentPage - 1
                updatePageVisibility()

            } else if (currentPage == 1) {
                currentPage = 6
                updatePageVisibility()
            }
        }

        endButton.setOnClickListener {
            sendstopsignal()
            currentPage = 6
            updatePageVisibility()
        }

        startagainButton.setOnClickListener {
            currentPage = 1
            updatePageVisibility()
        }


//        handler.postDelayed({
//            checkAndUpdatePage()
//        }, 10000)
        val breathingArray = resources.getStringArray(R.array.breathing_array)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, breathingArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedBreathingTechnique = breathingArray[position]
//                circle.setBreathingExerciseType(selectedBreathingTechnique)

                }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
        return view
    }

    private fun sendstopsignal() {
        val signal: Boolean = false
        val constrolsignalrequest = controlsignal(signal)
        val apiService = RetrofitClient.createService(ApiService::class.java)
        val call = apiService.start_stop(constrolsignalrequest)

        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                val apiResponse = response.body()
                if (response.isSuccessful) {
                    if (apiResponse?.success == true) {
                        showToast("Stopping the device")
                        Log.d("App", "Stopping device")
                    }
                    else{
                        showToast("Server error")
                    }
                } else {
                    showToast("Server error")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("App", "Network error: ${t.message}", t)
                showToast("Network error")
            }
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun sendstartsignal() {
        val signal: Boolean = true
        val constrolsignalrequest = controlsignal(signal)
        val apiService = RetrofitClient.createService(ApiService::class.java)
        val call = apiService.start_stop(constrolsignalrequest)

        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                val apiResponse = response.body()

                if (response.isSuccessful) {
                    if (apiResponse?.success == true) {
                        showToast("Starting the device")
                        Log.d("App", "Starting device")
                    }
                    else{
                        showToast("Server error")
                    }
                } else {
                    showToast("Server error")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("App", "Network error: ${t.message}", t)
                showToast("Network error")
            }
        })
    }

    private fun handleIncomingData(dataString: String?) {
        if (dataString == "Maxima"){
            Log.d("breathing MAXIMA", "COMPRESSING")
            circle.startCompressing()

        }
        else if (dataString == "Minima"){
            Log.d("breathing MINIMA", "EXPANDING")
            circle.startExpanding()


        }

    }

    private fun handlefeedback(dataString: String?) {
        Log.d("feedback printing", dataString.toString())
        dataString?.let {circle?.setCustomText(it)}
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

    override fun onDestroyView() {
        super.onDestroyView()
        mqttHelper.disconnect()
    }

    private fun navigateToAnalyticsFragment() {
        val fragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
        val analyticsFragment = AnalyticsFragment() // Replace with the actual fragment class
        fragmentTransaction.replace(R.id.fragment_container, analyticsFragment, "AnalyticsFragment")
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }


    private fun checkAndUpdatePage() {
        if (isAdded) {
            if (currentPage == 4) {
                Log.d("page", "4")
                currentPage = (currentPage % 5) + 1
                updatePageVisibility()

//                handler.postDelayed({
//                    checkAndUpdatePage()
//                }, 1000)
            }
        }
    }

    private fun updatePageVisibility() {
        backButton.visibility = if (currentPage != 1 && currentPage != 4 && currentPage != 5 && currentPage != 6) View.VISIBLE else View.GONE
        nextButton.visibility = if (currentPage != 5 && currentPage != 6) View.VISIBLE else View.GONE
        page11Text.visibility = if (currentPage == 1) View.VISIBLE else View.GONE
        page12Text.visibility = if (currentPage == 1) View.VISIBLE else View.GONE
        page13Text.visibility = if (currentPage == 1) View.VISIBLE else View.GONE
        page14Text.visibility = if (currentPage == 1) View.VISIBLE else View.GONE
        page2Text.visibility = if (currentPage == 2) View.VISIBLE else View.GONE
        spinner.visibility = if (currentPage == 2) View.VISIBLE else View.GONE
        page3Text.visibility = if (currentPage == 3) View.VISIBLE else View.GONE
        page4Text.visibility = if (currentPage == 4) View.VISIBLE else View.GONE
        progress.visibility = if (currentPage == 4) View.VISIBLE else View.GONE
        circle.visibility = if (currentPage == 5) View.VISIBLE else View.GONE

        if (currentPage==6){
            circle.resetView()
        }
        endButton.visibility = if (currentPage == 5) View.VISIBLE else View.GONE
        card.visibility = if (currentPage == 6) View.VISIBLE else View.GONE
        Log.d("page", currentPage.toString())
    }

}

































































//
//package com.example.breathein
//
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import android.graphics.Color
//import android.os.Build
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.TextView
//import androidx.fragment.app.Fragment
//import android.widget.Spinner
//import android.widget.ProgressBar
//import android.widget.AdapterView
//import android.widget.ArrayAdapter
//import android.os.Handler
//import android.os.Looper
//import android.os.Parcelable
//import android.util.Log
//import android.widget.LinearLayout
//import android.widget.RelativeLayout
//import android.widget.ScrollView
//import android.widget.Toast
//import androidx.localbroadcastmanager.content.LocalBroadcastManager
//import com.example.breathein.network.ApiResponse
//import com.example.breathein.network.ApiService
//import com.example.breathein.network.RetrofitClient
//import com.example.breathein.network.SensorData
//import com.example.breathein.network.SensorService
//import com.example.breathein.network.controlsignal
//import com.example.breathein.views.LineChartView
//import retrofit2.Call
//import retrofit2.Callback
//import retrofit2.Response
//import com.github.mikephil.charting.charts.LineChart
//import com.github.mikephil.charting.components.Description
//import com.github.mikephil.charting.components.XAxis
//import com.github.mikephil.charting.components.YAxis
//import com.github.mikephil.charting.data.Entry
//import com.github.mikephil.charting.data.LineData
//import com.github.mikephil.charting.data.LineDataSet
//
//class TrainingFragment : Fragment() {
//
//    private lateinit var page11Text: TextView
//    private lateinit var page12Text: TextView
//    private lateinit var page13Text: TextView
//    private lateinit var page14Text: TextView
//    private lateinit var page2Text: TextView
//    private lateinit var spinner: Spinner
//    private lateinit var page3Text: TextView
//    private lateinit var page4Text: TextView
//    private lateinit var progress: ProgressBar
//    private lateinit var nextButton: Button
//    private lateinit var backButton: Button
//    private lateinit var endButton: Button
//    private lateinit var changeButton: Button
//    private lateinit var circle: com.example.breathein.views.ExpandingCirclesView
//    private lateinit var handler: Handler
//    private lateinit var scrollView: ScrollView
//    private lateinit var endButton2: Button
//    public lateinit var lineChart1: LineChart
//    public lateinit var lineChart2: LineChart
//    public lateinit var lineChart3: LineChart
//    public lateinit var lineChart4: LineChart
//    public lateinit var lineChart5: LineChart
//    public lateinit var lineChart6: LineChart
//    public lateinit var lineChart7: LineChart
//    public lateinit var lineChart8: LineChart
//    public lateinit var lineChart9: LineChart
//    private lateinit var sensorService: SensorService
//    private lateinit var trainingFragment: TrainingFragment
//
//    var currentPage = 1
//    private lateinit var lineChartMap: MutableMap<Int, LineChart>
//
//
//    private lateinit var sensorServiceIntent: Intent
//    private lateinit var changeButton2: Button
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        val view = inflater.inflate(R.layout.fragment_training, container, false)
//
//
//        // Set the reference to the fragment in the SensorService
//        trainingFragment = this  // Reference to the current fragment
//
//        // Use the singleton pattern to get the instance of SensorService
//        sensorService = SensorService.getInstance()
//        sensorService.setTrainingFragment(trainingFragment)
//        registerReceiver()
//
//        page11Text = view.findViewById(R.id.page11Text)
//        page12Text = view.findViewById(R.id.page12Text)
//        page13Text = view.findViewById(R.id.page13Text)
//        page14Text = view.findViewById(R.id.page14Text)
//        page2Text = view.findViewById(R.id.page2Text)
//        spinner = view.findViewById(R.id.spinner)
//        page3Text = view.findViewById(R.id.page3Text)
//        page4Text = view.findViewById(R.id.page4Text)
//        progress = view.findViewById(R.id.progressBar)
//        nextButton = view.findViewById(R.id.nextButton)
//        backButton = view.findViewById(R.id.backButton)
//        circle = view.findViewById(R.id.circle)
//        handler = Handler(Looper.getMainLooper())
//        endButton = view.findViewById(R.id.endButton)
//        changeButton = view.findViewById(R.id.changeButton)
//        scrollView = view.findViewById(R.id.scrollView)
//        endButton2 = view.findViewById(R.id.endButton2)
//        changeButton2 = view.findViewById(R.id.changeButton2)
//        updatePageVisibility()
//        lineChart1 = view.findViewById(R.id.lineChart1)
//        lineChart2 = view.findViewById(R.id.lineChart2)
//        lineChart3 = view.findViewById(R.id.lineChart3)
//        lineChart4 = view.findViewById(R.id.lineChart4)
//        lineChart5 = view.findViewById(R.id.lineChart5)
//        lineChart6 = view.findViewById(R.id.lineChart6)
//        lineChart7 = view.findViewById(R.id.lineChart7)
//        lineChart8 = view.findViewById(R.id.lineChart8)
//        lineChart9 = view.findViewById(R.id.lineChart9)
//
//        lineChartMap = mutableMapOf(
//            R.id.lineChart1 to lineChart1,
//            R.id.lineChart2 to lineChart2,
//            R.id.lineChart3 to lineChart3,
//            R.id.lineChart4 to lineChart4,
//            R.id.lineChart5 to lineChart5,
//            R.id.lineChart6 to lineChart6,
//            R.id.lineChart7 to lineChart7,
//            R.id.lineChart8 to lineChart8,
//            R.id.lineChart9 to lineChart9
//        )
//        for (lineChart in lineChartMap.values) {
//            val dataSet = LineDataSet(mutableListOf(), "My Line Chart") // Use mutableListOf() instead of emptyList()
//            dataSet.color = Color.parseColor("#072B3B")
//            dataSet.valueTextColor = Color.parseColor("#0F4D66")
//            val lineData = LineData(dataSet)
//            lineChart.data = lineData
//            val xAxis = lineChart.xAxis
//            val yAxis = lineChart.axisLeft
//        }
//
//
//
//
//        handler = Handler(Looper.getMainLooper())
//
//
//        nextButton.setOnClickListener {
//
//            if(currentPage == 3){
//                sendstartsignal()
//                startSensorService()
//            }
//            currentPage = currentPage % 5 + 1
//            updatePageVisibility()
//
//        }
//
//        backButton.setOnClickListener {
//            if (currentPage > 1) {
//                currentPage = currentPage - 1
//                updatePageVisibility()
//
//            } else if (currentPage == 1) {
//                currentPage = 5
//                updatePageVisibility()
//            }
//        }
//
//        changeButton.setOnClickListener {
//            if (currentPage == 5) {
//                currentPage = 6
//            } else {
//                currentPage = 5
//            }
//            updatePageVisibility()
//        }
//
//        endButton.setOnClickListener {
//            sendstopsignal()
//            navigateToAnalyticsFragment()
//        }
//
//        changeButton2.setOnClickListener {
//            if (currentPage == 5) {
//                currentPage = 6
//            } else {
//                currentPage = 5
//            }
//            updatePageVisibility()
//        }
//
//        endButton2.setOnClickListener {
//            sendstopsignal()
//            navigateToAnalyticsFragment()
//        }
//
////        handler.postDelayed({
////            checkAndUpdatePage()
////        }, 10000)
//        val breathingArray = resources.getStringArray(R.array.breathing_array)
//        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, breathingArray)
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        spinner.adapter = adapter
//        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
//                val selectedBreathingTechnique = breathingArray[position]
//            }
//            override fun onNothingSelected(parent: AdapterView<*>?) {
//            }
//        }
//
//
//
//
//        return view
//    }
//
//    private val sensorDataReceiver: BroadcastReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent?) {
//            if (intent?.action == "sensor-data-event") {
//                val sensorData = intent.getParcelableExtra<SensorData>("sensorData")
//
//                if (sensorData != null) {
//                    Log.d("plot", "received data")
//                    plotSensorData(sensorData)
//
//                }
//            }
//        }
//    }
//
//    private fun registerReceiver() {
//        val filter = IntentFilter("sensor-data-event")
//        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(sensorDataReceiver, filter)
//    }
//
//    private fun unregisterReceiver() {
//        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(sensorDataReceiver)
//    }
//
//    private fun sendstopsignal() {
//        val signal: Boolean = false
//        val constrolsignalrequest = controlsignal(signal)
//        val apiService = RetrofitClient.createService(ApiService::class.java)
//        val call = apiService.start_stop(constrolsignalrequest)
//
//        call.enqueue(object : Callback<ApiResponse> {
//            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
//                val apiResponse = response.body()
//                if (response.isSuccessful) {
//                    if (apiResponse?.success == true) {
//                        showToast("Stopping the device")
//                        Log.d("App", "Stopping device")
//                    }
//                    else{
//                        showToast("Server error")
//                    }
//                } else {
//                    showToast("Server error")
//                }
//            }
//
//            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
//                Log.e("App", "Network error: ${t.message}", t)
//                showToast("Network error")
//            }
//        })
//    }
//
//    private fun showToast(message: String) {
//        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
//    }
//
//    private fun sendstartsignal() {
//        val signal: Boolean = true
//
//        val constrolsignalrequest = controlsignal(signal)
//        val apiService = RetrofitClient.createService(ApiService::class.java)
//        val call = apiService.start_stop(constrolsignalrequest)
//
//        call.enqueue(object : Callback<ApiResponse> {
//            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
//                val apiResponse = response.body()
//
//                if (response.isSuccessful) {
//                    if (apiResponse?.success == true) {
//                        showToast("Starting the device")
//                        Log.d("App", "Starting device")
//                    }
//                    else{
//                        showToast("Server error")
//                    }
//                } else {
//                    showToast("Server error")
//                }
//            }
//
//            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
//                Log.e("App", "Network error: ${t.message}", t)
//                showToast("Network error")
//            }
//        })
//
//
//
//
//    }
//
//
//    private fun startSensorService() {
//        sensorServiceIntent = Intent(requireContext(), SensorService::class.java)
//        sensorServiceIntent.putExtra("fragmentTag", "TrainingFragment")
//        requireContext().startService(sensorServiceIntent)
//
//    }
//
//    private fun plotSensorData(sensorData: SensorData) {
//        Log.d("plot", "plotting data")
//
//        for (lineChartId in lineChartMap.keys) {
//            val lineChart = lineChartMap[lineChartId]
//
//            if (lineChart != null) {
//                val yValue = when (lineChartId) {
//                    R.id.lineChart1 -> sensorData.frequency_0
//                    R.id.lineChart2 -> sensorData.frequency_1
//                    R.id.lineChart3 -> sensorData.bmeT
//                    R.id.lineChart4 -> sensorData.bmeRH
//                    R.id.lineChart5 -> sensorData.ensTvoc
//                    R.id.lineChart6 -> sensorData.ensCO2
//                    R.id.lineChart7 -> sensorData.axX
//                    R.id.lineChart8 -> sensorData.axY
//                    R.id.lineChart9 -> sensorData.axZ
//                    else -> 0f
//                }
//
//                val xValue = sensorData.timestamp.toFloat()
//                updateLineChart(lineChart, xValue, yValue)
//            }
//        }
//    }
//
//    private fun updateLineChart(lineChart: LineChart, xValue: Float, yValue: Float) {
//        val data = lineChart.lineData
//
//        val dataSet: LineDataSet
//        if (data != null && data.dataSetCount > 0) {
//            // If the LineDataSet already exists, use it
//            dataSet = data.getDataSetByIndex(0) as LineDataSet
//            dataSet.addEntry(Entry(xValue, yValue))
//            if (dataSet.entryCount > 15) {
//                dataSet.removeEntry(0)
//            }
//            data.notifyDataChanged()
//            lineChart.notifyDataSetChanged()
//        } else {
//            // If the LineDataSet does not exist, create a new one
//            val entries = mutableListOf<Entry>()
//            entries.add(Entry(xValue, yValue))
//
//            dataSet = LineDataSet(entries, "My Line Chart")
//            dataSet.color = Color.parseColor("#072B3B")
//            dataSet.valueTextColor = Color.parseColor("#0F4D66")
//
//            val newLineData = LineData(dataSet)
//            lineChart.data = newLineData
//        }
//
//        lineChart.invalidate()
//    }
//
//
//    override fun onDestroyView() {
//        super.onDestroyView()
////        requireContext().stopService(sensorServiceIntent)
//        unregisterReceiver()
//    }
//
//    private fun navigateToAnalyticsFragment() {
//        val fragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
//        val analyticsFragment = AnalyticsFragment() // Replace with the actual fragment class
//        fragmentTransaction.replace(R.id.fragment_container, analyticsFragment, "AnalyticsFragment")
//        fragmentTransaction.addToBackStack(null)
//        fragmentTransaction.commit()
//    }
//
//
//    private fun checkAndUpdatePage() {
//        if (isAdded) {
//            if (currentPage == 4) {
//                Log.d("page", "4")
//                currentPage = (currentPage % 5) + 1
//                updatePageVisibility()
//
////                handler.postDelayed({
////                    checkAndUpdatePage()
////                }, 1000)
//            }
//        }
//    }
//
//    private fun updatePageVisibility() {
//        backButton.visibility = if (currentPage != 1 && currentPage != 4 && currentPage != 5 && currentPage != 6) View.VISIBLE else View.GONE
//        nextButton.visibility = if (currentPage != 5 && currentPage != 6) View.VISIBLE else View.GONE
//        page11Text.visibility = if (currentPage == 1) View.VISIBLE else View.GONE
//        page12Text.visibility = if (currentPage == 1) View.VISIBLE else View.GONE
//        page13Text.visibility = if (currentPage == 1) View.VISIBLE else View.GONE
//        page14Text.visibility = if (currentPage == 1) View.VISIBLE else View.GONE
//        page2Text.visibility = if (currentPage == 2) View.VISIBLE else View.GONE
//        spinner.visibility = if (currentPage == 2) View.VISIBLE else View.GONE
//        page3Text.visibility = if (currentPage == 3) View.VISIBLE else View.GONE
//        page4Text.visibility = if (currentPage == 4) View.VISIBLE else View.GONE
//        progress.visibility = if (currentPage == 4) View.VISIBLE else View.GONE
//        circle.visibility = if (currentPage == 5) View.VISIBLE else View.GONE
//        endButton.visibility = if (currentPage == 5) View.VISIBLE else View.GONE
//        changeButton.visibility = if (currentPage == 5) View.VISIBLE else View.GONE
//        scrollView.visibility = if (currentPage == 6) View.VISIBLE else View.GONE
//        endButton2.visibility = if (currentPage == 6) View.VISIBLE else View.GONE
//        changeButton2.visibility = if (currentPage == 6) View.VISIBLE else View.GONE
//        Log.d("page", currentPage.toString())
//    }
//
//}





































































































//package com.example.breathein
//
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import android.graphics.Color
//import android.os.Build
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.TextView
//import androidx.fragment.app.Fragment
//import android.widget.Spinner
//import android.widget.ProgressBar
//import android.widget.AdapterView
//import android.widget.ArrayAdapter
//import android.os.Handler
//import android.os.Looper
//import android.os.Parcelable
//import android.util.Log
//import android.widget.LinearLayout
//import android.widget.RelativeLayout
//import android.widget.ScrollView
//import android.widget.Toast
//import androidx.localbroadcastmanager.content.LocalBroadcastManager
//import com.example.breathein.network.ApiResponse
//import com.example.breathein.network.ApiService
//import com.example.breathein.network.MqttHelper
//import com.example.breathein.network.RetrofitClient
//import com.example.breathein.network.SensorData
//import com.example.breathein.network.SensorService
//import com.example.breathein.network.controlsignal
//import com.example.breathein.views.LineChartView
//import retrofit2.Call
//import retrofit2.Callback
//import retrofit2.Response
//import com.github.mikephil.charting.charts.LineChart
//import com.github.mikephil.charting.components.Description
//import com.github.mikephil.charting.components.XAxis
//import com.github.mikephil.charting.components.YAxis
//import com.github.mikephil.charting.data.Entry
//import com.github.mikephil.charting.data.LineData
//import com.github.mikephil.charting.data.LineDataSet
//import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
//import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
//import org.eclipse.paho.client.mqttv3.MqttMessage
//
//class TrainingFragment : Fragment() {
//
//    private lateinit var page11Text: TextView
//    private lateinit var page12Text: TextView
//    private lateinit var page13Text: TextView
//    private lateinit var page14Text: TextView
//    private lateinit var page2Text: TextView
//    private lateinit var spinner: Spinner
//    private lateinit var page3Text: TextView
//    private lateinit var page4Text: TextView
//    private lateinit var progress: ProgressBar
//    private lateinit var nextButton: Button
//    private lateinit var backButton: Button
//    private lateinit var endButton: Button
//    //    private lateinit var changeButton: Button
//    private lateinit var circle: com.example.breathein.views.ExpandingCirclesView
//    private lateinit var handler: Handler
//    //    private lateinit var scrollView: ScrollView
////    private lateinit var endButton2: Button
////    public lateinit var lineChart1: LineChart
////    public lateinit var lineChart2: LineChart
////    public lateinit var lineChart3: LineChart
////    public lateinit var lineChart4: LineChart
////    public lateinit var lineChart5: LineChart
////    public lateinit var lineChart6: LineChart
////    public lateinit var lineChart7: LineChart
////    public lateinit var lineChart8: LineChart
////    public lateinit var lineChart9: LineChart
////    private lateinit var sensorService: SensorService
//    private lateinit var trainingFragment: TrainingFragment
//
//    var currentPage = 1
//    private lateinit var lineChartMap: MutableMap<Int, LineChart>
//    private lateinit var mqttHelper: MqttHelper
//
////    private lateinit var sensorServiceIntent: Intent
////    private lateinit var changeButton2: Button
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        val view = inflater.inflate(R.layout.fragment_training, container, false)
////        registerReceiver()
//
//        mqttHelper = MqttHelper("tcp://18.169.68.55:1883", "your_client_id")
//        mqttHelper.connect(object : MqttCallbackExtended {
//            override fun connectionLost(cause: Throwable?) {
//                Log.d("connection", "Connection lost. Attempting to reconnect...")
//            }
//
//            override fun messageArrived(topic: String?, message: MqttMessage?) {
//                val dataString = message?.toString()
//                val sensorData = parseSensorData(dataString)
//
//                Log.d("package arrived", dataString.toString())
//
//                if (topic == "iot" && sensorData != null){
//                    handleIncomingData(sensorData)
//                }
//                else if (topic == "feedback" && sensorData != null){
//                    handlefeedback(sensorData)
//                }
//
//            }
//
//            override fun deliveryComplete(token: IMqttDeliveryToken?) {
//            }
//
//            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
//                mqttHelper.subscribe("iot", 1)
//                mqttHelper.subscribe("feedback", 1)
//            }
//        })
//
//
//        trainingFragment = this  // Reference to the current fragment
////        sensorService = SensorService.getInstance()
////        sensorService.setTrainingFragment(trainingFragment)
//        page11Text = view.findViewById(R.id.page11Text)
//        page12Text = view.findViewById(R.id.page12Text)
//        page13Text = view.findViewById(R.id.page13Text)
//        page14Text = view.findViewById(R.id.page14Text)
//        page2Text = view.findViewById(R.id.page2Text)
//        spinner = view.findViewById(R.id.spinner)
//        page3Text = view.findViewById(R.id.page3Text)
//        page4Text = view.findViewById(R.id.page4Text)
//        progress = view.findViewById(R.id.progressBar)
//        nextButton = view.findViewById(R.id.nextButton)
//        backButton = view.findViewById(R.id.backButton)
//        circle = view.findViewById(R.id.circle)
//        handler = Handler(Looper.getMainLooper())
//        endButton = view.findViewById(R.id.endButton)
////        changeButton = view.findViewById(R.id.changeButton)
////        scrollView = view.findViewById(R.id.scrollView)
////        endButton2 = view.findViewById(R.id.endButton2)
////        changeButton2 = view.findViewById(R.id.changeButton2)
//        updatePageVisibility()
//        handler = Handler(Looper.getMainLooper())
////        sensorService.setCircle(circle)
//        circle.startCompressing()
//
//        nextButton.setOnClickListener {
//
//            if(currentPage == 3){
//                sendstartsignal()
////                startSensorService()
//            }
//            currentPage = currentPage % 5 + 1
//            updatePageVisibility()
//
//        }
//
//        backButton.setOnClickListener {
//            if (currentPage > 1) {
//                currentPage = currentPage - 1
//                updatePageVisibility()
//
//            } else if (currentPage == 1) {
//                currentPage = 5
//                updatePageVisibility()
//            }
//        }
//
////        changeButton.setOnClickListener {
////            if (currentPage == 5) {
////                currentPage = 6
////            } else {
////                currentPage = 5
////            }
////            updatePageVisibility()
////        }
//
//        endButton.setOnClickListener {
//            sendstopsignal()
//            navigateToAnalyticsFragment()
//        }
//
////        changeButton2.setOnClickListener {
////            if (currentPage == 5) {
////                currentPage = 6
////            } else {
////                currentPage = 5
////            }
////            updatePageVisibility()
////        }
//
////        endButton2.setOnClickListener {
////            sendstopsignal()
////            navigateToAnalyticsFragment()
////        }
//
////        handler.postDelayed({
////            checkAndUpdatePage()
////        }, 10000)
//        val breathingArray = resources.getStringArray(R.array.breathing_array)
//        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, breathingArray)
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        spinner.adapter = adapter
//        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
//                val selectedBreathingTechnique = breathingArray[position]
//                circle.setBreathingExerciseType(selectedBreathingTechnique)
//
//            }
//            override fun onNothingSelected(parent: AdapterView<*>?) {
//            }
//        }
//        return view
//    }
//
//    private fun sendstopsignal() {
//        val signal: Boolean = false
//        val constrolsignalrequest = controlsignal(signal)
//        val apiService = RetrofitClient.createService(ApiService::class.java)
//        val call = apiService.start_stop(constrolsignalrequest)
//
//        call.enqueue(object : Callback<ApiResponse> {
//            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
//                val apiResponse = response.body()
//                if (response.isSuccessful) {
//                    if (apiResponse?.success == true) {
//                        showToast("Stopping the device")
//                        Log.d("App", "Stopping device")
//                    }
//                    else{
//                        showToast("Server error")
//                    }
//                } else {
//                    showToast("Server error")
//                }
//            }
//
//            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
//                Log.e("App", "Network error: ${t.message}", t)
//                showToast("Network error")
//            }
//        })
//    }
//
//    private fun showToast(message: String) {
//        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
//    }
//
//    private fun sendstartsignal() {
//        val signal: Boolean = true
//        val constrolsignalrequest = controlsignal(signal)
//        val apiService = RetrofitClient.createService(ApiService::class.java)
//        val call = apiService.start_stop(constrolsignalrequest)
//
//        call.enqueue(object : Callback<ApiResponse> {
//            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
//                val apiResponse = response.body()
//
//                if (response.isSuccessful) {
//                    if (apiResponse?.success == true) {
//                        showToast("Starting the device")
//                        Log.d("App", "Starting device")
//                    }
//                    else{
//                        showToast("Server error")
//                    }
//                } else {
//                    showToast("Server error")
//                }
//            }
//
//            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
//                Log.e("App", "Network error: ${t.message}", t)
//                showToast("Network error")
//            }
//        })
//    }
//
////    private fun startSensorService() {
////        sensorServiceIntent = Intent(requireContext(), SensorService::class.java)
////        sensorServiceIntent.putExtra("fragmentTag", "TrainingFragment")
////        requireContext().startService(sensorServiceIntent)
////
////    }
//
//    private fun handleIncomingData(dataString: String?) {
//        if (dataString == "Maxima"){
//            Log.d("MAXIMA", "COMPRESSING")
//            circle.startCompressing()
//        }
//        else if (dataString == "Minima"){
//            Log.d("MINIMA", "EXPANDING")
//            circle.startExpanding()
//        }
//    }
//
//    private fun handlefeedback(dataString: String?) {
//        Log.d("feedback printing", dataString.toString())
//        dataString?.let {circle?.setCustomText(it)}
//    }
//
//    private fun parseSensorData(dataString: String?): String? {
//        if (dataString.isNullOrEmpty()) {
//            return null
//        }
//        val values = dataString.split(" - ").map { it.trim() }
//
//        if (values.size >= 1) {
//            val maximaMinima = values[0]
//            Log.d("package parsed", maximaMinima)
//            return maximaMinima
//        }
//        return null
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
////        requireContext().stopService(sensorServiceIntent)
////        unregisterReceiver()
//        mqttHelper.disconnect()
//
//
//    }
//
//    private fun navigateToAnalyticsFragment() {
//        val fragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
//        val analyticsFragment = AnalyticsFragment() // Replace with the actual fragment class
//        fragmentTransaction.replace(R.id.fragment_container, analyticsFragment, "AnalyticsFragment")
//        fragmentTransaction.addToBackStack(null)
//        fragmentTransaction.commit()
//    }
//
//
//    private fun checkAndUpdatePage() {
//        if (isAdded) {
//            if (currentPage == 4) {
//                Log.d("page", "4")
//                currentPage = (currentPage % 5) + 1
//                updatePageVisibility()
//
////                handler.postDelayed({
////                    checkAndUpdatePage()
////                }, 1000)
//            }
//        }
//    }
//
//    private fun updatePageVisibility() {
//        backButton.visibility = if (currentPage != 1 && currentPage != 4 && currentPage != 5 && currentPage != 6) View.VISIBLE else View.GONE
//        nextButton.visibility = if (currentPage != 5 && currentPage != 6) View.VISIBLE else View.GONE
//        page11Text.visibility = if (currentPage == 1) View.VISIBLE else View.GONE
//        page12Text.visibility = if (currentPage == 1) View.VISIBLE else View.GONE
//        page13Text.visibility = if (currentPage == 1) View.VISIBLE else View.GONE
//        page14Text.visibility = if (currentPage == 1) View.VISIBLE else View.GONE
//        page2Text.visibility = if (currentPage == 2) View.VISIBLE else View.GONE
//        spinner.visibility = if (currentPage == 2) View.VISIBLE else View.GONE
//        page3Text.visibility = if (currentPage == 3) View.VISIBLE else View.GONE
//        page4Text.visibility = if (currentPage == 4) View.VISIBLE else View.GONE
//        progress.visibility = if (currentPage == 4) View.VISIBLE else View.GONE
//        circle.visibility = if (currentPage == 5) View.VISIBLE else View.GONE
//        endButton.visibility = if (currentPage == 5) View.VISIBLE else View.GONE
////        changeButton.visibility = if (currentPage == 5) View.VISIBLE else View.GONE
////        scrollView.visibility = if (currentPage == 6) View.VISIBLE else View.GONE
////        endButton2.visibility = if (currentPage == 6) View.VISIBLE else View.GONE
////        changeButton2.visibility = if (currentPage == 6) View.VISIBLE else View.GONE
//        Log.d("page", currentPage.toString())
//    }
//
////    private val sensorDataReceiver: BroadcastReceiver = object : BroadcastReceiver() {
////        override fun onReceive(context: Context?, intent: Intent?) {
////            if (intent?.action == "sensor-data-event") {
////                val sensorData = intent?.getStringExtra("sensorData")
////
////                if (sensorData != null) {
////                    if (sensorData == "Maxima"){
////                        Log.d("MAXIMA", "COMPRESSING")
////                        circle.startCompressing()
////                    }
////                    else if (sensorData == "Minima"){
////                        Log.d("MINIMA", "EXPANDING")
////                        circle.startExpanding()
////                    }
////                }
////            }
////        }
////    }
//
////    private fun registerReceiver() {
////        val filter = IntentFilter("sensor-data-event")
////        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(sensorDataReceiver, filter)
////    }
////
////    private fun unregisterReceiver() {
////        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(sensorDataReceiver)
////    }
//
//}


