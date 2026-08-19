package com.peakconnect.controller

import com.peakconnect.service.BookingService
import com.razorpay.Utils
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/payments")
class PaymentWebhookController(
    private val bookingService: BookingService,
    @Value("\${razorpay.key-secret}") private val webhookSecret: String
) {

    @PostMapping("/webhook")
    fun handleWebhook(
        @RequestBody payload: String,
        @RequestHeader("X-Razorpay-Signature") signature: String
    ): ResponseEntity<String> {
        // Verify signature
        val isSignatureValid = try {
            Utils.verifyWebhookSignature(payload, signature, webhookSecret)
        } catch (e: Exception) {
            false
        }

        if (!isSignatureValid) {
            return ResponseEntity.badRequest().body("Invalid signature")
        }

        // Parse JSON payload
        val jsonPayload = JSONObject(payload)
        val event = jsonPayload.optString("event")

        // Handle relevant events
        if (event == "order.paid" || event == "payment.captured") {
            val paymentEntity = jsonPayload.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity")
            val orderId = paymentEntity.getString("order_id")
            
            bookingService.markBookingConfirmed(orderId)
        } else if (event == "payment.failed") {
            val paymentEntity = jsonPayload.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity")
            val orderId = paymentEntity.getString("order_id")
            
            bookingService.markBookingCancelled(orderId)
        }

        return ResponseEntity.ok("OK")
    }
}
