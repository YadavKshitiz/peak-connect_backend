package com.peakconnect.app

import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.network.AppError
import com.peakconnect.app.data.local.entity.GuideBookingEntity
import com.peakconnect.app.data.local.entity.GuideProfileEntity
import com.peakconnect.app.data.repository.BookingRepository
import com.peakconnect.app.data.repository.GuideRepository
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.ExperienceLevel
import com.peakconnect.app.feature.guide.home.GuideHomeViewModel
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
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GuideHomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val guideRepository: GuideRepository = mockk(relaxed = true)
    private val bookingRepository: BookingRepository = mockk(relaxed = true)

    private val profileFlow = MutableStateFlow<GuideProfileEntity?>(null)
    private val bookingsFlow = MutableStateFlow<List<GuideBookingEntity>>(emptyList())

    private val fixedNow = LocalDateTime.of(2026, 11, 1, 12, 0)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { guideRepository.getProfileFlow() } returns profileFlow
        every { guideRepository.getGuideBookingsFlow() } returns bookingsFlow
        coEvery { guideRepository.refreshProfile() } returns ApiResult.Success(Unit)
        coEvery { guideRepository.refreshGuideBookings() } returns ApiResult.Success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createGuideBooking(
        id: String,
        status: BookingStatus,
        date: LocalDateTime,
        statusSinceMillis: Long = 1000L
    ): GuideBookingEntity {
        return GuideBookingEntity(
            id = id,
            slotId = "slot-$id",
            activityTitle = "Triund Trek",
            trekkerId = "trekker-1",
            trekkerName = "Aarav Sharma",
            guideId = "guide-1",
            guideName = "Kshitiz",
            status = status,
            date = date,
            paymentOrderId = null,
            depositAmount = 500.0,
            refundAmount = null,
            refundPercentage = null,
            statusSinceMillis = statusSinceMillis,
            lastSyncMillis = 1000L
        )
    }

    @Test
    fun `verification banner reflects unverified and incomplete profile`() = runTest {
        val incompleteProfile = GuideProfileEntity(
            id = "g1",
            name = "Kshitiz",
            email = "guide@test.com",
            skills = emptyList(),
            languages = emptyList(),
            location = null,
            latitude = null,
            longitude = null,
            experienceLevel = ExperienceLevel.BEGINNER,
            isVerified = false,
            lastUpdated = 1000L
        )
        profileFlow.value = incompleteProfile

        val viewModel = GuideHomeViewModel(guideRepository, bookingRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isVerified)
        assertTrue(state.isProfileIncomplete)
    }

    @Test
    fun `verification banner reflects verified guide`() = runTest {
        val verifiedProfile = GuideProfileEntity(
            id = "g1",
            name = "Kshitiz",
            email = "guide@test.com",
            skills = listOf("High Altitude"),
            languages = listOf("English", "Hindi"),
            location = "Manali",
            latitude = 32.2,
            longitude = 77.1,
            experienceLevel = ExperienceLevel.EXPERT,
            isVerified = true,
            lastUpdated = 1000L
        )
        profileFlow.value = verifiedProfile

        val viewModel = GuideHomeViewModel(guideRepository, bookingRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isVerified)
        assertFalse(state.isProfileIncomplete)
    }

    @Test
    fun `stats row and sections calculate pending upcoming and completed correctly`() = runTest {
        val bookings = listOf(
            createGuideBooking("b1", BookingStatus.PENDING_GUIDE_RESPONSE, fixedNow.plusDays(1), statusSinceMillis = 500L),
            createGuideBooking("b2", BookingStatus.PENDING_GUIDE_RESPONSE, fixedNow.plusDays(2), statusSinceMillis = 200L),
            createGuideBooking("b3", BookingStatus.CONFIRMED, fixedNow.plusDays(3)), // upcoming
            createGuideBooking("b4", BookingStatus.CONFIRMED, fixedNow.minusDays(1)), // completed
            createGuideBooking("b5", BookingStatus.CANCELLED, fixedNow.plusDays(1)) // cancelled (not in stats)
        )
        bookingsFlow.value = bookings

        val viewModel = GuideHomeViewModel(guideRepository, bookingRepository).apply {
            nowSupplier = { fixedNow }
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.pendingCount)
        assertEquals(1, state.upcomingCount)
        assertEquals(1, state.completedCount)

        // Pending bookings are sorted by statusSinceMillis
        assertEquals(2, state.pendingBookings.size)
        assertEquals("b2", state.pendingBookings[0].id)
        assertEquals("b1", state.pendingBookings[1].id)

        // Upcoming bookings
        assertEquals(1, state.upcomingBookings.size)
        assertEquals("b3", state.upcomingBookings[0].id)
    }

    @Test
    fun `accept booking success triggers repository refresh and sets success message`() = runTest {
        coEvery { bookingRepository.acceptBooking("b1") } returns ApiResult.Success(mockk(relaxed = true))

        val viewModel = GuideHomeViewModel(guideRepository, bookingRepository)
        advanceUntilIdle()

        viewModel.acceptBooking("b1")
        advanceUntilIdle()

        coVerify { bookingRepository.acceptBooking("b1") }
        coVerify(atLeast = 2) { guideRepository.refreshGuideBookings() }
        assertEquals("Booking request accepted", viewModel.uiState.value.successMessage)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `accept booking conflict error displays expired or responded message`() = runTest {
        coEvery { bookingRepository.acceptBooking("b1") } returns ApiResult.Error(
            type = AppError.Conflict("Conflict occurred"),
            message = "Conflict occurred"
        )

        val viewModel = GuideHomeViewModel(guideRepository, bookingRepository)
        advanceUntilIdle()

        viewModel.acceptBooking("b1")
        advanceUntilIdle()

        assertEquals(
            "This booking request has expired or has already been responded to.",
            viewModel.uiState.value.errorMessage
        )
    }

    @Test
    fun `decline booking success closes dialog and sets success message`() = runTest {
        coEvery { bookingRepository.declineBooking("b1") } returns ApiResult.Success(mockk(relaxed = true))

        val viewModel = GuideHomeViewModel(guideRepository, bookingRepository)
        advanceUntilIdle()

        viewModel.showDeclineDialog("b1")
        assertEquals("b1", viewModel.uiState.value.declineDialogBookingId)

        viewModel.declineBooking("b1")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.declineDialogBookingId)
        coVerify { bookingRepository.declineBooking("b1") }
        assertEquals("Booking request declined", viewModel.uiState.value.successMessage)
    }
}
