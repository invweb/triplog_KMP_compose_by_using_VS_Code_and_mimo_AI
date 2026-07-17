package com.example.triplog

import android.content.Context

actual fun loadAsset(filename: String): String {
    return try {
        val context = TripLogApp.context
        context.assets.open(filename).bufferedReader().readText()
    } catch (_: Exception) {
        ""
    }
}

object TripLogApp {
    lateinit var context: Context
}
