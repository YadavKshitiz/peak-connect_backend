package com.peakconnect.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.peakconnect.app.core.notifications.ActiveScreenTracker
import com.peakconnect.app.core.notifications.LocalNotificationPoster
import com.peakconnect.app.core.preferences.NotificationPreferences
import com.peakconnect.app.core.session.JwtPayload
import com.peakconnect.app.core.session.TokenStore
import com.peakconnect.app.data.api.PeakConnectApi
import com.peakconnect.app.data.dto.BookingResponse
import com.peakconnect.app.data.local.dao.BookingDao
import com.peakconnect.app.data.local.dao.GuideDao
import com.peakconnect.app.data.local.dao.NotifiedEventDao
import com.peakconnect.app.data.local.dao.UserContentDao
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.Role
import com.peakconnect.app.feature.notifications.reminders.WorkManagerReminderScheduler
import com.peakconnect.app.feature.notifications.sync.BookingSyncWorker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BookingSyncWorkerTest {

    private lateinit var context: Context
    private val tokenStore: TokenStore = mockk(relaxed = true)
    private val api: PeakConnectApi = mockk(relaxed = true)
    private val bookingDao: BookingDao = mockk(relaxed = true)
    private val guideDao: GuideDao = mockk(relaxed = true)
    private val userContentDao: UserContentDao = mockk(relaxed = true)
    private val notifiedEventDao: NotifiedEventDao = mockk(relaxed = true)
    private val notificationPoster: LocalNotificationPoster = mockk(relaxed = true)
    private val notificationPreferences: NotificationPreferences = mockk(relaxed = true)
    private val activeScreenTracker: ActiveScreenTracker = ActiveScreenTracker()
    private lateinit var reminderScheduler: WorkManagerReminderScheduler

    // Helper to generate a test JWT: header.payload.signature
    private fun createJwt(role: String = "ROLE_TREKKER", expSeconds: Long = (System.currentTimeMillis() / 1000) + 3600): String {
        val header = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray())
        val payload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("""{"sub":"user@test.com","userId":"u1","role":"$role","exp":$expSeconds}""".toByteArray())
        return "$header.$payload.fakeSignature"
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        reminderScheduler = WorkManagerReminderScheduler(context)

        coEvery { notificationPreferences.masterEnabled } returns flowOf(true)
        coEvery { notificationPreferences.remindersEnabled } returns flowOf(true)
        coEvery { notifiedEventDao.exists(any()) } returns false
        coEvery { bookingDao.getAllBookings() } returns emptyList()
        coEvery { guideDao.getAllGuideBookings() } returns emptyList()
        coEvery { userContentDao.getAllWaitlistEntries() } returns emptyList()
    }

    private fun buildWorker(): BookingSyncWorker {
        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker {
                return BookingSyncWorker(
                    context = appContext,
                    workerParams = workerParameters,
                    tokenStore = tokenStore,
                    api = api,
                    bookingDao = bookingDao,
                    guideDao = guideDao,
                    userContentDao = userContentDao,
                    notifiedEventDao = notifiedEventDao,
                    notificationPoster = notificationPoster,
                    notificationPreferences = notificationPreferences,
                    reminderScheduler = reminderScheduler,
                    activeScreenTracker = activeScreenTracker
                )
            }
        }

        return TestListenableWorkerBuilder<BookingSyncWorker>(context)
            .setWorkerFactory(factory)
            .build()
    }

    @Test
    fun `logged out user skips sync entirely and returns success`() = runBlocking {
        coEvery { tokenStore.getToken() } returns null

        val worker = buildWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) { api.getMyBookings() }
        coVerify(exactly = 0) { api.getGuideBookings() }
    }

    @Test
    fun `expired token skips sync and posts token expired notification once`() = runBlocking {
        // Expired 1 hour ago
        val expiredToken = createJwt(expSeconds = (System.currentTimeMillis() / 1000) - 3600)
        coEvery { tokenStore.getToken() } returns expiredToken
        coEvery { notifiedEventDao.exists("token_expired_session") } returns false

        val worker = buildWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) {
            notificationPoster.postNotification(
                channelId = any(),
                title = any(),
                body = any(),
                type = "TOKEN_EXPIRED",
                bookingId = null,
                isGuide = false,
                dedupeKey = "token_expired_session",
                notificationId = any()
            )
        }
        coVerify(exactly = 0) { api.getMyBookings() }
    }

    @Test
    fun `disabled master notifications skips sync`() = runBlocking {
        val validToken = createJwt()
        coEvery { tokenStore.getToken() } returns validToken
        coEvery { notificationPreferences.masterEnabled } returns flowOf(false)

        val worker = buildWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) { api.getMyBookings() }
    }

    @Test
    fun `valid trekker session fetches bookings and updates room`() = runBlocking {
        val validToken = createJwt(role = "ROLE_TREKKER")
        coEvery { tokenStore.getToken() } returns validToken
        val bookingDto = BookingResponse(
            id = "b1",
            slotId = "s1",
            activityTitle = "Triund Trek",
            trekkerId = "u1",
            trekkerName = "Alex",
            guideId = null,
            guideName = null,
            status = BookingStatus.PENDING_GUIDE_RESPONSE,
            date = "2026-11-05T08:00:00",
            paymentOrderId = null,
            depositAmount = 50.0,
            refundAmount = null,
            refundPercentage = null
        )
        coEvery { api.getMyBookings() } returns Response.success(listOf(bookingDto))

        val worker = buildWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { api.getMyBookings() }
        coVerify(exactly = 1) { bookingDao.insertBookings(any()) }
        coVerify(exactly = 1) { notificationPreferences.setLastSyncTimestamp(any()) }
    }

    @Test
    fun `valid guide session fetches guide bookings and updates room`() = runBlocking {
        val validToken = createJwt(role = "ROLE_GUIDE")
        coEvery { tokenStore.getToken() } returns validToken
        val bookingDto = BookingResponse(
            id = "gb1",
            slotId = "s1",
            activityTitle = "Triund Trek",
            trekkerId = "u1",
            trekkerName = "Alex",
            guideId = "g1",
            guideName = "Tenzing",
            status = BookingStatus.CONFIRMED,
            date = "2026-11-05T08:00:00",
            paymentOrderId = null,
            depositAmount = 50.0,
            refundAmount = null,
            refundPercentage = null
        )
        coEvery { api.getGuideBookings() } returns Response.success(listOf(bookingDto))

        val worker = buildWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { api.getGuideBookings() }
        coVerify(exactly = 1) { guideDao.insertGuideBookings(any()) }
    }
}
