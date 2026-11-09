package com.example.unit4

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@Serializable
data class WeatherResponse(val hourly: HourlyWeather, val timezone: String = "Europe/Moscow")

@Serializable
data class HourlyWeather(
    val time: List<String>,
    @SerialName("temperature_2m") val temperature2m: List<Double?>
)

data class WeatherSnapshot(
    val temperature: Double,
    val time: LocalDateTime,
    val nextHourTemperature: Double,
    val nextTwoHoursTemperature: Double
)

fun WeatherResponse.toSnapshot(
    now: ZonedDateTime = ZonedDateTime.now(ZoneId.of("Europe/Moscow"))
): WeatherSnapshot? {
    if (timezone != "Europe/Moscow" || hourly.time.size != hourly.temperature2m.size) return null
    return try {
        val hour = now.withZoneSameInstant(ZoneId.of(timezone)).toLocalDateTime().truncatedTo(ChronoUnit.HOURS)
        val times = hourly.time.map(LocalDateTime::parse)
        if (times.zipWithNext().any { (a, b) -> !b.isAfter(a) }) return null
        val index = times.indexOf(hour)
        if (index < 0 || times.getOrNull(index + 1) != hour.plusHours(1) ||
            times.getOrNull(index + 2) != hour.plusHours(2)) return null
        val values = (index..index + 2).map { hourly.temperature2m[it] ?: return null }
        if (values.any { !it.isFinite() }) return null
        WeatherSnapshot(values[0], hour, values[1], values[2])
    } catch (_: java.time.DateTimeException) { null }
}
