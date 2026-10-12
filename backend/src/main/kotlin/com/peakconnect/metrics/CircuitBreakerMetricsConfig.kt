package com.peakconnect.metrics

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration

@Configuration
class CircuitBreakerMetricsConfig(
    private val circuitBreakerRegistry: CircuitBreakerRegistry
) {
    private val logger = LoggerFactory.getLogger(CircuitBreakerMetricsConfig::class.java)

    @PostConstruct
    fun initCircuitBreakers() {
        // Ensure all configured circuit breakers exist at startup and hook state transition logging
        listOf("weatherApi", "osrmApi", "paymentApi").forEach { name ->
            val cb = circuitBreakerRegistry.circuitBreaker(name)
            cb.eventPublisher.onStateTransition { event ->
                logger.info(
                    "CircuitBreaker '{}' state transition: {} -> {}",
                    name,
                    event.stateTransition.fromState,
                    event.stateTransition.toState
                )
            }
        }
    }
}
