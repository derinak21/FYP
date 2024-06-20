package com.example.breathein.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @POST("login")
    fun login(@Body loginRequest: user_password): Call<ApiResponse>

    @POST("signup")
    fun signup(@Body signupRequest: user_password): Call<ApiResponse>

    @POST("medicalinfo")
    fun medicalinfo(@Body medicalinforequest: medical_info): Call<ApiResponse>

    @POST("start_stop")
    fun start_stop(@Body start_stoprequest: controlsignal): Call<ApiResponse>


    @GET("test")
    fun testServerConnection(): Call<ApiResponse>
}


