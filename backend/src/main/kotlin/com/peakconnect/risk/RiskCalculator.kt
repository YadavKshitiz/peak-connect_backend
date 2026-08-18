package com.peakconnect.risk

import org.springframework.stereotype.Component
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.CacheManager
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import java.util.UUID

@Component
class RiskCalculator(
    private val weatherClient: WeatherClient,
    private val cacheManager: CacheManager
) {

    @Cacheable(value = ["riskCache"], key = "#slotId.toString() + '-' + #location")
    @CircuitBreaker(name = "weatherApi", fallbackMethod = "weatherApiFallback")
    @Retry(name = "weatherApi")
    fun calculateRiskForSlot(slotId: UUID, location: String): String {
        println("COMPUTING RISK FOR SLOT $slotId (This should not print on cache hit)")
        val weather = weatherClient.getWeather(location)
        return calculateRisk(weather)
    }

    fun weatherApiFallback(slotId: UUID, location: String, t: Throwable): String {
        println("Weather API fallback triggered for slot $slotId due to: ${t.message}")
        val cache = cacheManager.getCache("riskCache")
        val cachedValue = cache?.get("$slotId-$location", String::class.java)
        return cachedValue ?: "UNKNOWN (Degraded Mode)"
    }

    /**
     * Maps weather data to LOW, MODERATE, or HIGH risk.
     * Thresholds:
     * - HIGH: Wind speed > 15 m/s OR Temp < -10C OR Temp > 35C OR weather condition contains "Thunderstorm"
     * - MODERATE: Wind speed > 10 m/s OR condition contains "Rain" or "Snow"
     * - LOW: Otherwise
     */
    fun calculateRisk(weather: WeatherData?): String {
        if (weather == null) {
            throw RuntimeException("Weather data unavailable")
        }

        val windSpeed = weather.wind?.speed ?: 0.0
        val temp = weather.main?.temp
        val conditions = weather.weather?.map { it.main ?: "" } ?: emptyList()

        if (windSpeed > 15.0 || (temp != null && (temp < -10.0 || temp > 35.0)) || conditions.any { it.contains("Thunderstorm") }) {
            return "HIGH"
        }

        if (windSpeed > 10.0 || conditions.any { it.contains("Rain") || it.contains("Snow") }) {
            return "MODERATE"
        }

        return "LOW"
    }
}
