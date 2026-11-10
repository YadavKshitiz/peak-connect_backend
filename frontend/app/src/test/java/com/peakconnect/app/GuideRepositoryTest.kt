package com.peakconnect.app

import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.network.WakeUpRetryHelper
import com.peakconnect.app.data.api.PeakConnectApi
import com.peakconnect.app.data.dto.GuideProfileUpdateRequest
import com.peakconnect.app.data.dto.GuideResponse
import com.peakconnect.app.data.local.dao.GuideDao
import com.peakconnect.app.data.local.entity.GuideProfileEntity
import com.peakconnect.app.data.repository.GuideRepositoryImpl
import com.peakconnect.app.domain.model.ExperienceLevel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class GuideRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private val api: PeakConnectApi = mockk()
    private val guideDao: GuideDao = mockk(relaxed = true)
    private val wakeUpRetryHelper = WakeUpRetryHelper(healthRepository = mockk(relaxed = true))

    private val repository = GuideRepositoryImpl(
        api = api,
        guideDao = guideDao,
        wakeUpRetryHelper = wakeUpRetryHelper,
        ioDispatcher = testDispatcher
    )

    @Test
    fun `getProfile returns cached profile when available`() = runTest(testDispatcher) {
        val cachedEntity = GuideProfileEntity(
            id = "g_123",
            name = "Sherpa Tenzing",
            email = "tenzing@peakconnect.app",
            skills = listOf("High Altitude Trekking"),
            languages = listOf("English", "Nepali"),
            location = "Manali",
            latitude = 32.2396,
            longitude = 77.1887,
            experienceLevel = ExperienceLevel.EXPERT,
            isVerified = true,
            lastUpdated = 1000L
        )
        coEvery { guideDao.getGuideProfile() } returns cachedEntity

        val result = repository.getProfile()

        assertTrue(result is ApiResult.Success)
        val profile = (result as ApiResult.Success).data
        assertEquals("g_123", profile.id)
        assertEquals("Sherpa Tenzing", profile.name)
        coVerify(exactly = 0) { api.getMyGuideProfile() }
    }

    @Test
    fun `refreshProfile fetches from API and inserts into DAO`() = runTest(testDispatcher) {
        val dto = GuideResponse(
            id = "g_456",
            name = "Edmund Hillary",
            email = "hillary@peakconnect.app",
            skills = listOf("Alpine", "Ice Climbing"),
            languages = listOf("English"),
            location = "Leh",
            latitude = 34.1526,
            longitude = 77.5771,
            experienceLevel = ExperienceLevel.EXPERT,
            isVerified = true
        )
        coEvery { api.getMyGuideProfile() } returns Response.success(dto)

        val result = repository.refreshProfile()

        assertTrue(result is ApiResult.Success)
        coVerify { guideDao.insertOrUpdateProfile(any()) }
        val lastUpdated = repository.lastUpdatedProfileFlow.first()
        assertNotNull(lastUpdated)
    }

    @Test
    fun `updateProfile sends request to API and saves to DAO`() = runTest(testDispatcher) {
        val request = GuideProfileUpdateRequest(
            skills = listOf("Rock Climbing"),
            languages = listOf("Hindi", "English"),
            location = "Rishikesh",
            latitude = 30.0869,
            longitude = 78.2676,
            experienceLevel = ExperienceLevel.INTERMEDIATE
        )
        val dto = GuideResponse(
            id = "g_789",
            name = "Bachendri Pal",
            email = "bachendri@peakconnect.app",
            skills = listOf("Rock Climbing"),
            languages = listOf("Hindi", "English"),
            location = "Rishikesh",
            latitude = 30.0869,
            longitude = 78.2676,
            experienceLevel = ExperienceLevel.INTERMEDIATE,
            isVerified = true
        )
        coEvery { api.updateMyGuideProfile(request) } returns Response.success(dto)

        val result = repository.updateProfile(request)

        assertTrue(result is ApiResult.Success)
        assertEquals("g_789", (result as ApiResult.Success).data.id)
        coVerify { guideDao.insertOrUpdateProfile(any()) }
    }
}
