package com.peakconnect.risk

import org.springframework.stereotype.Component

@Component
class RiskCalculator {
    /**
     * Maps weather data to LOW, MODERATE, or HIGH risk.
     * Thresholds:
     * - HIGH: Wind speed > 15 m/s OR Temp < -10C OR Temp > 35C OR weather condition contains "Thunderstorm"
     * - MODERATE: Wind speed > 10 m/s OR condition contains "Rain" or "Snow"
     * - LOW: Otherwise
     */
    fun calculateRisk(weather: WeatherData?): String {
        if (weather == null) return "UNKNOWN"

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
