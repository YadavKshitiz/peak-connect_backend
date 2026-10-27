package com.peakconnect.app

import com.peakconnect.app.domain.model.CancellationPolicyInfo
import com.peakconnect.app.domain.model.CancellationPolicyType
import org.junit.Assert.assertEquals
import org.junit.Test

class CancellationPolicyInfoTest {

    @Test
    fun `deposit percentage matches backend 20 percent`() {
        assertEquals(0.20, CancellationPolicyInfo.DEPOSIT_PERCENTAGE, 0.001)
        assertEquals(20, CancellationPolicyInfo.DEPOSIT_PERCENTAGE_INT)
    }

    @Test
    fun `flexible policy calculates refund according to backend hours`() {
        // >= 48 hours -> 100%
        assertEquals(1.0, CancellationPolicyInfo.calculateRefundPercentage(CancellationPolicyType.FLEXIBLE, 48), 0.001)
        assertEquals(1.0, CancellationPolicyInfo.calculateRefundPercentage(CancellationPolicyType.FLEXIBLE, 72), 0.001)

        // 12 to 47 hours -> 50%
        assertEquals(0.5, CancellationPolicyInfo.calculateRefundPercentage(CancellationPolicyType.FLEXIBLE, 12), 0.001)
        assertEquals(0.5, CancellationPolicyInfo.calculateRefundPercentage(CancellationPolicyType.FLEXIBLE, 24), 0.001)

        // < 12 hours -> 0%
        assertEquals(0.0, CancellationPolicyInfo.calculateRefundPercentage(CancellationPolicyType.FLEXIBLE, 11), 0.001)
        assertEquals(0.0, CancellationPolicyInfo.calculateRefundPercentage(CancellationPolicyType.FLEXIBLE, 0), 0.001)
    }

    @Test
    fun `moderate policy calculates refund according to backend hours`() {
        // >= 168 hours (7 days) -> 100%
        assertEquals(1.0, CancellationPolicyInfo.calculateRefundPercentage(CancellationPolicyType.MODERATE, 168), 0.001)
        assertEquals(1.0, CancellationPolicyInfo.calculateRefundPercentage(CancellationPolicyType.MODERATE, 200), 0.001)

        // 48 to 167 hours (2 to 7 days) -> 50%
        assertEquals(0.5, CancellationPolicyInfo.calculateRefundPercentage(CancellationPolicyType.MODERATE, 48), 0.001)
        assertEquals(0.5, CancellationPolicyInfo.calculateRefundPercentage(CancellationPolicyType.MODERATE, 100), 0.001)

        // < 48 hours -> 0%
        assertEquals(0.0, CancellationPolicyInfo.calculateRefundPercentage(CancellationPolicyType.MODERATE, 47), 0.001)
        assertEquals(0.0, CancellationPolicyInfo.calculateRefundPercentage(CancellationPolicyType.MODERATE, 5), 0.001)
    }

    @Test
    fun `strict policy calculates refund according to backend hours`() {
        // >= 336 hours (14 days) -> 50%
        assertEquals(0.5, CancellationPolicyInfo.calculateRefundPercentage(CancellationPolicyType.STRICT, 336), 0.001)
        assertEquals(0.5, CancellationPolicyInfo.calculateRefundPercentage(CancellationPolicyType.STRICT, 400), 0.001)

        // < 336 hours -> 0%
        assertEquals(0.0, CancellationPolicyInfo.calculateRefundPercentage(CancellationPolicyType.STRICT, 335), 0.001)
        assertEquals(0.0, CancellationPolicyInfo.calculateRefundPercentage(CancellationPolicyType.STRICT, 0), 0.001)
    }

    @Test
    fun `tiers contain expected count and refund values`() {
        val flexibleTiers = CancellationPolicyInfo.getTiers(CancellationPolicyType.FLEXIBLE)
        assertEquals(3, flexibleTiers.size)
        assertEquals(listOf(100, 50, 0), flexibleTiers.map { it.refundPercentage })

        val moderateTiers = CancellationPolicyInfo.getTiers(CancellationPolicyType.MODERATE)
        assertEquals(3, moderateTiers.size)
        assertEquals(listOf(100, 50, 0), moderateTiers.map { it.refundPercentage })

        val strictTiers = CancellationPolicyInfo.getTiers(CancellationPolicyType.STRICT)
        assertEquals(2, strictTiers.size)
        assertEquals(listOf(50, 0), strictTiers.map { it.refundPercentage })
    }
}
