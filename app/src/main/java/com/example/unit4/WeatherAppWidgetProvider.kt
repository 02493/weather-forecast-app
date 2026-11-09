package com.example.unit4

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews

class WeatherAppWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        renderAll(context, WeatherCache.load(context)?.toSnapshot())
        UpdateWidgetsWorker.enqueue(context)
    }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        renderAll(context, WeatherCache.load(context)?.toSnapshot())
        UpdateWidgetsWorker.enqueue(context)
    }
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE && intent.getBooleanExtra(EXTRA_RENDER_ONLY, false)) {
            renderAll(context, WeatherCache.load(context)?.toSnapshot())
        } else super.onReceive(context, intent)
    }
    companion object {
        const val EXTRA_RENDER_ONLY = "com.example.unit4.RENDER_ONLY"
        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WeatherAppWidgetProvider::class.java))
            context.sendBroadcast(Intent(context, WeatherAppWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            })
        }
        fun renderAll(context: Context, weather: WeatherSnapshot?) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WeatherAppWidgetProvider::class.java))
            for (id in ids) {
                val small = views(context, id, false, weather)
                val large = views(context, id, true, weather)
                val views = if (Build.VERSION.SDK_INT >= 31) {
                    RemoteViews(mapOf(SizeF(110f, 140f) to small, SizeF(250f, 180f) to large))
                } else {
                    val options = manager.getAppWidgetOptions(id)
                    if (options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) >= 250 &&
                        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) >= 180) large else small
                }
                manager.updateAppWidget(id, views)
            }
        }
        private fun views(context: Context, id: Int, large: Boolean, weather: WeatherSnapshot?): RemoteViews {
            return RemoteViews(context.packageName, if (large) R.layout.weather_app_widget_large else R.layout.weather_app_widget_small).apply {
                setTextViewText(R.id.cityText, context.getString(R.string.city_moscow))
                setTextViewText(R.id.weatherText, weather?.let { WeatherText.temperature(it.temperature) }
                    ?: context.getString(R.string.temperature_placeholder))
                setTextViewText(R.id.updatedText, weather?.let { if (large) WeatherText.updated(context, it) else context.getString(R.string.widget_short_time, it.time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))) }
                    ?: context.getString(R.string.weather_unavailable))
                if (large) setTextViewText(R.id.forecastText, weather?.let { WeatherText.forecast(context, it) }
                    ?: context.getString(R.string.weather_unavailable))
                val update = Intent(context, WeatherAppWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(id))
                }
                setOnClickPendingIntent(R.id.updateWidgetButton, PendingIntent.getBroadcast(context, id, update,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            }
        }
    }
}
