package com.example.unit4

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object WeatherNotifications {
    private val channelId get() = MainActivity.NOTIFICATION_ID.toString()
    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(channelId,
            context.getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW).apply {
            description = context.getString(R.string.channel_description)
        })
    }
    fun show(context: Context, weather: WeatherSnapshot): Boolean {
        createChannel(context)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled() ||
            manager.getNotificationChannel(channelId)?.importance == NotificationManager.IMPORTANCE_NONE) return false
        val header = WeatherText.header(context, weather)
        val compact = RemoteViews(context.packageName, R.layout.notification_weather).apply {
            setTextViewText(R.id.headerText, header)
        }
        val expanded = RemoteViews(context.packageName, R.layout.notification_weather_expanded).apply {
            setTextViewText(R.id.headerText, header)
            setTextViewText(R.id.comparisonText, WeatherText.comparison(context, weather))
        }
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_weather).setContentTitle(header)
            .setContentText(WeatherText.comparison(context, weather))
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(compact).setCustomBigContentView(expanded)
            .setContentIntent(open).setOnlyAlertOnce(true).setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setWhen(weather.time.atZone(java.time.ZoneId.of("Europe/Moscow")).toInstant().toEpochMilli())
            .build()
        return try { manager.notify(MainActivity.NOTIFICATION_ID, notification); true }
        catch (_: SecurityException) { false }
    }
}
