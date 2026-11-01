package com.peakconnect.app

import androidx.lifecycle.SavedStateHandle
import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.network.AppError
import com.peakconnect.app.data.local.dao.ActivityDao
import com.peakconnect.app.data.local.entity.ActivityEntity
import com.peakconnect.app.data.local.entity.GuideBookingEntity
import com.peakconnect.app.data.repository.BookingRepository
import com.peakconnect.app.data.repository.GuideRepository
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.CancellationPolicyType
import com.peakconnect.app.domain.model.DifficultyLevel
import com.peakconnect.app.feature.guide.detail.GuideBookingDetailViewModel
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
import org.junit.Assert.assertFalse
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
class GuideBookingDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val guideRepository: GuideRepository = mockk(relaxed = true)
    private val bookingRepository: BookingRepository = mockk(relaxed = true)
    private val activityDao: ActivityDao = mockk(relaxed = true)

    private val bookingFlow = MutableStateFlow<GuideBookingEntity?>(null)
    private val fixedNow = LocalDateTime.of(2026, 11, 1, 12, 0)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { guideRepository.getGuideBookingFlow("booking-1") } returns bookingFlow
        coEvery { guideRepository.refreshGuideBookings() } returns ApiResult.Success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createBooking(
        id: String = "booking-1",
        status: BookingStatus = BookingStatus.PENDING_GUIDE_RESPONSE,
        date: LocalDateTime = fixedNow.plusDays(2)
    ): GuideBookingEntity {
        return GuideBookingEntity(
            id = id,
            slotId = "slot-1",
            activityTitle = "Kheerganga Trek",
            trekkerId = "trekker-1",
            trekkerName = "Priya Patel",
            guideId = "guide-1",
            guideName = "Kshitiz",
            status = status,
            date = date,
            paymentOrderId = null,
            depositAmount = 750.0,
            refundAmount = null,
            refundPercentage = null,
            statusSinceMillis = 5000L,
            lastSyncMillis = 1000L
        )
    }

    @Test
    fun `booking detail loads booking and activity location`() = runTest {
        val booking = createBooking()
        bookingFlow.value = booking

        val activity = ActivityEntity(
            id = "act-1",
            title = "Kheerganga Trek",
            description = "Hot springs trek",
            location = "Kasol, Himachal Pradesh",
            latitude = 32.0,
            longitude = 77.3,
            difficulty = DifficultyLevel.MODERATE,
            basePrice = BigDecimal("1500"),
            cancellationPolicy = CancellationPolicyType.MODERATE,
            minSlotPrice = BigDecimal("1500"),
            lastUpdated = 1000L
        )
        coEvery { activityDao.getSlotById("slot-1") } returns null
        coEvery { activityDao.getActivityForSlot("slot-1") } returns activity

        val savedStateHandle = SavedStateHandle(mapOf("bookingId" to "booking-1"))
        val viewModel = GuideBookingDetailViewModel(savedStateHandle, guideRepository, bookingRepository, activityDao)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.booking)
        assertEquals("Priya Patel", state.booking?.trekkerName)
        assertEquals("Kasol, Himachal Pradesh", state.activityLocation)
    }

    @Test
    fun `accept booking success sets success message and triggers refresh`() = runTest {
        val booking = createBooking()
        bookingFlow.value = booking

        coEvery { bookingRepository.acceptBooking("booking-1") } returns ApiResult.Success(mockk(relaxed = true))

        val savedStateHandle = SavedStateHandle(mapOf("bookingId" to "booking-1"))
        val viewModel = GuideBookingDetailViewModel(savedStateHandle, guideRepository, bookingRepository, activityDao)
        advanceUntilIdle()

        viewModel.acceptBooking()
        advanceUntilIdle()

        coVerify { bookingRepository.acceptBooking("booking-1") }
        coVerify(atLeast = 2) { guideRepository.refreshGuideBookings() }
        assertEquals("Booking request accepted", viewModel.uiState.value.successMessage)
    }

    @Test
    fun `accept booking conflict 409 maps to expired or responded message`() = runTest {
        val booking = createBooking()
        bookingFlow.value = booking

        coEvery { bookingRepository.acceptBooking("booking-1") } returns ApiResult.Error(
            type = AppError.Conflict("Conflict occurred"),
            message = "Conflict occurred"
        )

        val savedStateHandle = SavedStateHandle(mapOf("bookingId" to "booking-1"))
        val viewModel = GuideBookingDetailViewModel(savedStateHandle, guideRepository, bookingRepository, activityDao)
        advanceUntilIdle()

        viewModel.acceptBooking()
        advanceUntilIdle()

        assertEquals(
            "This booking request has expired or has already been responded to.",
            viewModel.uiState.value.errorMessage
        )
    }

    @Test
    fun `decline booking flow manages dialog and executes decline`() = runTest {
        val booking = createBooking()
        bookingFlow.value = booking

        coEvery { bookingRepository.declineBooking("booking-1") } returns ApiResult.Success(mockk(relaxed = true))

        val savedStateHandle = SavedStateHandle(mapOf("bookingId" to "booking-1"))
        val viewModel = GuideBookingDetailViewModel(savedStateHandle, guideRepository, bookingRepository, activityDao)
        advanceUntilIdle()

        viewModel.showDeclineDialog(true)
        assertTrue(viewModel.uiState.value.showDeclineDialog)

        viewModel.declineBooking()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showDeclineDialog)
        coVerify { bookingRepository.declineBooking("booking-1") }
        assertEquals("Booking request declined", viewModel.uiState.value.successMessage)
    }

    @Test
    fun `mark no-show flow manages dialog and executes no-show`() = runTest {
        val booking = createBooking(status = BookingStatus.CONFIRMED, date = fixedNow.minusDays(1))
        bookingFlow.value = booking

        coEvery { bookingRepository.markNoShow("booking-1") } returns ApiResult.Success(mockk(relaxed = true))

        val savedStateHandle = SavedStateHandle(mapOf("bookingId" to "booking-1"))
        val viewModel = GuideBookingDetailViewModel(savedStateHandle, guideRepository, bookingRepository, activityDao)
        advanceUntilIdle()

        viewModel.showNoShowDialog(true)
        assertTrue(viewModel.uiState.value.showNoShowDialog)

        viewModel.markNoShow()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showNoShowDialog)
        coVerify { bookingRepository.markNoShow("booking-1") }
        assertEquals("Trekker marked as no-show", viewModel.uiState.value.successMessage)
    }
}
