package com.fittrack.app

import android.app.Application
import com.fittrack.app.data.api.RetrofitClient

/**
 * Application class — runs once when the app starts.
 * Initializes RetrofitClient so the auth token interceptor
 * has access to SharedPreferences from the start.
 */
class FitTrackApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
    }
}
