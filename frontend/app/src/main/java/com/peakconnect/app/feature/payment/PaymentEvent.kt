package com.peakconnect.app.feature.payment

sealed interface PaymentEvent {
    data class Success(val paymentId: String, val orderId: String?) : PaymentEvent
    data class Failure(val code: Int, val message: String?) : PaymentEvent
    data class ExternalWallet(val walletName: String?) : PaymentEvent
}
