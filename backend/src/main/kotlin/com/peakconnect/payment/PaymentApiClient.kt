package com.peakconnect.payment

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.springframework.stereotype.Component

@Component
class PaymentApiClient {

    @CircuitBreaker(name = "paymentApi", fallbackMethod = "createOrderFallback")
    @Retry(name = "paymentApi")
    fun createOrder(amount: Double): String {
        // TODO: implement real call in Task 5
        throw RuntimeException("Razorpay integration not implemented yet")
    }

    fun createOrderFallback(amount: Double, t: Throwable): String {
        println("Payment API fallback triggered: ${t.message}")
        // Return a clearly labeled unavailable result
        return "UNAVAILABLE_DEGRADED"
    }
}
