package com.example.breathein.network

import com.example.breathein.user


data class ApiResponse(val success: Boolean, val message: String?, val user: user?)
