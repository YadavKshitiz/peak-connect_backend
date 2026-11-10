package com.peakconnect.app

import com.peakconnect.app.data.local.entity.GuideBookingEntity
import com.peakconnect.app.data.repository.GuideRepository
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.feature.guide.bookings.GuideBookingFilter
import com.peakconnect.app.feature.guide.bookings.GuideBookingsViewModel
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
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class GuideBookingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val guideRepository: GuideRepository = mockk(relaxed = true)
    private val bookingsFlow = MutableStateFlow<List<GuideBookingEntity>>(emptyList())
    private val lastUpdatedFlow = MutableStateFlow<Long?>(5000L)

    private val testNow = LocalDateTime.of(2026, 11, 10, 12, 0)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { guideRepository.getGuideBookingsFlow() } returns bookingsFlow
        every { guideRepository.lastUpdatedBookingsFlow } returns lastUpdatedFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleBooking(
        id: String,
        status: BookingStatus,
        date: LocalDateTime = testNow.plusDays(1)
    ) = GuideBookingEntity(
        id = id,
        slotId = "slot_$id",
        activityTitle = "Trek $id",
        trekkerId = "trekker_1",
        trekkerName = "Trekker One",
        guideId = "g_1",
        guideName = "Guide One",
        status = status,
        date = date,
        paymentOrderId = null,
        depositAmount = null,
        refundAmount = null,
        refundPercentage = null,
        statusSinceMillis = 1000L,
        lastSyncMillis = 1000L
    )

    @Test
    fun `initial load observes bookings and updates filtered bookings`() = runTest {
        val b1 = sampleBooking("1", BookingStatus.PENDING_GUIDE_RESPONSE)
        val b2 = sampleBooking("2", BookingStatus.CONFIRMED)
        bookingsFlow.value = listOf(b1, b2)

        val viewModel = GuideBookingsViewModel(guideRepository)
        viewModel.nowSupplier = { testNow }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(2, viewModel.uiState.value.bookings.size)
        assertEquals(2, viewModel.uiState.value.filteredBookings.size)
    }

    @Test
    fun `filtering by status correctly filters list`() = runTest {
        val bPending = sampleBooking("1", BookingStatus.PENDING_GUIDE_RESPONSE)
        val bConfirmedFuture = sampleBooking("2", BookingStatus.CONFIRMED, testNow.plusDays(2))
        val bConfirmedPast = sampleBooking("3", BookingStatus.CONFIRMED, testNow.minusDays(2))
        val bCancelled = sampleBooking("4", BookingStatus.CANCELLED)

        bookingsFlow.value = listOf(bPending, bConfirmedFuture, bConfirmedPast, bCancelled)

        val viewModel = GuideBookingsViewModel(guideRepository)
        viewModel.nowSupplier = { testNow }
        advanceUntilIdle()

        viewModel.setFilter(GuideBookingFilter.PENDING)
        assertEquals(1, viewModel.uiState.value.filteredBookings.size)
        assertEquals("1", viewModel.uiState.value.filteredBookings[0].id)

        viewModel.setFilter(GuideBookingFilter.CONFIRMED)
        assertEquals(1, viewModel.uiState.value.filteredBookings.size)
        assertEquals("2", viewModel.uiState.value.filteredBookings[0].id)

        viewModel.setFilter(GuideBookingFilter.PAST)
        assertEquals(1, viewModel.uiState.value.filteredBookings.size)
        assertEquals("3", viewModel.uiState.value.filteredBookings[0].id)

        viewModel.setFilter(GuideBookingFilter.CANCELLED)
        assertEquals(1, viewModel.uiState.value.filteredBookings.size)
        assertEquals("4", viewModel.uiState.value.filteredBookings[0].id)
    }

    @Test
    fun `refresh triggers repository refresh`() = runTest {
        val viewModel = GuideBookingsViewModel(guideRepository)
        viewModel.nowSupplier = { testNow }
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        coVerify { guideRepository.refreshGuideBookings() }
        assertFalse(viewModel.uiState.value.isRefreshing)
    }
}
