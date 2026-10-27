package com.peakconnect.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.peakconnect.app.core.network.WakeUpRetryHelper
import com.peakconnect.app.core.session.SessionManager
import com.peakconnect.app.core.session.SessionState
import com.peakconnect.app.core.session.TokenStore
import com.peakconnect.app.data.api.PeakConnectApi
import com.peakconnect.app.data.local.PeakConnectDatabase
import com.peakconnect.app.data.local.entity.BookingEntity
import com.peakconnect.app.data.local.entity.FavouriteEntity
import com.peakconnect.app.data.local.entity.GuideBookingEntity
import com.peakconnect.app.data.local.entity.GuideProfileEntity
import com.peakconnect.app.data.local.entity.NotificationEntity
import com.peakconnect.app.data.local.entity.NotifiedEventEntity
import com.peakconnect.app.data.local.entity.WaitlistEntryEntity
import com.peakconnect.app.data.repository.AuthRepositoryImpl
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.ExperienceLevel
import com.peakconnect.app.domain.model.Role
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthRepositoryLogoutTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var db: PeakConnectDatabase
    private lateinit var tokenStore: FakeTokenStore
    private lateinit var sessionManager: SessionManager
    private lateinit var repository: AuthRepositoryImpl
    private val api: PeakConnectApi = mockk(relaxed = true)
    private val wakeUpRetryHelper: WakeUpRetryHelper = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PeakConnectDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        tokenStore = FakeTokenStore("initial_token")
        sessionManager = SessionManager(tokenStore)
        repository = AuthRepositoryImpl(
            api = api,
            sessionManager = sessionManager,
            userContentDao = db.userContentDao(),
            bookingDao = db.bookingDao(),
            guideDao = db.guideDao(),
            notifiedEventDao = db.notifiedEventDao(),
            wakeUpRetryHelper = wakeUpRetryHelper,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `logout clears user specific Room tables and keeps favourites intact`() = runTest(testDispatcher) {
        val now = LocalDateTime.now()

        // 1. Seed tables with user data
        db.bookingDao().insertBookings(
            listOf(
                BookingEntity(
                    id = "b1",
                    slotId = "s1",
                    activityTitle = "Everest Base Camp",
                    trekkerId = "u1",
                    trekkerName = "Alex",
                    guideId = "g1",
                    guideName = "Tenzing",
                    status = BookingStatus.CONFIRMED,
                    date = now,
                    paymentOrderId = "p1",
                    depositAmount = 100.0,
                    refundAmount = null,
                    refundPercentage = null,
                    statusSinceMillis = 1000L,
                    lastSyncMillis = 1000L
                )
            )
        )

        db.guideDao().insertGuideBookings(
            listOf(
                GuideBookingEntity(
                    id = "gb1",
                    slotId = "s1",
                    activityTitle = "Everest Base Camp",
                    trekkerId = "u1",
                    trekkerName = "Alex",
                    guideId = "g1",
                    guideName = "Tenzing",
                    status = BookingStatus.CONFIRMED,
                    date = now,
                    paymentOrderId = "p1",
                    depositAmount = 100.0,
                    refundAmount = null,
                    refundPercentage = null,
                    statusSinceMillis = 1000L,
                    lastSyncMillis = 1000L
                )
            )
        )

        db.guideDao().insertOrUpdateProfile(
            GuideProfileEntity(
                id = "g1",
                name = "Tenzing Norgay",
                email = "tenzing@example.com",
                skills = listOf("High Altitude", "Navigation"),
                languages = listOf("English", "Nepali"),
                location = "Namche",
                latitude = 27.8,
                longitude = 86.7,
                experienceLevel = ExperienceLevel.EXPERT,
                isVerified = true,
                lastUpdated = 1000L
            )
        )

        db.userContentDao().insertNotification(
            NotificationEntity(
                id = "n1",
                type = "BOOKING_UPDATE",
                title = "Booking Confirmed",
                message = "Your trek is set",
                createdAt = 1000L
            )
        )

        db.notifiedEventDao().insert(
            NotifiedEventEntity(key = "event_1", notifiedAt = 1000L)
        )

        db.userContentDao().insertWaitlistEntry(
            WaitlistEntryEntity(
                slotId = "slot_w1",
                activityTitle = "Annapurna Circuit",
                slotDate = now,
                joinedAt = 1000L
            )
        )

        // Seed Favourite (Must NOT be cleared!)
        db.userContentDao().insertFavourite(
            FavouriteEntity(activityId = "act_fav_1", addedAt = 1000L)
        )

        // Verify pre-conditions
        assertEquals(1, db.bookingDao().getAllBookings().size)
        assertEquals(1, db.guideDao().getAllGuideBookings().size)
        assertEquals("Tenzing Norgay", db.guideDao().getGuideProfile()?.name)
        assertEquals(1, db.userContentDao().getNotificationsFlow().first().size)
        assertTrue(db.notifiedEventDao().exists("event_1"))
        assertEquals(1, db.userContentDao().getWaitlistEntriesFlow().first().size)
        assertEquals(1, db.userContentDao().getFavouritesFlow().first().size)

        // 2. Perform Logout
        repository.logout()

        // 3. Verify user specific data cleared
        assertEquals(0, db.bookingDao().getAllBookings().size)
        assertEquals(0, db.guideDao().getAllGuideBookings().size)
        assertNull(db.guideDao().getGuideProfile())
        assertEquals(0, db.userContentDao().getNotificationsFlow().first().size)
        assertFalse(db.notifiedEventDao().exists("event_1"))
        assertEquals(0, db.userContentDao().getWaitlistEntriesFlow().first().size)

        // 4. Verify favourites STAY intact
        val favourites = db.userContentDao().getFavouritesFlow().first()
        assertEquals(1, favourites.size)
        assertEquals("act_fav_1", favourites[0].activityId)

        // 5. Verify token cleared and session LoggedOut
        assertNull(tokenStore.getToken())
        assertEquals(SessionState.LoggedOut, sessionManager.sessionState.value)
    }

    private class FakeTokenStore(private var token: String? = null) : TokenStore {
        override suspend fun getToken(): String? = token
        override fun getCachedToken(): String? = token
        override fun getTokenFlow(): kotlinx.coroutines.flow.Flow<String?> = kotlinx.coroutines.flow.flowOf(token)
        override suspend fun saveToken(token: String) {
            this.token = token
        }
        override suspend fun clearToken() {
            this.token = null
        }
    }
}
