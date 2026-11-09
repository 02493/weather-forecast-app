package com.example.unit4

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class UpdateWeatherWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    private val weatherService = Dependencies.weatherService
    override suspend fun doWork(): Result {
        val response = weatherService.getCurrentWeather() ?: return Result.retry()
        val snapshot = response.toSnapshot() ?: return Result.retry()
        WeatherCache.save(applicationContext, response)
        WeatherNotifications.show(applicationContext, snapshot)
        val ids = AppWidgetManager.getInstance(applicationContext).getAppWidgetIds(
            ComponentName(applicationContext, WeatherAppWidgetProvider::class.java))
        applicationContext.sendBroadcast(Intent(applicationContext, WeatherAppWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            putExtra(WeatherAppWidgetProvider.EXTRA_RENDER_ONLY, true)
        })
        return Result.success()
    }
}
