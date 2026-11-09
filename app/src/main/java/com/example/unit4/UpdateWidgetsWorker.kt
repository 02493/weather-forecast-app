package com.example.unit4

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters

class UpdateWidgetsWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    private val weatherService = Dependencies.weatherService
    override suspend fun doWork(): Result {
        val response = weatherService.getCurrentWeather() ?: return Result.retry()
        val snapshot = response.toSnapshot() ?: return Result.retry()
        WeatherCache.save(applicationContext, response)
        WeatherAppWidgetProvider.renderAll(applicationContext, snapshot)
        return Result.success()
    }
    companion object {
        fun enqueue(context: Context) {
            val work = OneTimeWorkRequestBuilder<UpdateWidgetsWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            WorkManager.getInstance(context).enqueueUniqueWork("weather_widgets", ExistingWorkPolicy.KEEP, work)
        }
    }
}
