package com.peakconnect.app

import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.data.local.dao.ActivityDao
import com.peakconnect.app.data.local.dao.UserContentDao
import com.peakconnect.app.data.local.entity.ActivityEntity
import com.peakconnect.app.data.local.entity.BookingEntity
import com.peakconnect.app.data.local.entity.WaitlistEntryEntity
import com.peakconnect.app.data.repository.BookingRepository
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.CancellationPolicyType
import com.peakconnect.app.domain.model.DifficultyLevel
import com.peakconnect.app.domain.model.WaitlistStatus
import com.peakconnect.app.feature.trekker.booking.BookingListViewModel
import com.peakconnect.app.feature.trekker.booking.BookingTab
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BookingListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val bookingRepository: BookingRepository = mockk(relaxed = true)
    private val activityDao: ActivityDao = mockk(relaxed = true)
    private val userContentDao: UserContentDao = mockk(relaxed = true)

    private val bookingsFlow = MutableStateFlow<List<BookingEntity>>(emptyList())
    private val activitiesFlow = MutableStateFlow<List<ActivityEntity>>(emptyList())
    private val waitlistFlow = MutableStateFlow<List<WaitlistEntryEntity>>(emptyList())

    private val fixedNow = LocalDateTime.of(2026, 11, 1, 12, 0)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { bookingRepository.getMyBookingsFlow() } returns bookingsFlow
        every { activityDao.getAllActivitiesFlow() } returns activitiesFlow
        every { userContentDao.getWaitlistEntriesFlow() } returns waitlistFlow
        coEvery { bookingRepository.refreshMyBookings() } returns ApiResult.Success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createBooking(
        id: String,
        status: BookingStatus,
        date: LocalDateTime,
        guideId: String? = "guide-1",
        guideName: String? = "Tenzing",
        title: String = "Hampta Pass"
    ): BookingEntity {
        return BookingEntity(
            id = id,
            slotId = "slot-$id",
            activityTitle = title,
            trekkerId = "trekker-1",
            trekkerName = "Alex",
            guideId = guideId,
            guideName = guideName,
            status = status,
            date = date,
            paymentOrderId = null,
            depositAmount = 500.0,
            refundAmount = null,
            refundPercentage = null,
            statusSinceMillis = 1000L,
            lastSyncMillis = 1000L
        )
    }

    @Test
    fun `tab grouping categorizes every booking status correctly`() = runTest {
        val testBookings = listOf(
            // Active
            createBooking("b1", BookingStatus.PENDING_GUIDE_RESPONSE, fixedNow.plusDays(2)),
            createBooking("b2", BookingStatus.AWAITING_PAYMENT, fixedNow.plusDays(3)),
            // Upcoming (CONFIRMED in future)
            createBooking("b3", BookingStatus.CONFIRMED, fixedNow.plusDays(5)),
            createBooking("b4", BookingStatus.CONFIRMED, fixedNow.plusDays(1)),
            // Past (CONFIRMED in past and NO_SHOW)
            createBooking("b5", BookingStatus.CONFIRMED, fixedNow.minusDays(2)),
            createBooking("b6", BookingStatus.NO_SHOW, fixedNow.minusDays(4)),
            // Cancelled (CANCELLED and REMATCH_FAILED)
            createBooking("b7", BookingStatus.CANCELLED, fixedNow.plusDays(10)),
            createBooking("b8", BookingStatus.REMATCH_FAILED, fixedNow.plusDays(7), guideId = null, guideName = null)
        )

        val viewModel = BookingListViewModel(bookingRepository, activityDao, userContentDao)
        viewModel.nowSupplier = { fixedNow }

        bookingsFlow.value = testBookings
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Active tab
        assertEquals(2, state.activeBookings.size)
        assertEquals(2, state.activeCount)
        assertTrue(state.activeBookings.any { it.booking.id == "b1" })
        assertTrue(state.activeBookings.any { it.booking.id == "b2" })

        // Upcoming tab (sorted ascending)
        assertEquals(2, state.upcomingBookings.size)
        assertEquals("b4", state.upcomingBookings[0].booking.id) // earlier date first
        assertEquals("b3", state.upcomingBookings[1].booking.id)

        // Past tab (sorted descending)
        assertEquals(2, state.pastBookings.size)
        assertEquals("b5", state.pastBookings[0].booking.id) // minus 2 days before minus 4 days
        assertEquals("b6", state.pastBookings[1].booking.id)

        // Cancelled tab (sorted descending)
        assertEquals(2, state.cancelledBookings.size)
        assertEquals("b7", state.cancelledBookings[0].booking.id) // plus 10 days before plus 7 days
        assertEquals("b8", state.cancelledBookings[1].booking.id)
    }

    @Test
    fun `null guide booking is safely handled and exposed`() = runTest {
        val nullGuideBooking = createBooking(
            id = "null-guide-b",
            status = BookingStatus.REMATCH_FAILED,
            date = fixedNow.plusDays(1),
            guideId = null,
            guideName = null
        )

        val viewModel = BookingListViewModel(bookingRepository, activityDao, userContentDao)
        viewModel.nowSupplier = { fixedNow }

        bookingsFlow.value = listOf(nullGuideBooking)
        advanceUntilIdle()

        val item = viewModel.uiState.value.cancelledBookings.first()
        assertNull(item.booking.guideId)
        assertNull(item.booking.guideName)
    }

    @Test
    fun `selectTab updates current list`() = runTest {
        val testBookings = listOf(
            createBooking("b1", BookingStatus.PENDING_GUIDE_RESPONSE, fixedNow.plusDays(1)),
            createBooking("b2", BookingStatus.CONFIRMED, fixedNow.plusDays(2))
        )

        val viewModel = BookingListViewModel(bookingRepository, activityDao, userContentDao)
        viewModel.nowSupplier = { fixedNow }

        bookingsFlow.value = testBookings
        advanceUntilIdle()

        // Default tab is ACTIVE
        assertEquals(BookingTab.ACTIVE, viewModel.uiState.value.selectedTab)
        assertEquals(1, viewModel.uiState.value.currentList.size)
        assertEquals("b1", viewModel.uiState.value.currentList.first().booking.id)

        // Switch to UPCOMING
        viewModel.selectTab(BookingTab.UPCOMING)
        assertEquals(BookingTab.UPCOMING, viewModel.uiState.value.selectedTab)
        assertEquals(1, viewModel.uiState.value.currentList.size)
        assertEquals("b2", viewModel.uiState.value.currentList.first().booking.id)
    }

    @Test
    fun `refresh triggers booking repository refresh`() = runTest {
        val viewModel = BookingListViewModel(bookingRepository, activityDao, userContentDao)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        coVerify(atLeast = 1) { bookingRepository.refreshMyBookings() }
    }

    @Test
    fun `waitlist entries are loaded into state and tab exposes them`() = runTest {
        val viewModel = BookingListViewModel(bookingRepository, activityDao, userContentDao)
        advanceUntilIdle()

        val waitlistEntry = WaitlistEntryEntity(
            slotId = "slot-w1",
            activityTitle = "Kedarkantha Trek",
            slotDate = fixedNow.plusDays(10),
            joinedAt = 1000L,
            status = WaitlistStatus.WAITING,
            guideName = "Nawang"
        )
        waitlistFlow.value = listOf(waitlistEntry)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.waitlistEntries.size)
        assertEquals("slot-w1", state.waitlistEntries[0].slotId)
        assertEquals(WaitlistStatus.WAITING, state.waitlistEntries[0].status)
    }

    @Test
    fun `promoted waitlist entry triggers promoted banner with view and dismiss actions`() = runTest {
        val viewModel = BookingListViewModel(bookingRepository, activityDao, userContentDao)
        advanceUntilIdle()

        val promotedEntry = WaitlistEntryEntity(
            slotId = "slot-w2",
            activityTitle = "Kedarkantha Trek",
            slotDate = fixedNow.plusDays(10),
            joinedAt = 1000L,
            status = WaitlistStatus.PROMOTED,
            bookingId = "booking-promo-1",
            guideName = "Dorje"
        )
        waitlistFlow.value = listOf(promotedEntry)
        advanceUntilIdle()

        val banner = viewModel.uiState.value.promotedBanner
        assertNotNull(banner)
        assertEquals("booking-promo-1", banner?.bookingId)
        assertEquals("Dorje", banner?.guideName)

        // Dismiss banner
        viewModel.dismissPromotedBanner("booking-promo-1")
        assertNull(viewModel.uiState.value.promotedBanner)
    }

    @Test
    fun `removeFromWaitlist calls userContentDao deleteWaitlistEntry`() = runTest {
        val viewModel = BookingListViewModel(bookingRepository, activityDao, userContentDao)
        advanceUntilIdle()

        viewModel.removeFromWaitlist("slot-to-delete")
        advanceUntilIdle()

        coVerify { userContentDao.deleteWaitlistEntry("slot-to-delete") }
    }
}
