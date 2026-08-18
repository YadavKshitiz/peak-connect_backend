package com.peakconnect.job

import com.peakconnect.entity.BookingStatus
import com.peakconnect.repository.BookingRepository
import com.peakconnect.repository.SlotRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import org.slf4j.LoggerFactory

@Component
class AutoCancelUnpaidBookingsJob(
    private val bookingRepository: BookingRepository,
    private val slotRepository: SlotRepository,
    private val cacheManager: CacheManager,
    @Value("\${booking.payment-timeout-minutes:15}")
    private val paymentTimeoutMinutes: Long
) {
    private val logger = LoggerFactory.getLogger(AutoCancelUnpaidBookingsJob::class.java)

    @Scheduled(fixedRateString = "\${scheduler.auto-cancel-interval-ms:300000}")
    @Transactional
    fun cancelUnpaidBookings() {
        val cutoffTime = LocalDateTime.now().minusMinutes(paymentTimeoutMinutes)
        val staleBookings = bookingRepository.findByStatusAndCreatedAtBefore(BookingStatus.AWAITING_PAYMENT, cutoffTime)
        
        if (staleBookings.isEmpty()) return

        var cancelledCount = 0
        val guideAvailabilityCache = cacheManager.getCache("guideAvailabilityCache")

        for (booking in staleBookings) {
            // Cancel booking
            booking.status = BookingStatus.CANCELLED
            
            // Free slot capacity
            val slot = booking.slot
            if (slot.currentOccupancy > 0) {
                slot.currentOccupancy--
                slotRepository.save(slot)
            }
            
            bookingRepository.save(booking)
            
            // Evict guide availability cache for this slot
            guideAvailabilityCache?.evict(slot.id.toString())
            cancelledCount++
        }

        logger.info("AutoCancelUnpaidBookingsJob completed: auto-cancelled \$cancelledCount stale bookings.")
    }
}
