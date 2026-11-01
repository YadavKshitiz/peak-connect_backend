package com.peakconnect.app.feature.trekker.waitlist

import com.peakconnect.app.data.local.dao.UserContentDao
import com.peakconnect.app.data.local.entity.BookingEntity
import com.peakconnect.app.data.local.entity.WaitlistEntryEntity
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.WaitlistPromoted
import com.peakconnect.app.domain.model.WaitlistStatus
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaitlistPromotionDetector @Inject constructor(
    private val userContentDao: UserContentDao,
    private val waitlistEventBus: WaitlistEventBus
) {
    private val knownBookingIds = mutableSetOf<String>()

    fun initializeKnownBookings(bookingIds: Collection<String>) {
        knownBookingIds.addAll(bookingIds)
    }

    suspend fun detectPromotions(
        bookings: List<BookingEntity>,
        now: LocalDateTime = LocalDateTime.now()
    ): List<WaitlistEntryEntity> {
        val entries = userContentDao.getAllWaitlistEntries()
        val promotedList = mutableListOf<WaitlistEntryEntity>()

        // 1. Check linked bookings for terminal status and close matching entries
        entries.forEach { entry ->
            val linkedBookingId = entry.bookingId
            if (linkedBookingId != null && entry.status != WaitlistStatus.CLOSED) {
                val linkedBooking = bookings.find { it.id == linkedBookingId }
                if (linkedBooking != null) {
                    val closureReason = when (linkedBooking.status) {
                        BookingStatus.CONFIRMED -> "Booked"
                        BookingStatus.CANCELLED -> "Cancelled"
                        BookingStatus.NO_SHOW -> "No-show"
                        BookingStatus.REMATCH_FAILED -> "No guide available"
                        else -> null
                    }
                    if (closureReason != null) {
                        val closedEntry = entry.copy(
                            status = WaitlistStatus.CLOSED,
                            closureReason = closureReason
                        )
                        userContentDao.insertWaitlistEntry(closedEntry)
                    }
                }
            }
        }

        // 2. Check for promotion of WAITING entries
        val refreshedEntries = userContentDao.getAllWaitlistEntries()
        val waitingEntries = refreshedEntries.filter { it.status == WaitlistStatus.WAITING }

        bookings.forEach { booking ->
            // If booking was never seen before and is not CANCELLED
            if (!knownBookingIds.contains(booking.id)) {
                if (booking.status != BookingStatus.CANCELLED) {
                    val matchingEntry = waitingEntries.find { it.slotId == booking.slotId }
                    if (matchingEntry != null) {
                        val promoted = matchingEntry.copy(
                            status = WaitlistStatus.PROMOTED,
                            bookingId = booking.id,
                            guideName = booking.guideName ?: matchingEntry.guideName,
                            closureReason = null
                        )
                        userContentDao.insertWaitlistEntry(promoted)
                        promotedList.add(promoted)
                        waitlistEventBus.emit(
                            WaitlistPromoted(
                                bookingId = booking.id,
                                slotId = matchingEntry.slotId
                            )
                        )
                    }
                }
            }
        }

        // 3. Close WAITING entries whose slot date has passed
        val latestEntries = userContentDao.getAllWaitlistEntries()
        latestEntries.filter { it.status == WaitlistStatus.WAITING }.forEach { entry ->
            if (entry.slotDate.isBefore(now)) {
                val expiredEntry = entry.copy(
                    status = WaitlistStatus.CLOSED,
                    closureReason = "Slot date passed"
                )
                userContentDao.insertWaitlistEntry(expiredEntry)
            }
        }

        // 4. Record all seen booking IDs
        bookings.forEach { knownBookingIds.add(it.id) }

        return promotedList
    }
}
