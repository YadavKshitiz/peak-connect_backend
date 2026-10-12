package com.peakconnect.risk

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.RestClientException

@Component
class WeatherClient(
    @Value("\${openweathermap.api-key}") private val apiKey: String,
    private val appMetricsService: com.peakconnect.metrics.AppMetricsService
) {
    private val restTemplate = RestTemplate()
    // Using a configurable base URL so it can be overridden for testing
    @Value("\${openweathermap.url:https://api.openweathermap.org/data/2.5/weather}")
    private lateinit var baseUrl: String

    fun getWeather(location: String): WeatherData? {
        val start = System.nanoTime()
        return try {
            val url = "$baseUrl?q=$location&appid=$apiKey&units=metric"
            val result = restTemplate.getForObject(url, WeatherData::class.java)
            appMetricsService.recordExternalApiCall("weather", "success", System.nanoTime() - start)
            result
        } catch (e: RestClientException) {
            println("Weather API call failed: ${e.message}")
            appMetricsService.recordExternalApiCall("weather", "error", System.nanoTime() - start)
            null // Handle gracefully for V1
        }
    }
}

// Minimal DTOs for OpenWeatherMap response
data class WeatherData(
    val weather: List<WeatherDescription>? = null,
    val main: MainData? = null,
    val wind: WindData? = null
)

data class WeatherDescription(
    val main: String?,
    val description: String?
)

data class MainData(
    val temp: Double?,
    val humidity: Int?
)

data class WindData(
    val speed: Double?
)
