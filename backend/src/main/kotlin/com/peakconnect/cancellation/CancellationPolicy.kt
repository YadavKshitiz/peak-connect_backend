package com.peakconnect.cancellation

import com.peakconnect.entity.Booking
import java.time.LocalDateTime

interface CancellationPolicy {
    fun calculateRefundPercentage(booking: Booking, cancellationTime: LocalDateTime): Double
}
