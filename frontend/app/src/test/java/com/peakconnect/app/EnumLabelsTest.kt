package com.peakconnect.app

import com.peakconnect.app.core.ui.labelRes
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.Difficulty
import com.peakconnect.app.domain.model.ExperienceLevel
import com.peakconnect.app.domain.model.RiskLevel
import com.peakconnect.app.domain.model.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnumLabelsTest {

    @Test
    fun bookingStatus_mapsToExpectedResource() {
        assertEquals(R.string.status_pending_guide_response, BookingStatus.PENDING_GUIDE_RESPONSE.labelRes())
        assertEquals(R.string.status_awaiting_payment, BookingStatus.AWAITING_PAYMENT.labelRes())
        assertEquals(R.string.status_confirmed, BookingStatus.CONFIRMED.labelRes())
        assertEquals(R.string.status_cancelled, BookingStatus.CANCELLED.labelRes())
        assertEquals(R.string.status_no_show, BookingStatus.NO_SHOW.labelRes())
        assertEquals(R.string.status_rematch_failed, BookingStatus.REMATCH_FAILED.labelRes())
        assertEquals(R.string.status_pending, BookingStatus.PENDING.labelRes())
    }

    @Test
    fun riskLevel_mapsToExpectedResource() {
        assertEquals(R.string.risk_low, RiskLevel.LOW.labelRes())
        assertEquals(R.string.risk_moderate, RiskLevel.MODERATE.labelRes())
        assertEquals(R.string.risk_high, RiskLevel.HIGH.labelRes())
        assertEquals(R.string.risk_unknown, RiskLevel.UNKNOWN.labelRes())
    }

    @Test
    fun difficulty_mapsToExpectedResource() {
        for (difficulty in Difficulty.values()) {
            assertTrue(difficulty.labelRes() > 0)
        }
    }

    @Test
    fun season_mapsToExpectedResource() {
        for (season in Season.values()) {
            assertTrue(season.labelRes() > 0)
        }
    }

    @Test
    fun experienceLevel_mapsToExpectedResource() {
        for (level in ExperienceLevel.values()) {
            assertTrue(level.labelRes() > 0)
        }
    }
}
