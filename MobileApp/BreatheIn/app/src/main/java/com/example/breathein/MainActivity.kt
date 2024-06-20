package com.example.breathein

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import android.widget.Button
import android.widget.EditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.widget.Toast
import com.example.breathein.network.ApiResponse
import com.example.breathein.network.ApiService
import com.example.breathein.network.user_password
import com.example.breathein.network.RetrofitClient
import com.google.gson.Gson


class MainActivity : ComponentActivity() {

    private lateinit var emailText: EditText
    private lateinit var passwordText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        emailText = findViewById(R.id.editTextTextEmailAddress3)
        passwordText = findViewById(R.id.editTextTextPassword)

        RetrofitClient.setContext(applicationContext)

        val textViewCreateAccount = findViewById<TextView>(R.id.textViewCreateAccount)
        textViewCreateAccount.setOnClickListener {
            navigateToSignupActivity()
        }
        val button = findViewById<Button>(R.id.button3)
        button.setOnClickListener {
            Login()


        }

    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities =
                connectivityManager.getNetworkCapabilities(network)
            return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_CELLULAR
            ))
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun Login() {
        val email = emailText.text.toString()
        val password = passwordText.text.toString()
        Log.d("App", "Logging in...")

        val loginRequest = user_password(email, password)
        val apiService = RetrofitClient.createService(ApiService::class.java)
        val call = apiService.login(loginRequest)

        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true) {
                        val user = apiResponse.user
                        if(user !=null){
                            Log.d("user", user.toString())
                            saveToken(user)
                        }
                        navigateToAccountFragment()
                    } else {
                        if (apiResponse?.message == "Account does not exist"){
                            showToast("Account does not exist")
                        }
                        else if (apiResponse?.message == "Wrong password"){
                            showToast("Wrong password")
                        }
                        else{
                            showToast("Server error")
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

    fun saveToken(user: user?) {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val userJson = Gson().toJson(user)
        Log.d("user", userJson)
        editor.putString("user", userJson)
        editor.apply()
    }



    private fun navigateToAccountFragment() {
        val intent = Intent(this, Account::class.java)
        intent.putExtra("fragmentToLoad", "AccountFragment")
        startActivity(intent)
    }

    private fun navigateToSignupActivity() {
        val intent = Intent(this, signup::class.java)
        startActivity(intent)
    }

}




