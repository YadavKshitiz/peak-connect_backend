package com.peakconnect.app

import com.peakconnect.app.core.network.BookingStatusSerializer
import com.peakconnect.app.core.network.CancellationPolicyTypeSerializer
import com.peakconnect.app.core.network.DifficultyLevelSerializer
import com.peakconnect.app.core.network.ExperienceLevelSerializer
import com.peakconnect.app.core.network.RoleSerializer
import com.peakconnect.app.core.network.SafeRiskLevelSerializer
import com.peakconnect.app.core.network.SeasonSerializer
import com.peakconnect.app.core.network.WaitlistStatusSerializer
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.CancellationPolicyType
import com.peakconnect.app.domain.model.DifficultyLevel
import com.peakconnect.app.domain.model.ExperienceLevel
import com.peakconnect.app.domain.model.RiskLevel
import com.peakconnect.app.domain.model.Role
import com.peakconnect.app.domain.model.Season
import com.peakconnect.app.domain.model.WaitlistStatus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class SafeEnumSerializerTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun `SafeRiskLevelSerializer parses valid values and falls back to UNKNOWN`() {
        assertEquals(RiskLevel.LOW, json.decodeFromString(SafeRiskLevelSerializer, "\"LOW\""))
        assertEquals(RiskLevel.MODERATE, json.decodeFromString(SafeRiskLevelSerializer, "\"MODERATE\""))
        assertEquals(RiskLevel.HIGH, json.decodeFromString(SafeRiskLevelSerializer, "\"HIGH\""))
        assertEquals(RiskLevel.UNKNOWN, json.decodeFromString(SafeRiskLevelSerializer, "\"UNKNOWN\""))
        // Fallback for unknown / weird strings
        assertEquals(RiskLevel.UNKNOWN, json.decodeFromString(SafeRiskLevelSerializer, "\"EXTREME_DANGER\""))
        assertEquals(RiskLevel.UNKNOWN, json.decodeFromString(SafeRiskLevelSerializer, "\"\""))
    }

    @Test
    fun `BookingStatusSerializer parses valid values and falls back to PENDING`() {
        assertEquals(BookingStatus.CONFIRMED, json.decodeFromString(BookingStatusSerializer, "\"CONFIRMED\""))
        assertEquals(BookingStatus.PENDING_GUIDE_RESPONSE, json.decodeFromString(BookingStatusSerializer, "\"PENDING_GUIDE_RESPONSE\""))
        assertEquals(BookingStatus.REMATCH_FAILED, json.decodeFromString(BookingStatusSerializer, "\"REMATCH_FAILED\""))
        assertEquals(BookingStatus.PENDING, json.decodeFromString(BookingStatusSerializer, "\"NON_EXISTENT_STATUS\""))
    }

    @Test
    fun `DifficultyLevelSerializer parses EXTREME and fallback`() {
        assertEquals(DifficultyLevel.EXTREME, json.decodeFromString(DifficultyLevelSerializer, "\"EXTREME\""))
        assertEquals(DifficultyLevel.CHALLENGING, json.decodeFromString(DifficultyLevelSerializer, "\"CHALLENGING\""))
        assertEquals(DifficultyLevel.MODERATE, json.decodeFromString(DifficultyLevelSerializer, "\"INVALID\""))
    }

    @Test
    fun `Other serializers handle valid values and fallbacks gracefully`() {
        assertEquals(Season.PEAK, json.decodeFromString(SeasonSerializer, "\"PEAK\""))
        assertEquals(Season.LOW, json.decodeFromString(SeasonSerializer, "\"UNKNOWN_SEASON\""))

        assertEquals(ExperienceLevel.EXPERT, json.decodeFromString(ExperienceLevelSerializer, "\"EXPERT\""))
        assertEquals(ExperienceLevel.BEGINNER, json.decodeFromString(ExperienceLevelSerializer, "\"UNKNOWN_EXP\""))

        assertEquals(CancellationPolicyType.STRICT, json.decodeFromString(CancellationPolicyTypeSerializer, "\"STRICT\""))
        assertEquals(CancellationPolicyType.MODERATE, json.decodeFromString(CancellationPolicyTypeSerializer, "\"UNKNOWN_POLICY\""))

        assertEquals(Role.GUIDE, json.decodeFromString(RoleSerializer, "\"GUIDE\""))
        assertEquals(Role.TREKKER, json.decodeFromString(RoleSerializer, "\"UNKNOWN_ROLE\""))

        assertEquals(WaitlistStatus.PROMOTED, json.decodeFromString(WaitlistStatusSerializer, "\"PROMOTED\""))
        assertEquals(WaitlistStatus.WAITING, json.decodeFromString(WaitlistStatusSerializer, "\"UNKNOWN_STATUS\""))
    }
}
