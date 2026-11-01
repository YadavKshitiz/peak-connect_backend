package com.peakconnect.app

import com.peakconnect.app.data.local.dao.UserContentDao
import com.peakconnect.app.data.local.entity.BookingEntity
import com.peakconnect.app.data.local.entity.WaitlistEntryEntity
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.WaitlistPromoted
import com.peakconnect.app.domain.model.WaitlistStatus
import com.peakconnect.app.feature.trekker.waitlist.WaitlistEventBus
import com.peakconnect.app.feature.trekker.waitlist.WaitlistPromotionDetector
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WaitlistPromotionDetectorTest {

    private val userContentDao: UserContentDao = mockk(relaxed = true)
    private val waitlistEventBus: WaitlistEventBus = mockk(relaxed = true)
    private lateinit var detector: WaitlistPromotionDetector

    private val fixedNow = LocalDateTime.of(2026, 11, 1, 12, 0)
    private val futureSlotDate = fixedNow.plusDays(5)
    private val pastSlotDate = fixedNow.minusDays(1)

    private fun createBooking(
        id: String,
        slotId: String,
        status: BookingStatus = BookingStatus.PENDING_GUIDE_RESPONSE,
        guideName: String? = "Tenzing"
    ): BookingEntity {
        return BookingEntity(
            id = id,
            slotId = slotId,
            activityTitle = "Kuari Pass",
            trekkerId = "trekker-1",
            trekkerName = "Alex",
            guideId = "guide-1",
            guideName = guideName,
            status = status,
            date = futureSlotDate,
            paymentOrderId = null,
            depositAmount = 500.0,
            refundAmount = null,
            refundPercentage = null,
            statusSinceMillis = 1000L,
            lastSyncMillis = 1000L
        )
    }

    private fun createWaitlistEntry(
        slotId: String,
        status: WaitlistStatus = WaitlistStatus.WAITING,
        bookingId: String? = null,
        slotDate: LocalDateTime = futureSlotDate,
        guideName: String? = "Tenzing"
    ): WaitlistEntryEntity {
        return WaitlistEntryEntity(
            slotId = slotId,
            activityTitle = "Kuari Pass",
            slotDate = slotDate,
            joinedAt = 1000L,
            status = status,
            guideId = "guide-1",
            guideName = guideName,
            bookingId = bookingId,
            closureReason = null
        )
    }

    @Before
    fun setUp() {
        detector = WaitlistPromotionDetector(userContentDao, waitlistEventBus)
    }

    @Test
    fun `new booking appears for a waited slot promotes entry and emits event`() = runTest {
        val entry = createWaitlistEntry("slot-1", status = WaitlistStatus.WAITING)
        coEvery { userContentDao.getAllWaitlistEntries() } returns listOf(entry)

        val newBooking = createBooking("booking-1", slotId = "slot-1", guideName = "Tenzing")
        val promotedList = detector.detectPromotions(listOf(newBooking), now = fixedNow)

        assertEquals(1, promotedList.size)
        val promoted = promotedList.first()
        assertEquals("slot-1", promoted.slotId)
        assertEquals(WaitlistStatus.PROMOTED, promoted.status)
        assertEquals("booking-1", promoted.bookingId)
        assertEquals("Tenzing", promoted.guideName)

        val capturedEntry = slot<WaitlistEntryEntity>()
        coVerify { userContentDao.insertWaitlistEntry(capture(capturedEntry)) }
        assertEquals(WaitlistStatus.PROMOTED, capturedEntry.captured.status)
        assertEquals("booking-1", capturedEntry.captured.bookingId)

        coVerify { waitlistEventBus.emit(WaitlistPromoted(bookingId = "booking-1", slotId = "slot-1")) }
    }

    @Test
    fun `cancelled new booking is ignored and does not promote entry`() = runTest {
        val entry = createWaitlistEntry("slot-1", status = WaitlistStatus.WAITING)
        coEvery { userContentDao.getAllWaitlistEntries() } returns listOf(entry)

        val cancelledBooking = createBooking("booking-1", slotId = "slot-1", status = BookingStatus.CANCELLED)
        val promotedList = detector.detectPromotions(listOf(cancelledBooking), now = fixedNow)

        assertTrue(promotedList.isEmpty())
        coVerify(exactly = 0) { waitlistEventBus.emit(any()) }
    }

    @Test
    fun `unrelated slot booking is ignored`() = runTest {
        val entry = createWaitlistEntry("slot-1", status = WaitlistStatus.WAITING)
        coEvery { userContentDao.getAllWaitlistEntries() } returns listOf(entry)

        val otherBooking = createBooking("booking-other", slotId = "slot-999")
        val promotedList = detector.detectPromotions(listOf(otherBooking), now = fixedNow)

        assertTrue(promotedList.isEmpty())
        coVerify(exactly = 0) { waitlistEventBus.emit(any()) }
    }

    @Test
    fun `already-seen booking is ignored on subsequent detection run`() = runTest {
        val entry = createWaitlistEntry("slot-1", status = WaitlistStatus.WAITING)
        coEvery { userContentDao.getAllWaitlistEntries() } returns listOf(entry)

        val booking = createBooking("booking-1", slotId = "slot-1")

        // First run promotes it
        detector.detectPromotions(listOf(booking), now = fixedNow)

        // Reset mock
        io.mockk.clearMocks(userContentDao, waitlistEventBus, answers = false)
        coEvery { userContentDao.getAllWaitlistEntries() } returns listOf(entry)

        // Second run with the same booking ID must ignore it
        val secondRunPromotions = detector.detectPromotions(listOf(booking), now = fixedNow)
        assertTrue(secondRunPromotions.isEmpty())
        coVerify(exactly = 0) { waitlistEventBus.emit(any()) }
    }

    @Test
    fun `pre-initialized known booking ids are ignored`() = runTest {
        detector.initializeKnownBookings(listOf("booking-already-known"))

        val entry = createWaitlistEntry("slot-1", status = WaitlistStatus.WAITING)
        coEvery { userContentDao.getAllWaitlistEntries() } returns listOf(entry)

        val booking = createBooking("booking-already-known", slotId = "slot-1")
        val promotedList = detector.detectPromotions(listOf(booking), now = fixedNow)

        assertTrue(promotedList.isEmpty())
        coVerify(exactly = 0) { waitlistEventBus.emit(any()) }
    }

    @Test
    fun `linked booking reaching CONFIRMED closes entry with Booked reason`() = runTest {
        val promotedEntry = createWaitlistEntry(
            slotId = "slot-1",
            status = WaitlistStatus.PROMOTED,
            bookingId = "booking-1"
        )
        coEvery { userContentDao.getAllWaitlistEntries() } returns listOf(promotedEntry)

        val confirmedBooking = createBooking("booking-1", slotId = "slot-1", status = BookingStatus.CONFIRMED)
        detector.detectPromotions(listOf(confirmedBooking), now = fixedNow)

        val capturedEntry = slot<WaitlistEntryEntity>()
        coVerify { userContentDao.insertWaitlistEntry(capture(capturedEntry)) }
        assertEquals(WaitlistStatus.CLOSED, capturedEntry.captured.status)
        assertEquals("Booked", capturedEntry.captured.closureReason)
    }

    @Test
    fun `linked booking reaching CANCELLED closes entry with Cancelled reason`() = runTest {
        val promotedEntry = createWaitlistEntry(
            slotId = "slot-1",
            status = WaitlistStatus.PROMOTED,
            bookingId = "booking-1"
        )
        coEvery { userContentDao.getAllWaitlistEntries() } returns listOf(promotedEntry)

        val cancelledBooking = createBooking("booking-1", slotId = "slot-1", status = BookingStatus.CANCELLED)
        detector.detectPromotions(listOf(cancelledBooking), now = fixedNow)

        val capturedEntry = slot<WaitlistEntryEntity>()
        coVerify { userContentDao.insertWaitlistEntry(capture(capturedEntry)) }
        assertEquals(WaitlistStatus.CLOSED, capturedEntry.captured.status)
        assertEquals("Cancelled", capturedEntry.captured.closureReason)
    }

    @Test
    fun `linked booking reaching REMATCH_FAILED closes entry with No guide available reason`() = runTest {
        val promotedEntry = createWaitlistEntry(
            slotId = "slot-1",
            status = WaitlistStatus.PROMOTED,
            bookingId = "booking-1"
        )
        coEvery { userContentDao.getAllWaitlistEntries() } returns listOf(promotedEntry)

        val rematchFailedBooking = createBooking("booking-1", slotId = "slot-1", status = BookingStatus.REMATCH_FAILED)
        detector.detectPromotions(listOf(rematchFailedBooking), now = fixedNow)

        val capturedEntry = slot<WaitlistEntryEntity>()
        coVerify { userContentDao.insertWaitlistEntry(capture(capturedEntry)) }
        assertEquals(WaitlistStatus.CLOSED, capturedEntry.captured.status)
        assertEquals("No guide available", capturedEntry.captured.closureReason)
    }

    @Test
    fun `linked booking reaching NO_SHOW closes entry with No-show reason`() = runTest {
        val promotedEntry = createWaitlistEntry(
            slotId = "slot-1",
            status = WaitlistStatus.PROMOTED,
            bookingId = "booking-1"
        )
        coEvery { userContentDao.getAllWaitlistEntries() } returns listOf(promotedEntry)

        val noShowBooking = createBooking("booking-1", slotId = "slot-1", status = BookingStatus.NO_SHOW)
        detector.detectPromotions(listOf(noShowBooking), now = fixedNow)

        val capturedEntry = slot<WaitlistEntryEntity>()
        coVerify { userContentDao.insertWaitlistEntry(capture(capturedEntry)) }
        assertEquals(WaitlistStatus.CLOSED, capturedEntry.captured.status)
        assertEquals("No-show", capturedEntry.captured.closureReason)
    }

    @Test
    fun `waiting entry whose slot date has passed becomes CLOSED with Slot date passed reason`() = runTest {
        val expiredWaitingEntry = createWaitlistEntry(
            slotId = "slot-old",
            status = WaitlistStatus.WAITING,
            slotDate = pastSlotDate
        )
        coEvery { userContentDao.getAllWaitlistEntries() } returns listOf(expiredWaitingEntry)

        detector.detectPromotions(emptyList(), now = fixedNow)

        val capturedEntry = slot<WaitlistEntryEntity>()
        coVerify { userContentDao.insertWaitlistEntry(capture(capturedEntry)) }
        assertEquals(WaitlistStatus.CLOSED, capturedEntry.captured.status)
        assertEquals("Slot date passed", capturedEntry.captured.closureReason)
    }

    @Test
    fun `booking with null guide safely promotes waitlist entry with fallback guideName`() = runTest {
        val entry = createWaitlistEntry("slot-1", status = WaitlistStatus.WAITING, guideName = "PreferredGuide")
        coEvery { userContentDao.getAllWaitlistEntries() } returns listOf(entry)

        val nullGuideBooking = createBooking("booking-null-guide", slotId = "slot-1", guideName = null)
        val promotedList = detector.detectPromotions(listOf(nullGuideBooking), now = fixedNow)

        assertEquals(1, promotedList.size)
        assertEquals("PreferredGuide", promotedList.first().guideName)
    }
}
