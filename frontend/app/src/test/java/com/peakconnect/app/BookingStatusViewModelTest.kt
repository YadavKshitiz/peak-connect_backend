package com.peakconnect.app

import androidx.lifecycle.SavedStateHandle
import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.network.AppError
import com.peakconnect.app.data.local.dao.ActivityDao
import com.peakconnect.app.data.local.dao.UserContentDao
import com.peakconnect.app.data.local.entity.ActivityEntity
import com.peakconnect.app.data.local.entity.BookingEntity
import com.peakconnect.app.data.local.entity.SlotEntity
import com.peakconnect.app.data.repository.BookingRepository
import com.peakconnect.app.domain.model.Booking
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.CancellationPolicyType
import com.peakconnect.app.domain.model.DifficultyLevel
import com.peakconnect.app.domain.model.RiskLevel
import com.peakconnect.app.domain.model.Season
import com.peakconnect.app.feature.trekker.booking.BookingStatusEvent
import com.peakconnect.app.feature.trekker.booking.BookingStatusViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
class BookingStatusViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val bookingRepository: BookingRepository = mockk(relaxed = true)
    private val activityDao: ActivityDao = mockk(relaxed = true)
    private val userContentDao: UserContentDao = mockk(relaxed = true)

    private val bookingId = "test-booking-123"
    private val slotId = "test-slot-456"
    private val activityId = "test-act-789"

    private val bookingFlow = MutableStateFlow<BookingEntity?>(null)

    private val sampleBooking = BookingEntity(
        id = bookingId,
        slotId = slotId,
        activityTitle = "Kuari Pass",
        trekkerId = "trekker-1",
        trekkerName = "Alex",
        guideId = "guide-1",
        guideName = "Tenzing",
        status = BookingStatus.PENDING_GUIDE_RESPONSE,
        date = LocalDateTime.now().plusDays(5),
        paymentOrderId = null,
        depositAmount = 500.0,
        refundAmount = null,
        refundPercentage = null,
        statusSinceMillis = 1000L,
        lastSyncMillis = 1000L
    )

    private val sampleSlot = SlotEntity(
        id = slotId,
        activityId = activityId,
        date = LocalDateTime.now().plusDays(5),
        capacity = 10,
        currentOccupancy = 4,
        season = Season.PEAK,
        currentPrice = BigDecimal("2500.00"),
        riskLevel = RiskLevel.LOW
    )

    private val sampleActivity = ActivityEntity(
        id = activityId,
        title = "Kuari Pass",
        description = "Scenic trek",
        location = "Joshimath",
        latitude = 30.55,
        longitude = 79.56,
        difficulty = DifficultyLevel.MODERATE,
        basePrice = BigDecimal("2500.00"),
        cancellationPolicy = CancellationPolicyType.FLEXIBLE,
        minSlotPrice = BigDecimal("2500.00"),
        lastUpdated = 1000L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { bookingRepository.getBookingFlow(bookingId) } returns bookingFlow
        coEvery { activityDao.getSlotById(slotId) } returns sampleSlot
        coEvery { activityDao.getActivityById(activityId) } returns sampleActivity
        coEvery { activityDao.getActivityForSlot(slotId) } returns sampleActivity
        coEvery { bookingRepository.refreshMyBookings() } returns ApiResult.Success(Unit)
    }

    private var currentViewModel: BookingStatusViewModel? = null

    @After
    fun tearDown() {
        currentViewModel?.stopPolling()
        Dispatchers.resetMain()
    }

    private fun createViewModel(): BookingStatusViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("bookingId" to bookingId))
        val vm = BookingStatusViewModel(savedStateHandle, bookingRepository, activityDao, userContentDao)
        currentViewModel = vm
        return vm
    }

    @Test
    fun `initial state loads booking slot and activity details`() = runTest {
        val viewModel = createViewModel()
        bookingFlow.value = sampleBooking
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(bookingId, state.booking?.id)
        assertEquals(slotId, state.slot?.id)
        assertEquals(activityId, state.activity?.id)
    }

    @Test
    fun `polling starts and periodically refreshes bookings`() = runTest {
        val viewModel = createViewModel()
        bookingFlow.value = sampleBooking
        advanceUntilIdle()

        viewModel.startPolling()
        assertTrue(viewModel.uiState.value.isPolling)

        // Advance 5 seconds
        advanceTimeBy(5100L)
        coVerify(atLeast = 1) { bookingRepository.refreshMyBookings() }

        // Advance another 5 seconds
        advanceTimeBy(5000L)
        coVerify(atLeast = 2) { bookingRepository.refreshMyBookings() }

        // Stop polling
        viewModel.stopPolling()
        assertFalse(viewModel.uiState.value.isPolling)

        io.mockk.clearMocks(bookingRepository, answers = false)
        advanceTimeBy(10000L)
        // No more calls after stopping
        coVerify(exactly = 0) { bookingRepository.refreshMyBookings() }
    }

    @Test
    fun `status change emits event and automatically stops polling on terminal status`() = runTest {
        val viewModel = createViewModel()
        bookingFlow.value = sampleBooking
        testScheduler.runCurrent()

        val events = mutableListOf<BookingStatusEvent>()
        val job = launch { viewModel.events.toList(events) }

        viewModel.startPolling()
        assertTrue(viewModel.uiState.value.isPolling)

        // Guide accepts -> status changes to AWAITING_PAYMENT
        bookingFlow.value = sampleBooking.copy(status = BookingStatus.AWAITING_PAYMENT)
        testScheduler.runCurrent()

        assertEquals(1, events.size)
        assertTrue(events[0] is BookingStatusEvent.StatusChanged)
        assertEquals(BookingStatus.AWAITING_PAYMENT, (events[0] as BookingStatusEvent.StatusChanged).newStatus)
        // Still polling in awaiting payment
        assertTrue(viewModel.uiState.value.isPolling)

        // Payment confirmed -> terminal status (automatically stops polling!)
        bookingFlow.value = sampleBooking.copy(status = BookingStatus.CONFIRMED)
        testScheduler.runCurrent()

        assertEquals(2, events.size)
        assertEquals(BookingStatus.CONFIRMED, (events[1] as BookingStatusEvent.StatusChanged).newStatus)
        // Polling should have automatically stopped
        assertFalse(viewModel.uiState.value.isPolling)

        job.cancel()
        viewModel.stopPolling()
    }

    @Test
    fun `cancel booking success sets authoritative refund and refreshes`() = runTest {
        val viewModel = createViewModel()
        bookingFlow.value = sampleBooking
        advanceUntilIdle()

        val cancelledDomainBooking = Booking(
            id = bookingId,
            slotId = slotId,
            activityTitle = "Kuari Pass",
            trekkerId = "trekker-1",
            trekkerName = "Alex",
            guideId = "guide-1",
            guideName = "Tenzing",
            status = BookingStatus.CANCELLED,
            date = sampleBooking.date,
            depositAmount = 500.0,
            refundAmount = 1600.0,
            refundPercentage = 1.0
        )

        coEvery { bookingRepository.cancelBooking(bookingId) } returns ApiResult.Success(cancelledDomainBooking)

        viewModel.cancelBooking()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isCancelling)
        assertNotNull(state.cancellationResult)
        assertEquals(1600.0, state.cancellationResult?.refundAmount)
        assertEquals(1.0, state.cancellationResult?.refundPercentage)

        coVerify { bookingRepository.refreshMyBookings() }
    }

    @Test
    fun `cancel booking failure with 409 conflict sets already cancelled message`() = runTest {
        val viewModel = createViewModel()
        bookingFlow.value = sampleBooking
        advanceUntilIdle()

        coEvery { bookingRepository.cancelBooking(bookingId) } returns ApiResult.Error(
            type = AppError.Conflict("Booking is already cancelled"),
            message = "Booking is already cancelled"
        )

        viewModel.cancelBooking()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isCancelling)
        assertEquals("This booking is already cancelled.", state.cancellationError)
    }

    @Test
    fun `booking linked to waitlist entry sets isFromWaitlist to true`() = runTest {
        coEvery { userContentDao.getWaitlistEntryByBookingId(bookingId) } returns mockk(relaxed = true)
        val viewModel = createViewModel()
        bookingFlow.value = sampleBooking
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isFromWaitlist)
    }
}
