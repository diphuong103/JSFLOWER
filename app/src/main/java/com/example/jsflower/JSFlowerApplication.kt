package com.example.jsflower

import android.app.Application
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger

class JSFlowerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Khởi tạo Facebook SDK
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(this)
    }
}