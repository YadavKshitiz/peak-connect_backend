package com.peakconnect.job

import com.peakconnect.entity.BookingStatus
import com.peakconnect.entity.WaitlistStatus
import com.peakconnect.repository.BookingRepository
import com.peakconnect.repository.SlotRepository
import com.peakconnect.repository.WaitlistRepository
import com.peakconnect.service.WaitlistService
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import org.slf4j.LoggerFactory

@Component
class WaitlistExpiryJob(
    private val waitlistRepository: WaitlistRepository,
    private val bookingRepository: BookingRepository,
    private val slotRepository: SlotRepository,
    private val waitlistService: WaitlistService,
    private val cacheManager: CacheManager,
    @Value("\${waitlist.confirmation-window-minutes:60}")
    private val confirmationWindowMinutes: Long
) {
    private val logger = LoggerFactory.getLogger(WaitlistExpiryJob::class.java)

    @Scheduled(fixedRateString = "\${scheduler.waitlist-expiry-interval-ms:60000}")
    @Transactional
    fun expirePromotedWaitlists() {
        val cutoffTime = LocalDateTime.now().minusMinutes(confirmationWindowMinutes)
        val expiredEntries = waitlistRepository.findByStatusAndPromotedAtBefore(WaitlistStatus.PROMOTED, cutoffTime)
        
        if (expiredEntries.isEmpty()) return

        var expiredCount = 0
        val guideAvailabilityCache = cacheManager.getCache("guideAvailabilityCache")

        for (waitlist in expiredEntries) {
            waitlist.status = WaitlistStatus.EXPIRED
            waitlistRepository.save(waitlist)

            val booking = waitlist.booking
            if (booking != null && booking.status == BookingStatus.AWAITING_PAYMENT) {
                booking.status = BookingStatus.CANCELLED
                bookingRepository.save(booking)

                val slot = booking.slot
                if (slot.currentOccupancy > 0) {
                    slot.currentOccupancy--
                    slotRepository.save(slot)
                }

                guideAvailabilityCache?.evict(slot.id.toString())
                
                // Re-trigger promotion for the next person in line
                waitlistService.promoteNext(slot.id!!)
            }
            
            expiredCount++
        }

        logger.info("WaitlistExpiryJob completed: expired \$expiredCount waitlist entries.")
    }
}
