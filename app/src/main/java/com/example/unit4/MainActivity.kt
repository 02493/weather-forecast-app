package com.example.unit4

import android.Manifest
import android.animation.ValueAnimator
import com.google.android.material.shape.MaterialShapeDrawable
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.unit4.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        showStatus(if (granted) R.string.permission_granted else R.string.permission_denied)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setAccessibilityHeading(binding.cityTitle, true)
      ViewCompat.setOnApplyWindowInsetsListener(binding.contentColumn) { view, insets ->
    val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
    val density = resources.displayMetrics.density

    view.setPadding(
        (24 * density).toInt() + bars.left,
        bars.top,
        (24 * density).toInt() + bars.right,
        (28 * density).toInt() + bars.bottom
    )
    insets
}
        WeatherNotifications.createChannel(this)
        val work = PeriodicWorkRequestBuilder<UpdateWeatherWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("weather_periodic", ExistingPeriodicWorkPolicy.KEEP, work)
        WeatherCache.load(this)?.toSnapshot()?.let(::render)
        binding.createNotificationButton.setOnClickListener { loadWeather(notify = true) }
        binding.refreshButton.setOnClickListener { loadWeather(notify = false) }
        binding.notificationSettingsButton.setOnClickListener {
            openNotificationSettings()
        }
        binding.updateWidgetButton.setOnClickListener {
            WeatherAppWidgetProvider.requestUpdate(this)
            showStatus(R.string.widget_update_requested)
            showFeedback(R.string.widget_update_requested)
        }
        if (Build.VERSION.SDK_INT >= 33 && savedInstanceState == null &&
            !getPreferences(MODE_PRIVATE).getBoolean("notification_permission_requested", false)) {
            getPreferences(MODE_PRIVATE).edit().putBoolean("notification_permission_requested", true).apply()
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (savedInstanceState == null && ValueAnimator.areAnimatorsEnabled()) {
            binding.contentColumn.apply {
                alpha = 0f
                translationY = 10f * resources.displayMetrics.density
                animate().alpha(1f).translationY(0f).setDuration(450).start()
            }
        }
        loadWeather(notify = false)
    }
    private fun loadWeather(notify: Boolean) = lifecycleScope.launch {
        setLoading(true)
        showStatus(R.string.weather_loading)
        try {
            val response = Dependencies.weatherService.getCurrentWeather()
                ?: run { showStatus(R.string.weather_error); showFeedback(R.string.weather_error); return@launch }
            val weather = response.toSnapshot()
            if (weather == null) { showStatus(R.string.weather_error); showFeedback(R.string.weather_error); return@launch }
            WeatherCache.save(this@MainActivity, response)
            render(weather)
            WeatherAppWidgetProvider.renderAll(this@MainActivity, weather)
            if (notify) {
                val shown = WeatherNotifications.show(this@MainActivity, weather)
                val message = if (shown) R.string.notification_updated else R.string.notification_disabled
                showStatus(message)
                showFeedback(message, settings = !shown)
            } else showStatus(R.string.fresh_weather)
        } finally { setLoading(false) }
    }
    private fun openNotificationSettings() {
        startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
    }
    private fun showStatus(message: Int) {
        binding.statusText.setText(message)
        val attention = when (message) {
            R.string.weather_error, R.string.weather_loading, R.string.permission_denied,
            R.string.notification_disabled -> true
            else -> false
        }
        binding.statusText.setCompoundDrawablesRelativeWithIntrinsicBounds(
            if (attention) R.drawable.status_dot_attention else R.drawable.status_dot, 0, 0, 0)
    }
    private fun showFeedback(message: Int, settings: Boolean = false) {
        val feedback = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.surface))
            .setTextColor(ContextCompat.getColor(this, R.color.paper))
            .setActionTextColor(ContextCompat.getColor(this, R.color.accent))
        (feedback.view.background as? MaterialShapeDrawable)?.let { shape ->
            shape.shapeAppearanceModel = shape.shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(18f * resources.displayMetrics.density).build()
        }
        feedback.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).maxLines = 4
        if (settings) feedback.setAction(R.string.open_settings) { openNotificationSettings() }
        feedback.show()
    }
    private fun setLoading(loading: Boolean) {
        binding.createNotificationButton.isEnabled = !loading
        binding.refreshButton.isEnabled = !loading
        binding.refreshButton.visibility = if (loading) View.INVISIBLE else View.VISIBLE
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }
    private fun degree(value: Double) = getString(R.string.weather_degree, String.format(Locale.getDefault(), "%.1f", value))
    private fun render(weather: WeatherSnapshot) {
        binding.temperatureText.text = degree(weather.temperature)
        binding.temperatureText.contentDescription = WeatherText.header(this, weather)
        binding.forecastText.text = WeatherText.comparison(this, weather)
        binding.updatedText.text = WeatherText.updated(this, weather)
        val time = DateTimeFormatter.ofPattern("HH:mm")
        binding.hourOneTime.text = weather.time.plusHours(1).format(time)
        binding.hourTwoTime.text = weather.time.plusHours(2).format(time)
        binding.hourOneTemperature.text = degree(weather.nextHourTemperature)
        binding.hourTwoTemperature.text = degree(weather.nextTwoHoursTemperature)
    }
    companion object { const val NOTIFICATION_ID = 4 }
}
