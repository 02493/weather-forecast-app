package com.example.unit4

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object WeatherCache {
    private val json = Json { ignoreUnknownKeys = true }
    fun save(context: Context, response: WeatherResponse) {
        context.applicationContext.getSharedPreferences("weather", Context.MODE_PRIVATE)
            .edit().putString("response", json.encodeToString(response)).apply()
    }
    fun load(context: Context): WeatherResponse? {
        val value = context.applicationContext.getSharedPreferences("weather", Context.MODE_PRIVATE)
            .getString("response", null) ?: return null
        return try { json.decodeFromString<WeatherResponse>(value) } catch (_: Exception) { null }
    }
}
