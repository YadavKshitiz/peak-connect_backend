package com.peakconnect.service

import com.peakconnect.config.TwoLevelCache
import com.peakconnect.dto.*
import com.peakconnect.entity.BookingStatus
import com.peakconnect.entity.WaitlistStatus
import com.peakconnect.repository.BookingRepository
import com.peakconnect.repository.WaitlistRepository
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class AdminStatsService(
    private val bookingRepository: BookingRepository,
    private val waitlistRepository: WaitlistRepository,
    private val cacheManager: CacheManager
) {

    @Transactional(readOnly = true)
    fun getBookingsToday(): BookingsTodayStats {
        val todayStart = LocalDate.now().atStartOfDay()
        val tomorrowStart = todayStart.plusDays(1)
        
        val bookingsToday = bookingRepository.findAll().filter { 
            it.createdAt.isAfter(todayStart) && it.createdAt.isBefore(tomorrowStart)
        }
        
        val created = bookingsToday.size
        val confirmed = bookingsToday.count { it.status == BookingStatus.CONFIRMED }
        
        return BookingsTodayStats(
            created = created,
            confirmed = confirmed
        )
    }

    fun getCacheHitRate(): CacheStats {
        var totalHits = 0L
        var totalMisses = 0L
        
        cacheManager.cacheNames.forEach { name ->
            val cache = cacheManager.getCache(name)
            if (cache is TwoLevelCache) {
                totalHits += cache.hits.get()
                totalMisses += cache.misses.get()
            }
        }
        
        val totalRequests = totalHits + totalMisses
        val hitRate = if (totalRequests > 0) {
            totalHits.toDouble() / totalRequests.toDouble()
        } else {
            0.0
        }
        
        return CacheStats(
            hits = totalHits,
            misses = totalMisses,
            hitRate = hitRate
        )
    }

    @Transactional(readOnly = true)
    fun getActiveWaitlists(): WaitlistStats {
        val activeEntries = waitlistRepository.findAll().filter { 
            it.status == WaitlistStatus.WAITING || it.status == WaitlistStatus.PROMOTED 
        }
        
        val totalWaiting = activeEntries.count { it.status == WaitlistStatus.WAITING }
        val totalPromoted = activeEntries.count { it.status == WaitlistStatus.PROMOTED }
        
        val breakdown = activeEntries.groupBy { it.slot.id!! }.mapValues { (_, entries) ->
            SlotWaitlistStats(
                waiting = entries.count { it.status == WaitlistStatus.WAITING },
                promoted = entries.count { it.status == WaitlistStatus.PROMOTED }
            )
        }
        
        return WaitlistStats(
            totalWaiting = totalWaiting,
            totalPromoted = totalPromoted,
            breakdownBySlot = breakdown
        )
    }

    @Transactional(readOnly = true)
    fun getGuideResponseStats(): GuideResponseStats {
        val allBookings = bookingRepository.findAll()
        
        var totalAssignments = 0
        var totalAccepted = 0
        var totalDeclined = 0 // Manual + timeouts
        var totalTimedOut = 0
        
        allBookings.forEach { booking ->
            // Current assignment count (if they accepted it or still pending)
            val isAccepted = booking.guideAcceptedAt != null ||
                booking.status == BookingStatus.AWAITING_PAYMENT ||
                booking.status == BookingStatus.CONFIRMED ||
                booking.status == BookingStatus.NO_SHOW ||
                (booking.status == BookingStatus.CANCELLED && (booking.depositAmount != null || booking.paymentOrderId != null))

            if (isAccepted) {
                // The guide accepted the booking
                totalAssignments++
                totalAccepted++
            } else if (booking.status == BookingStatus.PENDING_GUIDE_RESPONSE) {
                // Currently pending guide response
                totalAssignments++
            }
            
            // Add historical declines and timeouts
            val timedOutIds = booking.timedOutGuideIds.size
            
            totalAssignments += booking.declinedGuideIds.size
            totalDeclined += booking.declinedGuideIds.size
            totalTimedOut += timedOutIds
        }
        
        val acceptRate = if (totalAssignments > 0) totalAccepted.toDouble() / totalAssignments.toDouble() else 0.0
        val declineRate = if (totalAssignments > 0) totalDeclined.toDouble() / totalAssignments.toDouble() else 0.0
        
        return GuideResponseStats(
            totalAssignments = totalAssignments,
            accepted = totalAccepted,
            declined = totalDeclined,
            timedOut = totalTimedOut,
            acceptRate = acceptRate,
            declineRate = declineRate
        )
    }
}
