package com.peakconnect.routing

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.springframework.stereotype.Component

@Component
class OsrmRoutingClient {

    @CircuitBreaker(name = "osrmApi", fallbackMethod = "getDistanceFallback")
    @Retry(name = "osrmApi")
    fun getDistance(originLat: Double, originLng: Double, destLat: Double, destLng: Double): Double {
        // TODO: implement real call in Task 9
        throw RuntimeException("OSRM integration not implemented yet")
    }

    fun getDistanceFallback(originLat: Double, originLng: Double, destLat: Double, destLng: Double, t: Throwable): Double {
        println("OSRM API fallback triggered: ${t.message}")
        // Return a clearly labeled unavailable/default result
        // Since we need a Double, returning -1.0 to indicate "unavailable"
        return -1.0
    }
}
