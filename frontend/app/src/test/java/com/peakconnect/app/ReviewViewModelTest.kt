package com.peakconnect.app

import androidx.lifecycle.SavedStateHandle
import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.network.AppError
import com.peakconnect.app.data.local.entity.ActivityEntity
import com.peakconnect.app.data.local.entity.BookingEntity
import com.peakconnect.app.data.local.entity.SlotEntity
import com.peakconnect.app.data.repository.ActivityRepository
import com.peakconnect.app.data.repository.BookingRepository
import com.peakconnect.app.domain.model.Booking
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.CancellationPolicyType
import com.peakconnect.app.domain.model.DifficultyLevel
import com.peakconnect.app.domain.model.ExperienceLevel
import com.peakconnect.app.domain.model.MatchedGuide
import com.peakconnect.app.domain.model.RiskLevel
import com.peakconnect.app.domain.model.Season
import com.peakconnect.app.feature.trekker.review.BookingReviewViewModel
import com.peakconnect.app.feature.trekker.review.ReviewUiEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class ReviewViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val bookingRepository: BookingRepository = mockk(relaxed = true)
    private val activityRepository: ActivityRepository = mockk(relaxed = true)

    private val slotId = "slot-rev-101"
    private val guideId = "guide-rev-202"

    private val sampleSlot = SlotEntity(
        id = slotId,
        activityId = "act-1",
        date = LocalDateTime.now().plusDays(3),
        capacity = 10,
        currentOccupancy = 5,
        season = Season.PEAK,
        currentPrice = BigDecimal("10000.00"),
        riskLevel = RiskLevel.LOW
    )

    private val sampleActivity = ActivityEntity(
        id = "act-1",
        title = "Hampta Pass",
        description = "Scenic trek",
        location = "Manali",
        latitude = 32.24,
        longitude = 77.19,
        difficulty = DifficultyLevel.CHALLENGING,
        basePrice = BigDecimal("8000.00"),
        cancellationPolicy = CancellationPolicyType.MODERATE,
        minSlotPrice = BigDecimal("10000.00"),
        lastUpdated = System.currentTimeMillis()
    )

    private val sampleGuide = MatchedGuide(
        guideId = guideId,
        name = "Aman Verma",
        skills = listOf("Rock Climbing", "First Aid"),
        languages = listOf("English", "Hindi"),
        location = "Manali",
        experienceLevel = ExperienceLevel.EXPERT,
        score = 0.95
    )

    private val sampleBooking = Booking(
        id = "booking-confirmed-1",
        slotId = slotId,
        activityTitle = "Hampta Pass",
        trekkerId = "trekker-1",
        trekkerName = "Trekker User",
        guideId = guideId,
        guideName = "Aman Verma",
        status = BookingStatus.PENDING_GUIDE_RESPONSE,
        date = sampleSlot.date
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { activityRepository.getSlotById(slotId) } returns sampleSlot
        coEvery { activityRepository.getActivityForSlot(slotId) } returns sampleActivity
        coEvery { bookingRepository.getCachedMatchedGuide(guideId) } returns sampleGuide
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads data and calculates 20 percent estimated deposit accurately`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf("slotId" to slotId, "guideId" to guideId))
        val viewModel = BookingReviewViewModel(bookingRepository, activityRepository, savedStateHandle)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(sampleSlot, state.slot)
        assertEquals(sampleActivity, state.activity)
        assertEquals(sampleGuide, state.guide)
        assertEquals(20, state.depositPercentage)
        // 20% of 10000.00 = 2000.00
        assertEquals(BigDecimal("2000.00"), state.estimatedDeposit)
    }

    @Test
    fun `confirm booking success emits BookingSuccess`() = runTest(testDispatcher) {
        coEvery {
            bookingRepository.confirmBooking(slotId, guideId)
        } returns ApiResult.Success(sampleBooking)

        val savedStateHandle = SavedStateHandle(mapOf("slotId" to slotId, "guideId" to guideId))
        val viewModel = BookingReviewViewModel(bookingRepository, activityRepository, savedStateHandle)
        advanceUntilIdle()

        val events = mutableListOf<ReviewUiEvent>()
        val job = launch { viewModel.events.toList(events) }

        viewModel.confirmBooking()
        advanceUntilIdle()

        assertTrue(events.any { it is ReviewUiEvent.BookingSuccess && it.bookingId == "booking-confirmed-1" })
        job.cancel()
    }

    @Test
    fun `double tap protection prevents multiple concurrent confirm calls`() = runTest(testDispatcher) {
        coEvery {
            bookingRepository.confirmBooking(slotId, guideId)
        } returns ApiResult.Success(sampleBooking)

        val savedStateHandle = SavedStateHandle(mapOf("slotId" to slotId, "guideId" to guideId))
        val viewModel = BookingReviewViewModel(bookingRepository, activityRepository, savedStateHandle)
        advanceUntilIdle()

        viewModel.confirmBooking()
        // Immediately trigger second call before idle
        viewModel.confirmBooking()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            bookingRepository.confirmBooking(slotId, guideId)
        }
    }

    @Test
    fun `409 slot full shows slot full dialog`() = runTest(testDispatcher) {
        coEvery {
            bookingRepository.confirmBooking(slotId, guideId)
        } returns ApiResult.Error(
            AppError.Conflict("Slot is already full. Join waitlist?"),
            "Slot is already full"
        )

        val savedStateHandle = SavedStateHandle(mapOf("slotId" to slotId, "guideId" to guideId))
        val viewModel = BookingReviewViewModel(bookingRepository, activityRepository, savedStateHandle)
        advanceUntilIdle()

        viewModel.confirmBooking()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showSlotFullDialog)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun `409 other error shows general error dialog with backend message`() = runTest(testDispatcher) {
        coEvery {
            bookingRepository.confirmBooking(slotId, guideId)
        } returns ApiResult.Error(
            AppError.Conflict("Guide is no longer available for this slot"),
            "Guide is no longer available"
        )

        val savedStateHandle = SavedStateHandle(mapOf("slotId" to slotId, "guideId" to guideId))
        val viewModel = BookingReviewViewModel(bookingRepository, activityRepository, savedStateHandle)
        advanceUntilIdle()

        viewModel.confirmBooking()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showGeneralErrorDialog)
        assertEquals("Guide is no longer available", state.generalErrorMessage)
    }

    @Test
    fun `timeout on confirm triggers recovery check on retry and detects existing booking`() = runTest(testDispatcher) {
        // First attempt fails with Timeout
        coEvery {
            bookingRepository.confirmBooking(slotId, guideId)
        } returns ApiResult.Error(AppError.Timeout, "Request timed out")

        val savedStateHandle = SavedStateHandle(mapOf("slotId" to slotId, "guideId" to guideId))
        val viewModel = BookingReviewViewModel(bookingRepository, activityRepository, savedStateHandle)
        advanceUntilIdle()

        viewModel.confirmBooking()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.networkTimeoutError)

        // Simulate server actually received the booking
        val existingBookingEntity = BookingEntity(
            id = "recovered-booking-99",
            slotId = slotId,
            activityTitle = "Hampta Pass",
            trekkerId = "trekker-1",
            trekkerName = "Trekker",
            guideId = guideId,
            guideName = "Aman Verma",
            status = BookingStatus.PENDING_GUIDE_RESPONSE,
            date = sampleSlot.date,
            paymentOrderId = null,
            depositAmount = null,
            refundAmount = null,
            refundPercentage = null,
            statusSinceMillis = System.currentTimeMillis(),
            lastSyncMillis = System.currentTimeMillis()
        )
        coEvery { bookingRepository.getBookingForSlot(slotId) } returns existingBookingEntity

        val events = mutableListOf<ReviewUiEvent>()
        val job = launch { viewModel.events.toList(events) }

        // User hits Retry
        viewModel.retryConfirmWithRecoveryCheck()
        advanceUntilIdle()

        coVerify { bookingRepository.refreshMyBookings() }
        coVerify(exactly = 1) { bookingRepository.confirmBooking(any(), any()) } // Not called a second time!
        assertTrue(events.any { it is ReviewUiEvent.BookingSuccess && it.bookingId == "recovered-booking-99" })
        job.cancel()
    }
}
