package com.peakconnect.app

import com.peakconnect.app.data.dto.BookingResponse
import com.peakconnect.app.data.local.entity.BookingEntity
import com.peakconnect.app.data.local.entity.GuideBookingEntity
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.feature.notifications.sync.CancelReminder
import com.peakconnect.app.feature.notifications.sync.PendingReminder
import com.peakconnect.app.feature.notifications.sync.ReminderType
import com.peakconnect.app.feature.notifications.sync.SyncDiffCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class SyncDiffCalculatorTest {

    private val now = 100_000L
    private val dateTimeStr = "2026-11-05T09:00:00"

    private fun createBookingDto(
        id: String = "b1",
        slotId: String = "s1",
        status: BookingStatus = BookingStatus.PENDING_GUIDE_RESPONSE,
        guideName: String? = "Tenzing",
        activityTitle: String = "Hampta Pass",
        trekkerName: String = "Alex"
    ) = BookingResponse(
        id = id,
        slotId = slotId,
        activityTitle = activityTitle,
        trekkerId = "u1",
        trekkerName = trekkerName,
        guideId = "g1",
        guideName = guideName,
        status = status,
        date = dateTimeStr,
        paymentOrderId = null,
        depositAmount = 50.0,
        refundAmount = null,
        refundPercentage = null
    )

    private fun createBookingEntity(
        id: String = "b1",
        slotId: String = "s1",
        status: BookingStatus = BookingStatus.PENDING_GUIDE_RESPONSE,
        statusSince: Long = 50_000L
    ) = BookingEntity(
        id = id,
        slotId = slotId,
        activityTitle = "Hampta Pass",
        trekkerId = "u1",
        trekkerName = "Alex",
        guideId = "g1",
        guideName = "Tenzing",
        status = status,
        date = LocalDateTime.parse(dateTimeStr),
        paymentOrderId = null,
        depositAmount = 50.0,
        refundAmount = null,
        refundPercentage = null,
        statusSinceMillis = statusSince,
        lastSyncMillis = 50_000L
    )

    // ==========================================
    // TREKKER TESTS
    // ==========================================

    @Test
    fun `trekker first-run seeding updates room but emits zero notifications and zero reminders`() {
        val incoming = listOf(
            createBookingDto(id = "b1", status = BookingStatus.PENDING_GUIDE_RESPONSE),
            createBookingDto(id = "b2", status = BookingStatus.AWAITING_PAYMENT),
            createBookingDto(id = "b3", status = BookingStatus.CONFIRMED)
        )

        val result = SyncDiffCalculator.calculateTrekkerDiff(
            previousBookings = emptyList(),
            incomingBookings = incoming,
            isFirstSync = true,
            activeBookingId = null,
            alreadyNotifiedKeys = emptySet(),
            nowMillis = now
        )

        assertEquals(3, result.updatedBookings.size)
        assertTrue("Notifications must be empty on first sync seeding", result.notifications.isEmpty())
        assertTrue("Reminders must be empty on first sync seeding", result.remindersToSchedule.isEmpty())
        assertTrue(result.remindersToCancel.isEmpty())
    }

    @Test
    fun `trekker pending to awaiting payment emits notification and schedules payment reminder`() {
        val prev = listOf(createBookingEntity(id = "b1", status = BookingStatus.PENDING_GUIDE_RESPONSE))
        val incoming = listOf(createBookingDto(id = "b1", status = BookingStatus.AWAITING_PAYMENT))

        val result = SyncDiffCalculator.calculateTrekkerDiff(
            previousBookings = prev,
            incomingBookings = incoming,
            isFirstSync = false,
            activeBookingId = null,
            alreadyNotifiedKeys = emptySet(),
            nowMillis = now
        )

        assertEquals(1, result.notifications.size)
        val notif = result.notifications[0]
        assertEquals(R.string.notif_trekker_guide_accepted_title, notif.titleRes)
        assertEquals(R.string.notif_trekker_guide_accepted_body, notif.bodyRes)
        assertEquals("booking_b1_AWAITING_PAYMENT", notif.dedupeKey)

        assertEquals(1, result.remindersToSchedule.size)
        val reminder = result.remindersToSchedule[0] as PendingReminder.PaymentReminder
        assertEquals("b1", reminder.bookingId)
        assertEquals(now, reminder.statusSinceMillis)
    }

    @Test
    fun `trekker transition to confirmed emits notification, cancels payment reminder and schedules trek countdown`() {
        val prev = listOf(createBookingEntity(id = "b1", status = BookingStatus.AWAITING_PAYMENT))
        val incoming = listOf(createBookingDto(id = "b1", status = BookingStatus.CONFIRMED))

        val result = SyncDiffCalculator.calculateTrekkerDiff(
            previousBookings = prev,
            incomingBookings = incoming,
            isFirstSync = false,
            activeBookingId = null,
            alreadyNotifiedKeys = emptySet(),
            nowMillis = now
        )

        assertEquals(1, result.notifications.size)
        val notif = result.notifications[0]
        assertEquals(R.string.notif_trekker_confirmed_title, notif.titleRes)
        assertEquals("booking_b1_CONFIRMED", notif.dedupeKey)

        assertEquals(listOf(CancelReminder("b1", ReminderType.PAYMENT)), result.remindersToCancel)
        assertEquals(1, result.remindersToSchedule.size)
        assertTrue(result.remindersToSchedule[0] is PendingReminder.TrekTomorrowReminder)
    }

    @Test
    fun `trekker transition to cancelled emits notification and cancels all reminders`() {
        val prev = listOf(createBookingEntity(id = "b1", status = BookingStatus.CONFIRMED))
        val incoming = listOf(createBookingDto(id = "b1", status = BookingStatus.CANCELLED))

        val result = SyncDiffCalculator.calculateTrekkerDiff(
            previousBookings = prev,
            incomingBookings = incoming,
            isFirstSync = false,
            activeBookingId = null,
            alreadyNotifiedKeys = emptySet(),
            nowMillis = now
        )

        assertEquals(1, result.notifications.size)
        val notif = result.notifications[0]
        assertEquals(R.string.notif_trekker_cancelled_title, notif.titleRes)
        assertEquals("booking_b1_CANCELLED", notif.dedupeKey)
        assertEquals(listOf(CancelReminder("b1", ReminderType.ALL)), result.remindersToCancel)
    }

    @Test
    fun `trekker transition to rematch failed emits notification and cancels all reminders`() {
        val prev = listOf(createBookingEntity(id = "b1", status = BookingStatus.PENDING_GUIDE_RESPONSE))
        val incoming = listOf(createBookingDto(id = "b1", status = BookingStatus.REMATCH_FAILED))

        val result = SyncDiffCalculator.calculateTrekkerDiff(
            previousBookings = prev,
            incomingBookings = incoming,
            isFirstSync = false,
            activeBookingId = null,
            alreadyNotifiedKeys = emptySet(),
            nowMillis = now
        )

        assertEquals(1, result.notifications.size)
        val notif = result.notifications[0]
        assertEquals(R.string.notif_trekker_rematch_failed_title, notif.titleRes)
        assertEquals("booking_b1_REMATCH_FAILED", notif.dedupeKey)
        assertEquals(listOf(CancelReminder("b1", ReminderType.ALL)), result.remindersToCancel)
    }

    @Test
    fun `waitlist promotion emits spot opened up notification and returns promoted slot id`() {
        val prev = emptyList<BookingEntity>()
        val incoming = listOf(createBookingDto(id = "b_new", slotId = "slot_waitlist_123"))

        val result = SyncDiffCalculator.calculateTrekkerDiff(
            previousBookings = prev,
            incomingBookings = incoming,
            isFirstSync = false,
            activeBookingId = null,
            alreadyNotifiedKeys = emptySet(),
            waitlistSlotIds = setOf("slot_waitlist_123"),
            nowMillis = now
        )

        assertEquals(1, result.notifications.size)
        val notif = result.notifications[0]
        assertEquals(R.string.notif_trekker_waitlist_promoted_title, notif.titleRes)
        assertEquals("waitlist_promoted_b_new", notif.dedupeKey)
        assertEquals(listOf("slot_waitlist_123"), result.promotedWaitlistSlotIds)
    }

    @Test
    fun `trekker diff dedupes notification if key already notified`() {
        val prev = listOf(createBookingEntity(id = "b1", status = BookingStatus.PENDING_GUIDE_RESPONSE))
        val incoming = listOf(createBookingDto(id = "b1", status = BookingStatus.AWAITING_PAYMENT))

        val result = SyncDiffCalculator.calculateTrekkerDiff(
            previousBookings = prev,
            incomingBookings = incoming,
            isFirstSync = false,
            activeBookingId = null,
            alreadyNotifiedKeys = setOf("booking_b1_AWAITING_PAYMENT"),
            nowMillis = now
        )

        assertTrue("Notification should be suppressed by dedupe key", result.notifications.isEmpty())
        assertEquals(1, result.remindersToSchedule.size)
    }

    @Test
    fun `trekker diff suppresses notification if booking screen is currently active in foreground`() {
        val prev = listOf(createBookingEntity(id = "b1", status = BookingStatus.PENDING_GUIDE_RESPONSE))
        val incoming = listOf(createBookingDto(id = "b1", status = BookingStatus.AWAITING_PAYMENT))

        val result = SyncDiffCalculator.calculateTrekkerDiff(
            previousBookings = prev,
            incomingBookings = incoming,
            isFirstSync = false,
            activeBookingId = "b1",
            alreadyNotifiedKeys = emptySet(),
            nowMillis = now
        )

        assertTrue("Notification should be suppressed because user is on this screen", result.notifications.isEmpty())
    }

    @Test
    fun `trekker diff preserves statusSinceMillis if status has not changed`() {
        val prev = listOf(createBookingEntity(id = "b1", status = BookingStatus.CONFIRMED, statusSince = 20_000L))
        val incoming = listOf(createBookingDto(id = "b1", status = BookingStatus.CONFIRMED))

        val result = SyncDiffCalculator.calculateTrekkerDiff(
            previousBookings = prev,
            incomingBookings = incoming,
            isFirstSync = false,
            activeBookingId = null,
            alreadyNotifiedKeys = emptySet(),
            nowMillis = 90_000L
        )

        assertEquals(20_000L, result.updatedBookings[0].statusSinceMillis)
        assertEquals(90_000L, result.updatedBookings[0].lastSyncMillis)
    }

    // ==========================================
    // GUIDE TESTS
    // ==========================================

    private fun createGuideBookingEntity(
        id: String = "gb1",
        status: BookingStatus = BookingStatus.PENDING_GUIDE_RESPONSE,
        statusSince: Long = 50_000L
    ) = GuideBookingEntity(
        id = id,
        slotId = "s1",
        activityTitle = "Everest Base Camp",
        trekkerId = "u1",
        trekkerName = "Alex",
        guideId = "g1",
        guideName = "Tenzing",
        status = status,
        date = LocalDateTime.parse(dateTimeStr),
        paymentOrderId = null,
        depositAmount = 50.0,
        refundAmount = null,
        refundPercentage = null,
        statusSinceMillis = statusSince,
        lastSyncMillis = 50_000L
    )

    @Test
    fun `guide first-run seeding updates room but emits zero notifications and zero reminders`() {
        val incoming = listOf(
            createBookingDto(id = "gb1", status = BookingStatus.PENDING_GUIDE_RESPONSE),
            createBookingDto(id = "gb2", status = BookingStatus.CONFIRMED)
        )

        val result = SyncDiffCalculator.calculateGuideDiff(
            previousBookings = emptyList(),
            incomingBookings = incoming,
            isFirstSync = true,
            activeBookingId = null,
            alreadyNotifiedKeys = emptySet(),
            nowMillis = now
        )

        assertEquals(2, result.updatedBookings.size)
        assertTrue(result.notifications.isEmpty())
        assertTrue(result.remindersToSchedule.isEmpty())
    }

    @Test
    fun `guide new pending booking emits notification and schedules guide response reminder`() {
        val prev = emptyList<GuideBookingEntity>()
        val incoming = listOf(createBookingDto(id = "gb1", status = BookingStatus.PENDING_GUIDE_RESPONSE))

        val result = SyncDiffCalculator.calculateGuideDiff(
            previousBookings = prev,
            incomingBookings = incoming,
            isFirstSync = false,
            activeBookingId = null,
            alreadyNotifiedKeys = emptySet(),
            nowMillis = now
        )

        assertEquals(1, result.notifications.size)
        val notif = result.notifications[0]
        assertEquals(R.string.notif_guide_new_request_title, notif.titleRes)
        assertEquals("guide_new_request_gb1", notif.dedupeKey)
        assertTrue(notif.isGuide)

        assertEquals(1, result.remindersToSchedule.size)
        val reminder = result.remindersToSchedule[0] as PendingReminder.GuideResponseReminder
        assertEquals("gb1", reminder.bookingId)
        assertEquals(now, reminder.firstSeenMillis)
    }

    @Test
    fun `guide transition to confirmed emits trip confirmed notification and cancels guide response reminder`() {
        val prev = listOf(createGuideBookingEntity(id = "gb1", status = BookingStatus.AWAITING_PAYMENT))
        val incoming = listOf(createBookingDto(id = "gb1", status = BookingStatus.CONFIRMED, trekkerName = "Alex"))

        val result = SyncDiffCalculator.calculateGuideDiff(
            previousBookings = prev,
            incomingBookings = incoming,
            isFirstSync = false,
            activeBookingId = null,
            alreadyNotifiedKeys = emptySet(),
            nowMillis = now
        )

        assertEquals(1, result.notifications.size)
        val notif = result.notifications[0]
        assertEquals(R.string.notif_guide_confirmed_title, notif.titleRes)
        assertEquals("guide_booking_gb1_CONFIRMED", notif.dedupeKey)
        assertEquals(listOf(CancelReminder("gb1", ReminderType.GUIDE_RESPONSE)), result.remindersToCancel)
    }

    @Test
    fun `guide transition to cancelled emits cancellation notification and cancels all reminders`() {
        val prev = listOf(createGuideBookingEntity(id = "gb1", status = BookingStatus.CONFIRMED))
        val incoming = listOf(createBookingDto(id = "gb1", status = BookingStatus.CANCELLED))

        val result = SyncDiffCalculator.calculateGuideDiff(
            previousBookings = prev,
            incomingBookings = incoming,
            isFirstSync = false,
            activeBookingId = null,
            alreadyNotifiedKeys = emptySet(),
            nowMillis = now
        )

        assertEquals(1, result.notifications.size)
        val notif = result.notifications[0]
        assertEquals(R.string.notif_guide_cancelled_title, notif.titleRes)
        assertEquals("guide_booking_gb1_CANCELLED", notif.dedupeKey)
        assertEquals(listOf(CancelReminder("gb1", ReminderType.ALL)), result.remindersToCancel)
    }

    // ==========================================
    // TOKEN EXPIRY TESTS
    // ==========================================

    @Test
    fun `token expiry notification emitted exactly once when token expired and not yet notified`() {
        val notif = SyncDiffCalculator.calculateTokenExpiryNotification(
            isTokenExpired = true,
            isAlreadyNotified = false,
            tokenExpiryKey = "token_expired_session"
        )
        assertNotNull(notif)
        assertEquals(R.string.notif_token_expired_title, notif!!.titleRes)
        assertEquals(R.string.notif_token_expired_body, notif.bodyRes)
        assertEquals("token_expired_session", notif.dedupeKey)

        // When already notified, must be null (deduped, no spam)
        val secondCheck = SyncDiffCalculator.calculateTokenExpiryNotification(
            isTokenExpired = true,
            isAlreadyNotified = true,
            tokenExpiryKey = "token_expired_session"
        )
        assertNull(secondCheck)
    }

    @Test
    fun `token expiry returns null if token is not expired`() {
        val notif = SyncDiffCalculator.calculateTokenExpiryNotification(
            isTokenExpired = false,
            isAlreadyNotified = false
        )
        assertNull(notif)
    }
}
