package com.peakconnect.routing

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class OsrmRoutingClient(
    @Value("\${osrm.base-url:https://router.project-osrm.org}") private val baseUrl: String,
    private val restTemplate: RestTemplate = RestTemplate()
) {

    @CircuitBreaker(name = "osrmApi", fallbackMethod = "getDistanceFallback")
    @Retry(name = "osrmApi")
    fun getDistance(originLat: Double, originLng: Double, destLat: Double, destLng: Double): Double {
        // OSRM requires {originLng},{originLat};{destLng},{destLat}
        val url = "$baseUrl/route/v1/driving/$originLng,$originLat;$destLng,$destLat?overview=false"
        
        val response = restTemplate.getForObject(url, Map::class.java)
        
        if (response != null && response["code"] == "Ok") {
            val routes = response["routes"] as? List<*>
            if (routes != null && routes.isNotEmpty()) {
                val route = routes[0] as? Map<*, *>
                val distance = route?.get("distance") as? Number
                if (distance != null) {
                    return distance.toDouble()
                }
            }
        }
        
        throw RuntimeException("Failed to get valid route from OSRM")
    }

    fun getDistanceFallback(originLat: Double, originLng: Double, destLat: Double, destLng: Double, t: Throwable): Double {
        println("OSRM API fallback triggered: ${t.message}")
        // Return a clearly labeled unavailable/default result
        // Since we need a Double, returning -1.0 to indicate "unavailable"
        return -1.0
    }
}
