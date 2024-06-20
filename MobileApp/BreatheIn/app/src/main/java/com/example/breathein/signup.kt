package com.example.breathein

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.content.Intent
import android.util.Log
import android.util.Patterns
import android.widget.TextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.breathein.network.ApiResponse
import com.example.breathein.network.ApiService
import com.example.breathein.network.RetrofitClient
import com.example.breathein.network.user_password
import com.google.gson.Gson
import retrofit2.Callback
import retrofit2.Call
import retrofit2.Response

class signup : AppCompatActivity() {

    private lateinit var emailText: EditText
    private lateinit var passwordText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        emailText=findViewById(R.id.editTextTextEmailAddress3)
        passwordText = findViewById(R.id.editTextTextPassword)

        RetrofitClient.setContext(applicationContext)

        val textViewCreateAccount = findViewById<TextView>(R.id.textViewCreateAccount)
        textViewCreateAccount.setOnClickListener {
            navigateToMainActivity()
        }
        val button = findViewById<Button>(R.id.button3)
        button.setOnClickListener {
            if (isValidEmail(emailText.text.toString()) && isValidPassword(passwordText.text.toString())) {
                Signup()
            } else {
                if(!isValidEmail(emailText.text.toString())) {
                    Toast.makeText(this, "Invalid email", Toast.LENGTH_SHORT).show()
                }
                else{
                    Toast.makeText(this, "Invalid password", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun Signup() {
        val email = emailText.text.toString()
        val password = passwordText.text.toString()
        Log.d("email", email)

        val signupRequest = user_password(email, password)
        val apiService = RetrofitClient.createService(ApiService::class.java)
        val call = apiService.signup(signupRequest)

        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                val apiResponse = response.body()

                if (response.isSuccessful) {
                    if (apiResponse?.success == true) {
                        val user = apiResponse.user
                        saveToken(user)
                        navigateToMedicalinfo()
                        Log.d("App", "Signup successful")
                    }
                    else{
                        if(apiResponse?.message == "Account already exists") {
                            showToast("Account already exists")
                        }
                        else {
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


    fun isValidPassword(password: String): Boolean {
        val minLength = 8
        if (password.length < minLength) {
            return false
        }

        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }

        if (!hasLetter || !hasDigit) {
            return false
        }

        return true
    }


    fun isValidEmail(email: String): Boolean {
        Log.d("email", email)

        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }


    fun saveToken(user: user?) {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val userJson = Gson().toJson(user)

        editor.putString("user", userJson)
        editor.apply()
    }


    private fun navigateToMainActivity(){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToMedicalinfo(){
        val intent = Intent(this, Medicalinfo::class.java)
        startActivity(intent)
    }
}