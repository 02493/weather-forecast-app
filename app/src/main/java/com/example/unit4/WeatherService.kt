
package com.example.unit4

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

class WeatherService(private val client: HttpClient) {

    suspend fun getCurrentWeather(): WeatherResponse? {
        return try {
            val response = client.get("https://api.open-meteo.com/v1/dwd-icon") {
                parameter("latitude", "55.7522")
                parameter("longitude", "37.6156")
                parameter("hourly", "temperature_2m")
                parameter("timezone", "Europe/Moscow")
                parameter("forecast_days", 2)
            }

            if (!response.status.isSuccess()) {
                Log.e(TAG, "Ошибка HTTP: ${response.status}")
                return null
            }

            val weather = response.body<WeatherResponse>()

            if (weather.toSnapshot() == null) {
                Log.e(TAG, "Не удалось обработать данные погоды")
                null
            } else {
                weather
            }

        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке погоды", e)
            null
        }
    }

    companion object {
        private const val TAG = "WeatherService"
    }
}
