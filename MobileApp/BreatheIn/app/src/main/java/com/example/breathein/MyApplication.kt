package com.example.breathein

import android.app.Application
import android.content.Context
import com.example.breathein.network.AppContextProvider

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContextProvider.initialize(this)
    }
}

object AppContextProvider {
    private var initialized = false
    private lateinit var context: Context

    fun initialize(context: Context) {
        if (!initialized) {
            this.context = context
            initialized = true
        }
    }

    fun getAppContext(): Context {
        return context
    }
}

