package com.peakconnect.payment

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class DepositCalculator(
    @Value("\${payment.deposit-percentage:0.2}")
    private val depositPercentage: Double
) {

    /**
     * Computes the required non-refundable deposit for a booking.
     * Note: This deposit is non-refundable regardless of the later cancellation tier.
     */
    fun calculateDeposit(totalPrice: Double): Double {
        return totalPrice * depositPercentage
    }
}
