package com.peakconnect.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.TimeUnit

@Service
class AppMetricsService(
    private val meterRegistry: MeterRegistry
) {

    /**
     * Records a booking event with status (success/failure) and reason.
     * Expected reasons:
     * - confirmed (success)
     * - payment-failed (failure)
     * - auto-cancelled (failure)
     * - guide-declined-no-rematch (failure)
     */
    fun recordBookingEvent(status: String, reason: String) {
        meterRegistry.counter(
            "booking.events",
            "status", status,
            "reason", reason
        ).increment()
    }

    /**
     * Times execution of a booking endpoint and records percentiles (p50, p95).
     */
    fun <T> timeBookingEndpoint(endpoint: String, action: () -> T): T {
        val timer = Timer.builder("booking.endpoint.latency")
            .description("Latency of booking endpoints")
            .tag("endpoint", endpoint)
            .publishPercentiles(0.5, 0.95)
            .publishPercentileHistogram()
            .minimumExpectedValue(Duration.ofMillis(1))
            .maximumExpectedValue(Duration.ofSeconds(10))
            .register(meterRegistry)

        val start = System.nanoTime()
        return try {
            action()
        } finally {
            timer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS)
        }
    }

    /**
     * Times execution of an external API call (weather, osrm, payment) and tracks success/error status.
     */
    fun <T> timeExternalApi(api: String, action: () -> T): T {
        val start = System.nanoTime()
        var status = "success"
        return try {
            action()
        } catch (e: Throwable) {
            status = "error"
            throw e
        } finally {
            val duration = System.nanoTime() - start
            val timer = Timer.builder("external.api.latency")
                .description("Latency and status of external API calls")
                .tag("api", api)
                .tag("status", status)
                .publishPercentiles(0.5, 0.95)
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofSeconds(10))
                .register(meterRegistry)
            timer.record(duration, TimeUnit.NANOSECONDS)
        }
    }

    /**
     * Manually records external API call result when not using lambda wrapper.
     */
    fun recordExternalApiCall(api: String, status: String, durationNanos: Long) {
        val timer = Timer.builder("external.api.latency")
            .description("Latency and status of external API calls")
            .tag("api", api)
            .tag("status", status)
            .publishPercentiles(0.5, 0.95)
            .publishPercentileHistogram()
            .minimumExpectedValue(Duration.ofMillis(1))
            .maximumExpectedValue(Duration.ofSeconds(10))
            .register(meterRegistry)
        timer.record(durationNanos, TimeUnit.NANOSECONDS)
    }
}
