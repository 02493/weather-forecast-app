package com.example.unit4

import android.content.Context
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

object WeatherText {
    private fun number(value: Double) = String.format(Locale.getDefault(), "%.1f", value)
    fun temperature(value: Double) = "${number(value)} °C"
    fun header(context: Context, weather: WeatherSnapshot) =
        context.getString(R.string.weather_header, temperature(weather.temperature))
    fun comparison(context: Context, weather: WeatherSnapshot): String {
        val delta = Math.round(weather.nextHourTemperature * 10) - Math.round(weather.temperature * 10)
        val value = temperature(weather.nextHourTemperature)
        return when {
            delta > 0 -> context.getString(R.string.weather_warmer, value, number(abs(delta / 10.0)))
            delta < 0 -> context.getString(R.string.weather_colder, value, number(abs(delta / 10.0)))
            else -> context.getString(R.string.weather_same, value)
        }
    }
    fun forecast(context: Context, weather: WeatherSnapshot) = context.getString(
        R.string.weather_forecast, temperature(weather.nextHourTemperature), temperature(weather.nextTwoHoursTemperature))
    fun updated(context: Context, weather: WeatherSnapshot) = context.getString(
        R.string.weather_updated, weather.time.format(DateTimeFormatter.ofPattern("dd.MM HH:mm")))
}
