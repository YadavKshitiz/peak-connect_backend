package com.peakconnect.cancellation

import com.peakconnect.entity.Booking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Component
class FlexiblePolicy(
    @Value("\${cancellation.flexible.full-refund-hours:48}")
    private val fullRefundHours: Long,
    
    @Value("\${cancellation.flexible.partial-refund-hours:12}")
    private val partialRefundHours: Long
) : CancellationPolicy {

    override fun calculateRefundPercentage(booking: Booking, cancellationTime: LocalDateTime): Double {
        val hoursBeforeActivity = ChronoUnit.HOURS.between(cancellationTime, booking.slot.date)
        
        return when {
            hoursBeforeActivity >= fullRefundHours -> 1.0 // 100%
            hoursBeforeActivity >= partialRefundHours -> 0.5 // 50%
            else -> 0.0 // 0%
        }
    }
}
