package com.peakconnect.app

import androidx.lifecycle.SavedStateHandle
import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.session.SessionManager
import com.peakconnect.app.core.session.SessionState
import com.peakconnect.app.data.local.entity.BookingEntity
import com.peakconnect.app.data.repository.BookingRepository
import com.peakconnect.app.domain.model.Booking
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.MatchedGuide
import com.peakconnect.app.domain.model.Role
import com.peakconnect.app.feature.payment.CheckoutRequest
import com.peakconnect.app.feature.payment.PaymentEvent
import com.peakconnect.app.feature.payment.PaymentEventBus
import com.peakconnect.app.feature.payment.PaymentUiState
import com.peakconnect.app.feature.payment.PaymentViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PaymentViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val bookingId = "booking-123"

    private class FakeEventBus : PaymentEventBus {
        private val _events = MutableSharedFlow<PaymentEvent>(replay = 0, extraBufferCapacity = 64)
        override val events: SharedFlow<PaymentEvent> = _events.asSharedFlow()

        override fun post(event: PaymentEvent) {
            _events.tryEmit(event)
        }

        override suspend fun emit(event: PaymentEvent) {
            _events.emit(event)
        }
    }

    private class FakeBookingRepository : BookingRepository {
        val bookingFlow = MutableStateFlow<BookingEntity?>(null)
        var refreshCount = 0
        var onRefresh: (() -> Unit)? = null

        override val lastUpdatedFlow: Flow<Long?> = flowOf(1000L)
        override fun getMyBookingsFlow(): Flow<List<BookingEntity>> = flowOf(emptyList())
        override fun getBookingFlow(id: String): Flow<BookingEntity?> = bookingFlow

        override suspend fun refreshMyBookings(): ApiResult<Unit> {
            refreshCount++
            onRefresh?.invoke()
            return ApiResult.Success(Unit)
        }

        override suspend fun requestMatches(
            slotId: String,
            skillWeight: Double?,
            locationWeight: Double?,
            languageWeight: Double?
        ): ApiResult<List<MatchedGuide>> = ApiResult.Success(emptyList())

        override suspend fun confirmBooking(slotId: String, guideId: String): ApiResult<Booking> =
            ApiResult.Success(mockk(relaxed = true))

        override suspend fun cancelBooking(bookingId: String): ApiResult<Booking> =
            ApiResult.Success(mockk(relaxed = true))

        override suspend fun acceptBooking(bookingId: String): ApiResult<Booking> =
            ApiResult.Success(mockk(relaxed = true))

        override suspend fun declineBooking(bookingId: String): ApiResult<Booking> =
            ApiResult.Success(mockk(relaxed = true))

        override suspend fun markNoShow(bookingId: String): ApiResult<Booking> =
            ApiResult.Success(mockk(relaxed = true))

        override suspend fun joinWaitlist(
            slotId: String,
            guideId: String,
            activityTitle: String,
            slotDate: LocalDateTime
        ): ApiResult<String> = ApiResult.Success("ok")

        override suspend fun getBookingForSlot(slotId: String): BookingEntity? = null
        override fun getCachedMatchedGuide(guideId: String): MatchedGuide? = null
    }

    private lateinit var fakeEventBus: FakeEventBus
    private lateinit var fakeRepo: FakeBookingRepository
    private val sessionManager: SessionManager = mockk(relaxed = true)

    private fun sampleBooking(
        status: BookingStatus = BookingStatus.AWAITING_PAYMENT,
        orderId: String? = "order_test_99"
    ) = BookingEntity(
        id = bookingId,
        slotId = "slot-1",
        activityTitle = "Roopkund Trek",
        trekkerId = "user-1",
        trekkerName = "Trekker One",
        guideId = "guide-1",
        guideName = "Sherpa Tenzing",
        status = status,
        date = LocalDateTime.now().plusDays(7),
        paymentOrderId = orderId,
        depositAmount = 1500.0,
        refundAmount = null,
        refundPercentage = null,
        statusSinceMillis = 1000L,
        lastSyncMillis = 1000L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeEventBus = FakeEventBus()
        fakeRepo = FakeBookingRepository()

        every { sessionManager.sessionState } returns MutableStateFlow(
            SessionState.LoggedIn(
                role = Role.TREKKER,
                userId = "user-1",
                email = "trekker@example.com",
                name = "Trekker One"
            )
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): PaymentViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("bookingId" to bookingId))
        return PaymentViewModel(
            savedStateHandle = savedStateHandle,
            bookingRepository = fakeRepo,
            paymentEventBus = fakeEventBus,
            sessionManager = sessionManager
        )
    }

    @Test
    fun test_success_then_confirmed_within_polling() = runTest(testDispatcher) {
        val initialBooking = sampleBooking(BookingStatus.AWAITING_PAYMENT)
        fakeRepo.bookingFlow.value = initialBooking

        val viewModel = createViewModel()
        testScheduler.runCurrent()

        assertTrue(viewModel.uiState.value is PaymentUiState.Summary)
        val summary = viewModel.uiState.value as PaymentUiState.Summary
        assertTrue(summary.isPayEnabled)

        // When SDK reports success, polling starts
        fakeEventBus.emit(PaymentEvent.Success("pay_success_1", "order_test_99"))
        testScheduler.runCurrent()

        assertTrue(viewModel.uiState.value is PaymentUiState.Processing)

        // Backend confirms the booking during polling
        fakeRepo.onRefresh = {
            fakeRepo.bookingFlow.value = initialBooking.copy(status = BookingStatus.CONFIRMED)
        }

        // Advance 3 seconds for the first poll
        advanceTimeBy(3000L)
        testScheduler.runCurrent()

        assertTrue(viewModel.uiState.value is PaymentUiState.Success)
        val successState = viewModel.uiState.value as PaymentUiState.Success
        assertEquals(BookingStatus.CONFIRMED, successState.booking.status)
        assertEquals("Roopkund Trek", successState.booking.activityTitle)
    }

    @Test
    fun test_success_but_delayed() = runTest(testDispatcher) {
        val initialBooking = sampleBooking(BookingStatus.AWAITING_PAYMENT)
        fakeRepo.bookingFlow.value = initialBooking

        val viewModel = createViewModel()
        testScheduler.runCurrent()

        fakeEventBus.emit(PaymentEvent.Success("pay_delayed_1", "order_test_99"))
        testScheduler.runCurrent()
        assertTrue(viewModel.uiState.value is PaymentUiState.Processing)

        // Polling loop runs for 20 attempts * 3000ms = 60,000ms without confirmation
        advanceTimeBy(60_000L)
        testScheduler.runCurrent()

        assertTrue(viewModel.uiState.value is PaymentUiState.Delayed)
        val delayedState = viewModel.uiState.value as PaymentUiState.Delayed
        assertEquals(bookingId, delayedState.booking.id)
    }

    @Test
    fun test_failure() = runTest(testDispatcher) {
        val initialBooking = sampleBooking(BookingStatus.AWAITING_PAYMENT)
        fakeRepo.bookingFlow.value = initialBooking

        val viewModel = createViewModel()
        testScheduler.runCurrent()

        fakeEventBus.emit(PaymentEvent.Failure(code = 0, message = "Payment was cancelled by the user"))
        testScheduler.runCurrent()

        assertTrue(viewModel.uiState.value is PaymentUiState.Failed)
        val failedState = viewModel.uiState.value as PaymentUiState.Failed
        assertEquals("Payment was cancelled by the user", failedState.reason)
        assertTrue(failedState.canRetry)

        // Test onTryAgain resets state to summary
        viewModel.onTryAgain()
        testScheduler.runCurrent()
        assertTrue(viewModel.uiState.value is PaymentUiState.Summary)
    }

    @Test
    fun test_cancelled_by_backend() = runTest(testDispatcher) {
        val initialBooking = sampleBooking(BookingStatus.AWAITING_PAYMENT)
        fakeRepo.bookingFlow.value = initialBooking

        val viewModel = createViewModel()
        testScheduler.runCurrent()

        fakeEventBus.emit(PaymentEvent.Success("pay_cancelled_1", "order_test_99"))
        testScheduler.runCurrent()
        assertTrue(viewModel.uiState.value is PaymentUiState.Processing)

        // Backend reports booking CANCELLED (e.g. webhook failure response)
        fakeRepo.onRefresh = {
            fakeRepo.bookingFlow.value = initialBooking.copy(status = BookingStatus.CANCELLED)
        }

        advanceTimeBy(3000L)
        testScheduler.runCurrent()

        assertTrue(viewModel.uiState.value is PaymentUiState.Failed)
        val failedState = viewModel.uiState.value as PaymentUiState.Failed
        assertFalse(failedState.canRetry)
    }

    @Test
    fun test_guard_when_status_not_awaiting_payment() = runTest(testDispatcher) {
        // 1. Status is PENDING_GUIDE_RESPONSE
        fakeRepo.bookingFlow.value = sampleBooking(BookingStatus.PENDING_GUIDE_RESPONSE)

        val viewModel = createViewModel()
        testScheduler.runCurrent()

        assertTrue(viewModel.uiState.value is PaymentUiState.InvalidStatus)

        // Checkout request should not be emitted
        val emittedRequests = mutableListOf<CheckoutRequest>()
        val job = launch {
            viewModel.checkoutRequests.collect { emittedRequests.add(it) }
        }

        viewModel.onPayClicked()
        testScheduler.runCurrent()
        assertTrue(emittedRequests.isEmpty())

        // 2. Status is AWAITING_PAYMENT but paymentOrderId is blank
        fakeRepo.bookingFlow.value = sampleBooking(BookingStatus.AWAITING_PAYMENT, orderId = "")
        testScheduler.runCurrent()

        assertTrue(viewModel.uiState.value is PaymentUiState.Summary)
        val summary = viewModel.uiState.value as PaymentUiState.Summary
        assertFalse(summary.isPayEnabled)

        viewModel.onPayClicked()
        testScheduler.runCurrent()
        assertTrue(emittedRequests.isEmpty())

        job.cancel()
    }

    @Test
    fun test_double_event_dedupe() = runTest(testDispatcher) {
        val initialBooking = sampleBooking(BookingStatus.AWAITING_PAYMENT)
        fakeRepo.bookingFlow.value = initialBooking

        val viewModel = createViewModel()
        testScheduler.runCurrent()

        // First event
        fakeEventBus.emit(PaymentEvent.Success("pay_dup_999", "order_test_99"))
        testScheduler.runCurrent()
        assertTrue(viewModel.uiState.value is PaymentUiState.Processing)

        var refreshCountAfterFirst = fakeRepo.refreshCount

        // Duplicate event with same paymentId
        fakeEventBus.emit(PaymentEvent.Success("pay_dup_999", "order_test_99"))
        testScheduler.runCurrent()

        // Advance 3s
        advanceTimeBy(3000L)
        testScheduler.runCurrent()

        // Only one polling cycle was executed (refreshCount increased by 1 from the 3s delay, not 2)
        assertEquals(refreshCountAfterFirst + 1, fakeRepo.refreshCount)
    }
}
