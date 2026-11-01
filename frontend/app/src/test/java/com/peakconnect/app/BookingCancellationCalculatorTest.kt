package com.peakconnect.app

import com.peakconnect.app.data.local.entity.BookingEntity
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.CancellationPolicyType
import com.peakconnect.app.feature.trekker.booking.BookingCancellationCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class BookingCancellationCalculatorTest {

    private fun createBooking(
        status: BookingStatus = BookingStatus.CONFIRMED,
        date: LocalDateTime = LocalDateTime.of(2026, 11, 20, 10, 0),
        depositAmount: Double? = 400.0
    ): BookingEntity {
        return BookingEntity(
            id = "booking-test-1",
            slotId = "slot-test-1",
            activityTitle = "Triund Trek",
            trekkerId = "trekker-1",
            trekkerName = "Alex",
            guideId = "guide-1",
            guideName = "Tenzing",
            status = status,
            date = date,
            paymentOrderId = "order_123",
            depositAmount = depositAmount,
            refundAmount = null,
            refundPercentage = null,
            statusSinceMillis = 1000L,
            lastSyncMillis = 1000L
        )
    }

    @Test
    fun `flexible policy preview for all time bands`() {
        val activityDate = LocalDateTime.of(2026, 11, 20, 10, 0)
        val totalPrice = BigDecimal("2000.00")
        val deposit = 400.0 // 20%

        // Band 1: > 48h (e.g. 72h before) -> 100% refund of (2000 - 400) = 1600.00, tier 0
        val now72h = activityDate.minusHours(72)
        val preview72h = BookingCancellationCalculator.calculatePreview(
            booking = createBooking(date = activityDate, depositAmount = deposit),
            policy = CancellationPolicyType.FLEXIBLE,
            totalPrice = totalPrice,
            now = now72h
        )
        assertTrue(preview72h.isPaymentTaken)
        assertEquals(100, preview72h.refundPercentage)
        assertEquals(1.0, preview72h.refundPercentageRatio, 0.001)
        assertEquals(0, preview72h.activeTierIndex)
        assertEquals(BigDecimal("1600.00"), preview72h.refundableBase)
        assertEquals(BigDecimal("1600.00"), preview72h.estimatedRefundAmount)
        assertEquals(BigDecimal("400.00"), preview72h.depositAmount)

        // Band 2: 12h to 48h (e.g. 24h before) -> 50% refund = 800.00, tier 1
        val now24h = activityDate.minusHours(24)
        val preview24h = BookingCancellationCalculator.calculatePreview(
            booking = createBooking(date = activityDate, depositAmount = deposit),
            policy = CancellationPolicyType.FLEXIBLE,
            totalPrice = totalPrice,
            now = now24h
        )
        assertEquals(50, preview24h.refundPercentage)
        assertEquals(0.5, preview24h.refundPercentageRatio, 0.001)
        assertEquals(1, preview24h.activeTierIndex)
        assertEquals(BigDecimal("800.00"), preview24h.estimatedRefundAmount)

        // Band 3: < 12h (e.g. 5h before) -> 0% refund = 0.00, tier 2
        val now5h = activityDate.minusHours(5)
        val preview5h = BookingCancellationCalculator.calculatePreview(
            booking = createBooking(date = activityDate, depositAmount = deposit),
            policy = CancellationPolicyType.FLEXIBLE,
            totalPrice = totalPrice,
            now = now5h
        )
        assertEquals(0, preview5h.refundPercentage)
        assertEquals(0.0, preview5h.refundPercentageRatio, 0.001)
        assertEquals(2, preview5h.activeTierIndex)
        assertEquals(BigDecimal("0.00"), preview5h.estimatedRefundAmount)
    }

    @Test
    fun `moderate policy preview for all time bands`() {
        val activityDate = LocalDateTime.of(2026, 11, 20, 10, 0)
        val totalPrice = BigDecimal("3000.00")
        val deposit = 600.0

        // Band 1: > 7 days (168h, e.g. 200h before) -> 100% refund, tier 0
        val now200h = activityDate.minusHours(200)
        val preview200h = BookingCancellationCalculator.calculatePreview(
            booking = createBooking(date = activityDate, depositAmount = deposit),
            policy = CancellationPolicyType.MODERATE,
            totalPrice = totalPrice,
            now = now200h
        )
        assertEquals(100, preview200h.refundPercentage)
        assertEquals(0, preview200h.activeTierIndex)
        assertEquals(BigDecimal("2400.00"), preview200h.estimatedRefundAmount)

        // Band 2: 2 to 7 days (48h to 168h, e.g. 72h before) -> 50% refund, tier 1
        val now72h = activityDate.minusHours(72)
        val preview72h = BookingCancellationCalculator.calculatePreview(
            booking = createBooking(date = activityDate, depositAmount = deposit),
            policy = CancellationPolicyType.MODERATE,
            totalPrice = totalPrice,
            now = now72h
        )
        assertEquals(50, preview72h.refundPercentage)
        assertEquals(1, preview72h.activeTierIndex)
        assertEquals(BigDecimal("1200.00"), preview72h.estimatedRefundAmount)

        // Band 3: < 48h (e.g. 30h before) -> 0% refund, tier 2
        val now30h = activityDate.minusHours(30)
        val preview30h = BookingCancellationCalculator.calculatePreview(
            booking = createBooking(date = activityDate, depositAmount = deposit),
            policy = CancellationPolicyType.MODERATE,
            totalPrice = totalPrice,
            now = now30h
        )
        assertEquals(0, preview30h.refundPercentage)
        assertEquals(2, preview30h.activeTierIndex)
        assertEquals(BigDecimal("0.00"), preview30h.estimatedRefundAmount)
    }

    @Test
    fun `strict policy preview for all time bands`() {
        val activityDate = LocalDateTime.of(2026, 11, 20, 10, 0)
        val totalPrice = BigDecimal("5000.00")
        val deposit = 1000.0

        // Band 1: > 14 days (336h, e.g. 400h before) -> 50% refund, tier 0
        val now400h = activityDate.minusHours(400)
        val preview400h = BookingCancellationCalculator.calculatePreview(
            booking = createBooking(date = activityDate, depositAmount = deposit),
            policy = CancellationPolicyType.STRICT,
            totalPrice = totalPrice,
            now = now400h
        )
        assertEquals(50, preview400h.refundPercentage)
        assertEquals(0, preview400h.activeTierIndex)
        assertEquals(BigDecimal("2000.00"), preview400h.estimatedRefundAmount)

        // Band 2: < 14 days (e.g. 100h before) -> 0% refund, tier 1
        val now100h = activityDate.minusHours(100)
        val preview100h = BookingCancellationCalculator.calculatePreview(
            booking = createBooking(date = activityDate, depositAmount = deposit),
            policy = CancellationPolicyType.STRICT,
            totalPrice = totalPrice,
            now = now100h
        )
        assertEquals(0, preview100h.refundPercentage)
        assertEquals(1, preview100h.activeTierIndex)
        assertEquals(BigDecimal("0.00"), preview100h.estimatedRefundAmount)
    }

    @Test
    fun `pending guide response and awaiting payment have isPaymentTaken false`() {
        val pendingBooking = createBooking(status = BookingStatus.PENDING_GUIDE_RESPONSE)
        val pendingPreview = BookingCancellationCalculator.calculatePreview(pendingBooking)
        assertFalse(pendingPreview.isPaymentTaken)

        val awaitingBooking = createBooking(status = BookingStatus.AWAITING_PAYMENT)
        val awaitingPreview = BookingCancellationCalculator.calculatePreview(awaitingBooking)
        assertFalse(awaitingPreview.isPaymentTaken)
    }

    @Test
    fun `authoritative booking deposit is preserved over estimate`() {
        // Authoritative deposit is 350.0 (even if 20% of 2000 is 400.0)
        val booking = createBooking(depositAmount = 350.0)
        val preview = BookingCancellationCalculator.calculatePreview(
            booking = booking,
            policy = CancellationPolicyType.FLEXIBLE,
            totalPrice = BigDecimal("2000.00"),
            now = booking.date.minusDays(5)
        )
        assertEquals(BigDecimal("350.00"), preview.depositAmount)
        assertEquals(BigDecimal("1650.00"), preview.refundableBase)
        assertEquals(BigDecimal("1650.00"), preview.estimatedRefundAmount)
    }
}
