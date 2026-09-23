package com.peakconnect.job

import com.peakconnect.entity.BookingStatus
import com.peakconnect.repository.BookingRepository
import com.peakconnect.service.BookingService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class GuideResponseTimeoutJob(
    private val bookingRepository: BookingRepository,
    private val bookingService: BookingService,
    @Value("\${guide.response-timeout-minutes:20}") private val timeoutMinutes: Long
) {
    private val logger = LoggerFactory.getLogger(GuideResponseTimeoutJob::class.java)

    @Scheduled(fixedRate = 60000) // Runs every minute
    @Transactional
    fun autoDeclineUnresponsiveGuides() {
        val cutoffTime = LocalDateTime.now().minusMinutes(timeoutMinutes)
        val timedOutBookings = bookingRepository.findByStatusAndLastGuideAssignedAtBefore(
            BookingStatus.PENDING_GUIDE_RESPONSE,
            cutoffTime
        )

        var rematchCount = 0
        for (booking in timedOutBookings) {
            try {
                if (booking.guide != null) {
                    booking.declinedGuideIds.add(booking.guide!!.id!!)
                }
                bookingService.rematchGuide(booking)
                bookingRepository.save(booking)
                rematchCount++
            } catch (e: Exception) {
                logger.error("Failed to process timeout rematch for booking ${booking.id}: ${e.message}")
            }
        }

        if (rematchCount > 0) {
            logger.info("GuideResponseTimeoutJob processed timeout and rematched $rematchCount bookings.")
        }
    }
}
