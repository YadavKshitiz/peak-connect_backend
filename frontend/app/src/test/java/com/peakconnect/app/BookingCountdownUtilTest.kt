package com.peakconnect.app

import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.feature.trekker.booking.BookingCountdownCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookingCountdownUtilTest {

    @Test
    fun `window duration matches backend configuration`() {
        assertEquals(20 * 60 * 1000L, BookingCountdownCalculator.GUIDE_RESPONSE_WINDOW_MILLIS)
        assertEquals(15 * 60 * 1000L, BookingCountdownCalculator.PAYMENT_WINDOW_MILLIS)

        assertEquals(
            BookingCountdownCalculator.GUIDE_RESPONSE_WINDOW_MILLIS,
            BookingCountdownCalculator.getWindowDurationMillis(BookingStatus.PENDING_GUIDE_RESPONSE)
        )
        assertEquals(
            BookingCountdownCalculator.PAYMENT_WINDOW_MILLIS,
            BookingCountdownCalculator.getWindowDurationMillis(BookingStatus.AWAITING_PAYMENT)
        )
        assertEquals(
            0L,
            BookingCountdownCalculator.getWindowDurationMillis(BookingStatus.CONFIRMED)
        )
        assertEquals(
            0L,
            BookingCountdownCalculator.getWindowDurationMillis(BookingStatus.CANCELLED)
        )
    }

    @Test
    fun `calculateRemainingMillis calculates correct time with fake clock`() {
        val window = 20 * 60 * 1000L // 20 minutes
        val start = 1_000_000L

        // At start (0 elapsed)
        val remaining0 = BookingCountdownCalculator.calculateRemainingMillis(
            statusSinceMillis = start,
            windowDurationMillis = window,
            nowMillis = start
        )
        assertEquals(window, remaining0)

        // After 5 minutes (300,000 ms elapsed)
        val remaining5 = BookingCountdownCalculator.calculateRemainingMillis(
            statusSinceMillis = start,
            windowDurationMillis = window,
            nowMillis = start + 300_000L
        )
        assertEquals(15 * 60 * 1000L, remaining5)

        // After 19.5 minutes (1,170,000 ms elapsed) -> 30 seconds left
        val remaining19 = BookingCountdownCalculator.calculateRemainingMillis(
            statusSinceMillis = start,
            windowDurationMillis = window,
            nowMillis = start + 1_170_000L
        )
        assertEquals(30_000L, remaining19)
    }

    @Test
    fun `stop at zero behavior never returns negative or ticks below zero`() {
        val window = 15 * 60 * 1000L // 15 minutes
        val start = 1_000_000L

        // Exactly at expiry
        val atExpiry = BookingCountdownCalculator.calculateRemainingMillis(
            statusSinceMillis = start,
            windowDurationMillis = window,
            nowMillis = start + window
        )
        assertEquals(0L, atExpiry)
        assertTrue(BookingCountdownCalculator.isExpired(start, window, start + window))

        // 10 minutes past expiry
        val pastExpiry = BookingCountdownCalculator.calculateRemainingMillis(
            statusSinceMillis = start,
            windowDurationMillis = window,
            nowMillis = start + window + 600_000L
        )
        assertEquals(0L, pastExpiry)
        assertTrue(BookingCountdownCalculator.isExpired(start, window, start + window + 600_000L))
    }

    @Test
    fun `formatMinutesRemaining uses ceiling logic`() {
        assertEquals(0, BookingCountdownCalculator.formatMinutesRemaining(0L))
        assertEquals(1, BookingCountdownCalculator.formatMinutesRemaining(1000L)) // 1 second -> 1 min
        assertEquals(1, BookingCountdownCalculator.formatMinutesRemaining(60_000L)) // 60 seconds -> 1 min
        assertEquals(2, BookingCountdownCalculator.formatMinutesRemaining(61_000L)) // 61 seconds -> 2 min
        assertEquals(15, BookingCountdownCalculator.formatMinutesRemaining(15 * 60 * 1000L))
    }

    @Test
    fun `calculateState returns expired state when time is up`() {
        val window = 15 * 60 * 1000L
        val start = 5_000_000L

        val stateActive = BookingCountdownCalculator.calculateState(
            statusSinceMillis = start,
            windowDurationMillis = window,
            nowMillis = start + 10 * 60 * 1000L
        )
        assertFalse(stateActive.isExpired)
        assertEquals(5 * 60 * 1000L, stateActive.remainingMillis)
        assertEquals(5, stateActive.minutesRemaining)
        assertEquals("05:00", stateActive.formattedTime)

        val stateExpired = BookingCountdownCalculator.calculateState(
            statusSinceMillis = start,
            windowDurationMillis = window,
            nowMillis = start + 16 * 60 * 1000L
        )
        assertTrue(stateExpired.isExpired)
        assertEquals(0L, stateExpired.remainingMillis)
        assertEquals(0, stateExpired.minutesRemaining)
        assertEquals("00:00", stateExpired.formattedTime)
    }
}
