package com.peakconnect.app

import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.network.WakeUpRetryHelper
import com.peakconnect.app.data.api.PeakConnectApi
import com.peakconnect.app.data.local.dao.ActivityDao
import com.peakconnect.app.data.local.dao.BookingDao
import com.peakconnect.app.data.local.dao.GuideDao
import com.peakconnect.app.data.local.dao.UserContentDao
import com.peakconnect.app.data.repository.ActivityRepositoryImpl
import com.peakconnect.app.data.repository.BookingRepositoryImpl
import com.peakconnect.app.domain.model.Activity
import com.peakconnect.app.feature.trekker.waitlist.WaitlistPromotionDetector
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class WakeUpRetryAuditTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val api: PeakConnectApi = mockk(relaxed = true)
    private val wakeUpRetryHelper: WakeUpRetryHelper = mockk(relaxed = true)

    @Test
    fun `user-triggered read refreshMyBookings uses withWakeUpRetry`() = runTest(testDispatcher) {
        val bookingDao: BookingDao = mockk(relaxed = true)
        val guideDao: GuideDao = mockk(relaxed = true)
        val userContentDao: UserContentDao = mockk(relaxed = true)
        val waitlistPromotionDetector: WaitlistPromotionDetector = mockk(relaxed = true)

        coEvery { api.getMyBookings() } returns Response.success(emptyList())
        coEvery {
            wakeUpRetryHelper.withWakeUpRetry<Unit>(
                pollIntervalMs = any(),
                maxDurationMs = any(),
                block = any()
            )
        } coAnswers {
            val block = thirdArg<suspend () -> ApiResult<Unit>>()
            block()
        }

        val repository = BookingRepositoryImpl(
            api = api,
            bookingDao = bookingDao,
            guideDao = guideDao,
            userContentDao = userContentDao,
            waitlistPromotionDetector = waitlistPromotionDetector,
            wakeUpRetryHelper = wakeUpRetryHelper,
            ioDispatcher = testDispatcher
        )

        val result = repository.refreshMyBookings()

        assertTrue(result is ApiResult.Success)
        coVerify(exactly = 1) {
            wakeUpRetryHelper.withWakeUpRetry<Unit>(any(), any(), any())
        }
    }

    @Test
    fun `booking creation confirmBooking does NOT use withWakeUpRetry`() = runTest(testDispatcher) {
        val bookingDao: BookingDao = mockk(relaxed = true)
        val guideDao: GuideDao = mockk(relaxed = true)
        val userContentDao: UserContentDao = mockk(relaxed = true)
        val waitlistPromotionDetector: WaitlistPromotionDetector = mockk(relaxed = true)

        coEvery { api.confirmBooking(any()) } returns Response.error(400, "{}".toResponseBody())

        val repository = BookingRepositoryImpl(
            api = api,
            bookingDao = bookingDao,
            guideDao = guideDao,
            userContentDao = userContentDao,
            waitlistPromotionDetector = waitlistPromotionDetector,
            wakeUpRetryHelper = wakeUpRetryHelper,
            ioDispatcher = testDispatcher
        )

        repository.confirmBooking("slot-1", "guide-1")

        // WakeUpRetry must NOT be applied to booking-creating POST calls
        coVerify(exactly = 0) {
            wakeUpRetryHelper.withWakeUpRetry<Any>(any(), any(), any())
        }
    }

    @Test
    fun `user-triggered read refreshActivities uses withWakeUpRetry`() = runTest(testDispatcher) {
        val activityDao: ActivityDao = mockk(relaxed = true)
        val userContentDao: UserContentDao = mockk(relaxed = true)

        coEvery { api.getActivities(any(), any()) } returns Response.success(emptyList())
        coEvery {
            wakeUpRetryHelper.withWakeUpRetry<Unit>(
                pollIntervalMs = any(),
                maxDurationMs = any(),
                block = any()
            )
        } coAnswers {
            val block = thirdArg<suspend () -> ApiResult<Unit>>()
            block()
        }

        val repository = ActivityRepositoryImpl(
            api = api,
            activityDao = activityDao,
            userContentDao = userContentDao,
            wakeUpRetryHelper = wakeUpRetryHelper,
            ioDispatcher = testDispatcher
        )

        val result = repository.refreshActivities()

        assertTrue(result is ApiResult.Success)
        coVerify(exactly = 1) {
            wakeUpRetryHelper.withWakeUpRetry<Unit>(any(), any(), any())
        }
    }

    @Test
    fun `user-triggered read getActivity uses withWakeUpRetry`() = runTest(testDispatcher) {
        val activityDao: ActivityDao = mockk(relaxed = true)
        val userContentDao: UserContentDao = mockk(relaxed = true)

        coEvery {
            wakeUpRetryHelper.withWakeUpRetry<Activity>(
                pollIntervalMs = any(),
                maxDurationMs = any(),
                block = any()
            )
        } coAnswers {
            val block = thirdArg<suspend () -> ApiResult<Activity>>()
            block()
        }

        val repository = ActivityRepositoryImpl(
            api = api,
            activityDao = activityDao,
            userContentDao = userContentDao,
            wakeUpRetryHelper = wakeUpRetryHelper,
            ioDispatcher = testDispatcher
        )

        repository.getActivity("act-1")

        coVerify(exactly = 1) {
            wakeUpRetryHelper.withWakeUpRetry<Activity>(any(), any(), any())
        }
    }
}
