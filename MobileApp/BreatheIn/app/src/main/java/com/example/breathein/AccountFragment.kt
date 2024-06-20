package com.example.breathein

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.view.children
import com.google.gson.Gson
import androidx.fragment.app.Fragment
import com.example.breathein.network.ApiResponse
import com.example.breathein.network.ApiService
import com.example.breathein.network.RetrofitClient
import com.example.breathein.network.medical_info
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.DateFormat
import java.util.Calendar

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class AccountFragment : Fragment() {

    private var param1: String? = null
    private var param2: String? = null
    private lateinit var name: TextView
    private lateinit var age: TextView
    private lateinit var gender: TextView
    private lateinit var medical_info: TextView
    private lateinit var smoker_status: TextView
    private lateinit var calendar: CalendarView
    private lateinit var dateview: TextView
    private lateinit var card: CardView
    private lateinit var donutChart: PieChart
    private lateinit var donutChart2: PieChart
    private lateinit var button: Button


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)
        name = view.findViewById(R.id.textView2)
        age = view.findViewById(R.id.textView4)
        gender = view.findViewById(R.id.textView5)
        medical_info = view.findViewById(R.id.textView6)
        smoker_status = view.findViewById(R.id.textView7)
        calendar = view.findViewById(R.id.calendarView)
        dateview = view.findViewById(R.id.dateTextView)
        card = view.findViewById(R.id.cardView4)
        donutChart = view.findViewById(R.id.donutChart)
        donutChart2 = view.findViewById(R.id.donutChart2)
        button = view.findViewById(R.id.button)

        button.setOnClickListener {
            appearOtherViews()
            card.visibility = View.GONE
        }

        val sleepscore = "80"
        createDonutChart(donutChart, listOf(sleepscore.toFloat(), 100f-sleepscore.toFloat()))
        createDonutChart(donutChart2, listOf(sleepscore.toFloat(), 100f-sleepscore.toFloat()))

        val user = getToken()

        if (user != null) {
            updateUserInfo(user)
        }

        calendar.setOnDateChangeListener { view, year, month, dayOfMonth ->
            val date = "Date: $dayOfMonth/${month + 1}/$year"
            hideOtherViews()
            card.visibility = View.VISIBLE
            dateview.text = date
        }


        return view
    }

    private fun appearOtherViews() {
        view?.findViewById<ViewGroup>(R.id.parentLayout)?.children?.forEach { child ->
            child.visibility = View.VISIBLE
        }
    }
    private fun hideOtherViews() {
        view?.findViewById<ViewGroup>(R.id.parentLayout)?.children?.forEach { child ->
            child.visibility = View.GONE
        }
    }

    private fun createDonutChart(pieChart: PieChart, floatList: List<Float>) {
        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(floatList[0], ""))
        entries.add(PieEntry(floatList[1], ""))
        pieChart.isDrawHoleEnabled = true

        pieChart.setHoleColor(Color.parseColor("#00FFFFFF"))
        pieChart.setHoleRadius(100f);



        val dataSet = PieDataSet(entries, "Deep Sleep")

        dataSet.colors = listOf(
            Color.rgb(156, 173, 220),
            Color.rgb(93, 116, 179),
        )
        dataSet.setDrawValues(false)

        val data = PieData(dataSet)
        pieChart.description = null
        pieChart.legend.isEnabled = false
        pieChart.data = data

        pieChart.invalidate()
    }

    fun updateUserInfo(userInfo: user) {
        Log.d("user", userInfo.toString())
        name.text = "Welcome, ${userInfo.name}!"
        age.text = "Age: ${userInfo.age}"
        gender.text = "Gender: ${userInfo.gender}"
        medical_info.text = "Medical Conditions: ${userInfo.medical}"
        smoker_status.text = "Smoker Status: ${userInfo.smoker_status}"

    }

    private fun getToken(): user? {
        val sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val userJson = sharedPreferences.getString("user", null)
        return Gson().fromJson(userJson, user::class.java)
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AccountFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
