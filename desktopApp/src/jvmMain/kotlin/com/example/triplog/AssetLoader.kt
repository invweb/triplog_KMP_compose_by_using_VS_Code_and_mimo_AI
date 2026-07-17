package com.example.triplog

actual fun loadAsset(filename: String): String {
    return try {
        val stream = object {}.javaClass.getResourceAsStream("/$filename")
        stream?.bufferedReader()?.readText() ?: ""
    } catch (_: Exception) {
        ""
    }
}
