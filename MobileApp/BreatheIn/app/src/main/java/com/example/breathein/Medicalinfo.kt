package com.example.breathein

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import com.example.breathein.network.ApiResponse
import com.example.breathein.network.ApiService
import com.example.breathein.network.RetrofitClient
import com.example.breathein.network.medical_info
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Medicalinfo : AppCompatActivity() {

    private lateinit var namet: EditText
    private lateinit var spinnergender: Spinner
    private lateinit var spinnerage: Spinner
    private lateinit var spinnermed: Spinner
    private lateinit var spinnersmoker: Spinner

    private lateinit var gender: String
    private lateinit var age: String
    private lateinit var medical: String
    private lateinit var smoker_status: String
    private lateinit var triggers : String
    private lateinit var deviceid : EditText
    private lateinit var restingheartrate : EditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medicalinfo)

        namet = findViewById(R.id.editTextText)
        spinnergender = findViewById(R.id.spinner1)
        spinnerage = findViewById(R.id.spinner2)
        spinnermed = findViewById(R.id.spinner3)
        spinnersmoker = findViewById(R.id.spinner4)
        deviceid = findViewById(R.id.editTextText2)
        restingheartrate = findViewById(R.id.editTextText3)

        val checkBoxes = arrayOf(
            findViewById<CheckBox>(R.id.checkBox1),
            findViewById<CheckBox>(R.id.checkBox2),
            findViewById<CheckBox>(R.id.checkBox3),
            findViewById<CheckBox>(R.id.checkBox4),
            findViewById<CheckBox>(R.id.checkBox5),
            findViewById<CheckBox>(R.id.checkBox6),
            findViewById<CheckBox>(R.id.checkBox7),
            findViewById<CheckBox>(R.id.checkBox8)
        )


        triggers = ""
        for (checkBox in checkBoxes) {
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    if (triggers == ""){
                        triggers = "${checkBox.text.toString()}"

                    }
                    else {
                        triggers = "$triggers,${checkBox.text.toString()}"
                    }
                } else {
                    triggers = triggers.replace(",${checkBox.text.toString()}", "")
                }
            }
        }






        val genderarray = resources.getStringArray(R.array.gender_array)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genderarray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnergender.adapter = adapter
        spinnergender.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                gender = genderarray[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        val agearray = resources.getStringArray(R.array.age_array)
        val adapter2 = ArrayAdapter(this, android.R.layout.simple_spinner_item, agearray)
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerage.adapter = adapter2
        spinnerage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                age = agearray[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        val medarray = resources.getStringArray(R.array.medical_conditions)
        val adapter3 = ArrayAdapter(this, android.R.layout.simple_spinner_item, medarray)
        adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnermed.adapter = adapter3
        spinnermed.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                medical = medarray[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        val smokerarray = resources.getStringArray(R.array.smoker_status)
        val adapter4 = ArrayAdapter(this, android.R.layout.simple_spinner_item, smokerarray)
        adapter4.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnersmoker.adapter = adapter4
        spinnersmoker.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                smoker_status = smokerarray[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }


        RetrofitClient.setContext(applicationContext)


        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            Addmedicalinfo()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun Addmedicalinfo() {
        val name = namet.text.toString()
        val user = getToken()
        val email = user?.email

        var heartrate = 75f

        if(restingheartrate.text.toString()!="Resting Heart Rate"){
            heartrate= restingheartrate.text.toString().toFloat()
        }

        val medicalinfoRequest = medical_info(email, name, gender, age, medical, smoker_status, triggers, deviceid.text.toString(), heartrate.toString())

        Log.d("sent medical info", medicalinfoRequest.toString())

        val apiService = RetrofitClient.createService(ApiService::class.java)

        val call = apiService.medicalinfo(medicalinfoRequest)

        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                Log.d("email", email.toString())

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true){
                        Log.d("user received", apiResponse.user.toString())
                        saveToken(apiResponse.user)
                        navigateToAccountFragment()
                    }
                    else{
                        if (apiResponse?.message == "User not found"){
                            showToast("User not found")
                        }
                        else{
                            showToast("Server Error")
                        }
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


    fun getToken(): user? {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val userJson = sharedPreferences.getString("user", null)
        return Gson().fromJson(userJson, user::class.java)
    }

    fun saveToken(user: user?) {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val userJson = Gson().toJson(user)
        Log.d("user saved", userJson)
        editor.putString("user", userJson)
        editor.apply()
    }


    private fun navigateToAccountFragment() {
        val intent = Intent(this, Account::class.java)
        intent.putExtra("fragmentToLoad", "AccountFragment")
        startActivity(intent)
    }

}



