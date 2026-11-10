package com.peakconnect.app

import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.network.WakeUpRetryHelper
import com.peakconnect.app.data.api.PeakConnectApi
import com.peakconnect.app.data.dto.BookingResponse
import com.peakconnect.app.data.dto.WaitlistMessageResponse
import com.peakconnect.app.data.local.dao.BookingDao
import com.peakconnect.app.data.local.dao.GuideDao
import com.peakconnect.app.data.local.dao.UserContentDao
import com.peakconnect.app.data.repository.BookingRepositoryImpl
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.WaitlistStatus
import com.peakconnect.app.feature.trekker.waitlist.WaitlistPromotionDetector
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
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class BookingRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private val api: PeakConnectApi = mockk(relaxed = true)
    private val bookingDao: BookingDao = mockk(relaxed = true)
    private val guideDao: GuideDao = mockk(relaxed = true)
    private val userContentDao: UserContentDao = mockk(relaxed = true)
    private val waitlistPromotionDetector: WaitlistPromotionDetector = mockk(relaxed = true)
    private val wakeUpRetryHelper = WakeUpRetryHelper(healthRepository = mockk(relaxed = true))

    private val repository = BookingRepositoryImpl(
        api = api,
        bookingDao = bookingDao,
        guideDao = guideDao,
        userContentDao = userContentDao,
        waitlistPromotionDetector = waitlistPromotionDetector,
        wakeUpRetryHelper = wakeUpRetryHelper,
        ioDispatcher = testDispatcher
    )

    @Test
    fun `refreshMyBookings syncs bookings with DAO and updates timestamp`() = runTest(testDispatcher) {
        val dtos = listOf(
            BookingResponse(
                id = "b_1",
                slotId = "s_1",
                activityTitle = "Triund Trek",
                trekkerId = "t_1",
                trekkerName = "Trekker 1",
                guideId = null,
                guideName = null,
                status = BookingStatus.CONFIRMED,
                date = "2026-11-20T09:00:00"
            )
        )
        coEvery { api.getMyBookings() } returns Response.success(dtos)
        coEvery { bookingDao.getAllBookings() } returns emptyList()

        val result = repository.refreshMyBookings()

        assertTrue(result is ApiResult.Success)
        val lastUpdated = repository.lastUpdatedFlow.first()
        assertNotNull(lastUpdated)
    }

    @Test
    fun `joinWaitlist sends confirm DTO and inserts entry`() = runTest(testDispatcher) {
        coEvery { api.joinWaitlist(any()) } returns Response.success(WaitlistMessageResponse("Added to waitlist"))
        coEvery { userContentDao.getWaitlistEntry("slot_10") } returns null

        val result = repository.joinWaitlist(
            slotId = "slot_10",
            guideId = "guide_1",
            activityTitle = "Kedarkantha",
            slotDate = LocalDateTime.of(2026, 12, 1, 10, 0)
        )

        assertTrue(result is ApiResult.Success)
        coVerify { userContentDao.insertWaitlistEntry(any()) }
    }
}
