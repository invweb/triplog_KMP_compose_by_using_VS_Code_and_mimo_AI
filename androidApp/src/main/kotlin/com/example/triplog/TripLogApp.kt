package com.example.triplog

import android.app.Application
import com.yandex.mapkit.MapKitFactory

class TripLogApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY)
            MapKitFactory.initialize(this)
        } catch (_: Throwable) {
        }
    }
}
