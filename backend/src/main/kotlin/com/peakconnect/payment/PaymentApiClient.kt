package com.peakconnect.payment

import com.razorpay.Order
import com.razorpay.RazorpayClient
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class PaymentApiClient(
    @Value("\${razorpay.key-id}") private val keyId: String,
    @Value("\${razorpay.key-secret}") private val keySecret: String
) {

    private val razorpayClient: RazorpayClient by lazy {
        RazorpayClient(keyId, keySecret)
    }

    @CircuitBreaker(name = "paymentApi", fallbackMethod = "createOrderFallback")
    @Retry(name = "paymentApi")
    fun createOrder(amount: Double): String {
        val amountInPaise = (amount * 100).toLong()
        val orderRequest = JSONObject().apply {
            put("amount", amountInPaise)
            put("currency", "INR")
            put("receipt", "txn_${UUID.randomUUID().toString().substring(0, 8)}")
        }

        val order: Order = razorpayClient.orders.create(orderRequest)
        return order.get("id")
    }

    fun createOrderFallback(amount: Double, t: Throwable): String {
        println("Payment API fallback triggered: ${t.message}")
        // Return a clearly labeled unavailable result
        return "UNAVAILABLE_DEGRADED"
    }
}
