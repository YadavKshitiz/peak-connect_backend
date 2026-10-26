package com.peakconnect.app

import com.peakconnect.app.data.dto.BookingResponse
import com.peakconnect.app.data.local.dao.BookingDao
import com.peakconnect.app.data.local.dao.syncBookings
import com.peakconnect.app.data.local.entity.BookingEntity
import com.peakconnect.app.domain.model.BookingStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class BookingEntitySyncTest {

    private val bookingDao: BookingDao = mockk(relaxed = true)

    @Test
    fun `syncBookings preserves statusSinceMillis when status is unchanged`() = runTest {
        val initialStatusSince = 1000L
        val existingBooking = BookingEntity(
            id = "booking-1",
            slotId = "slot-1",
            activityTitle = "Everest Base Camp",
            trekkerId = "user-1",
            trekkerName = "John Doe",
            guideId = "guide-1",
            guideName = "Sherpa Tenzing",
            status = BookingStatus.CONFIRMED,
            date = LocalDateTime.of(2026, 6, 1, 10, 0),
            paymentOrderId = "order-1",
            depositAmount = 50.0,
            refundAmount = 0.0,
            refundPercentage = 0.0,
            statusSinceMillis = initialStatusSince,
            lastSyncMillis = 1000L
        )

        coEvery { bookingDao.getAllBookings() } returns listOf(existingBooking)

        val incomingDto = BookingResponse(
            id = "booking-1",
            slotId = "slot-1",
            activityTitle = "Everest Base Camp",
            trekkerId = "user-1",
            trekkerName = "John Doe",
            guideId = "guide-1",
            guideName = "Sherpa Tenzing",
            status = BookingStatus.CONFIRMED, // Unchanged status
            date = "2026-06-01T10:00:00",
            paymentOrderId = "order-1",
            depositAmount = 50.0,
            refundAmount = 0.0,
            refundPercentage = 0.0
        )

        val syncTime = 5000L
        bookingDao.syncBookings(listOf(incomingDto), now = syncTime)

        val captured = slot<List<BookingEntity>>()
        coVerify { bookingDao.insertBookings(capture(captured)) }

        val synced = captured.captured.first()
        assertEquals("statusSinceMillis must be preserved if status is unchanged", initialStatusSince, synced.statusSinceMillis)
        assertEquals(syncTime, synced.lastSyncMillis)
    }

    @Test
    fun `syncBookings updates statusSinceMillis when status changes`() = runTest {
        val initialStatusSince = 1000L
        val existingBooking = BookingEntity(
            id = "booking-1",
            slotId = "slot-1",
            activityTitle = "Everest Base Camp",
            trekkerId = "user-1",
            trekkerName = "John Doe",
            guideId = "guide-1",
            guideName = "Sherpa Tenzing",
            status = BookingStatus.PENDING,
            date = LocalDateTime.of(2026, 6, 1, 10, 0),
            paymentOrderId = "order-1",
            depositAmount = 50.0,
            refundAmount = 0.0,
            refundPercentage = 0.0,
            statusSinceMillis = initialStatusSince,
            lastSyncMillis = 1000L
        )

        coEvery { bookingDao.getAllBookings() } returns listOf(existingBooking)

        val incomingDto = BookingResponse(
            id = "booking-1",
            slotId = "slot-1",
            activityTitle = "Everest Base Camp",
            trekkerId = "user-1",
            trekkerName = "John Doe",
            guideId = "guide-1",
            guideName = "Sherpa Tenzing",
            status = BookingStatus.CONFIRMED, // Status changed from PENDING to CONFIRMED
            date = "2026-06-01T10:00:00",
            paymentOrderId = "order-1",
            depositAmount = 50.0,
            refundAmount = 0.0,
            refundPercentage = 0.0
        )

        val syncTime = 5000L
        bookingDao.syncBookings(listOf(incomingDto), now = syncTime)

        val captured = slot<List<BookingEntity>>()
        coVerify { bookingDao.insertBookings(capture(captured)) }

        val synced = captured.captured.first()
        assertEquals("statusSinceMillis must be updated to now when status changes", syncTime, synced.statusSinceMillis)
        assertEquals(syncTime, synced.lastSyncMillis)
    }

    @Test
    fun `syncBookings supports nullable guideId and guideName for waitlist promoted booking`() = runTest {
        coEvery { bookingDao.getAllBookings() } returns emptyList()

        val incomingDto = BookingResponse(
            id = "booking-waitlist-1",
            slotId = "slot-99",
            activityTitle = "Annapurna Circuit",
            trekkerId = "user-2",
            trekkerName = "Jane Trekker",
            guideId = null,
            guideName = null,
            status = BookingStatus.REMATCH_FAILED,
            date = "2026-07-15T08:00:00",
            paymentOrderId = null,
            depositAmount = 0.0,
            refundAmount = 0.0,
            refundPercentage = 0.0
        )

        val syncTime = 9000L
        bookingDao.syncBookings(listOf(incomingDto), now = syncTime)

        val captured = slot<List<BookingEntity>>()
        coVerify { bookingDao.insertBookings(capture(captured)) }

        val synced = captured.captured.first()
        assertNull(synced.guideId)
        assertNull(synced.guideName)
        assertEquals(BookingStatus.REMATCH_FAILED, synced.status)
        assertEquals(syncTime, synced.statusSinceMillis)
    }
}
