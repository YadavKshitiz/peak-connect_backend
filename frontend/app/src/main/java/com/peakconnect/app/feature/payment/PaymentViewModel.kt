package com.peakconnect.app.feature.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakconnect.app.BuildConfig
import com.peakconnect.app.core.session.SessionManager
import com.peakconnect.app.core.session.SessionState
import com.peakconnect.app.data.local.entity.toDomain
import com.peakconnect.app.data.repository.BookingRepository
import com.peakconnect.app.domain.model.Booking
import com.peakconnect.app.domain.model.BookingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CheckoutRequest(
    val activityTitle: String,
    val paymentOrderId: String,
    val email: String?
)

sealed interface PaymentUiState {
    data object Loading : PaymentUiState
    data class Summary(
        val booking: Booking,
        val isPayEnabled: Boolean,
        val isCheckoutInProgress: Boolean,
        val isTestMode: Boolean
    ) : PaymentUiState
    data class InvalidStatus(
        val booking: Booking,
        val reason: String
    ) : PaymentUiState
    data class Processing(
        val booking: Booking
    ) : PaymentUiState
    data class Success(
        val booking: Booking
    ) : PaymentUiState
    data class Failed(
        val booking: Booking?,
        val reason: String?,
        val canRetry: Boolean
    ) : PaymentUiState
    data class Delayed(
        val booking: Booking
    ) : PaymentUiState
}

@HiltViewModel
class PaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookingRepository: BookingRepository,
    private val paymentEventBus: PaymentEventBus,
    private val sessionManager: SessionManager
) : ViewModel() {

    val bookingId: String = savedStateHandle.get<String>("bookingId") ?: ""

    private val _uiState = MutableStateFlow<PaymentUiState>(PaymentUiState.Loading)
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private val _checkoutRequests = MutableSharedFlow<CheckoutRequest>(extraBufferCapacity = 1)
    val checkoutRequests: SharedFlow<CheckoutRequest> = _checkoutRequests.asSharedFlow()

    private var latestBooking: Booking? = null
    private var isCheckoutInProgress: Boolean = false
    private val processedPaymentIds = mutableSetOf<String>()
    private var pollingJob: Job? = null

    val isTestMode: Boolean = BuildConfig.RAZORPAY_KEY_ID.startsWith("rzp_test_")

    init {
        observeBooking()
        observePaymentEvents()
        refreshBooking()
    }

    private fun observeBooking() {
        if (bookingId.isBlank()) return
        viewModelScope.launch {
            bookingRepository.getBookingFlow(bookingId).collect { entity ->
                val domain = entity?.toDomain()
                latestBooking = domain
                updateUiStateFromBooking(domain)
            }
        }
    }

    private fun updateUiStateFromBooking(booking: Booking?) {
        val currentState = _uiState.value
        if (currentState is PaymentUiState.Processing) {
            if (booking != null) {
                if (booking.status == BookingStatus.CONFIRMED) {
                    pollingJob?.cancel()
                    _uiState.value = PaymentUiState.Success(booking)
                    return
                } else if (booking.status == BookingStatus.CANCELLED) {
                    pollingJob?.cancel()
                    _uiState.value = PaymentUiState.Failed(
                        booking = booking,
                        reason = "Booking was cancelled by the server",
                        canRetry = false
                    )
                    return
                }
            }
            return
        }

        if (currentState is PaymentUiState.Success || currentState is PaymentUiState.Delayed) {
            return
        }

        if (booking == null) {
            _uiState.value = PaymentUiState.Loading
            return
        }

        if (booking.status == BookingStatus.AWAITING_PAYMENT) {
            val hasOrder = !booking.paymentOrderId.isNullOrBlank()
            _uiState.value = PaymentUiState.Summary(
                booking = booking,
                isPayEnabled = !isCheckoutInProgress && hasOrder,
                isCheckoutInProgress = isCheckoutInProgress,
                isTestMode = isTestMode
            )
        } else {
            _uiState.value = PaymentUiState.InvalidStatus(
                booking = booking,
                reason = "Booking is not awaiting payment (Status: ${booking.status})"
            )
        }
    }

    private fun observePaymentEvents() {
        viewModelScope.launch {
            paymentEventBus.events.collect { event ->
                handlePaymentEvent(event)
            }
        }
    }

    private fun handlePaymentEvent(event: PaymentEvent) {
        when (event) {
            is PaymentEvent.Success -> {
                // Safety requirement: Ignore repeated events with the same payment id
                if (event.paymentId.isNotBlank()) {
                    if (processedPaymentIds.contains(event.paymentId)) {
                        return
                    }
                    processedPaymentIds.add(event.paymentId)
                }

                isCheckoutInProgress = false
                val currentBooking = latestBooking
                if (currentBooking != null) {
                    _uiState.value = PaymentUiState.Processing(currentBooking)
                    startPollingForConfirmation(currentBooking)
                }
            }
            is PaymentEvent.Failure -> {
                isCheckoutInProgress = false
                val currentBooking = latestBooking
                val canRetry = currentBooking?.status == BookingStatus.AWAITING_PAYMENT
                _uiState.value = PaymentUiState.Failed(
                    booking = currentBooking,
                    reason = event.message ?: "Payment was cancelled or failed",
                    canRetry = canRetry
                )
            }
            is PaymentEvent.ExternalWallet -> {
                // Wallet selected, keep waiting for payment result
            }
        }
    }

    fun onPayClicked() {
        val booking = latestBooking ?: return
        // Safety guards
        if (isCheckoutInProgress) return
        if (booking.status != BookingStatus.AWAITING_PAYMENT) return
        val orderId = booking.paymentOrderId
        if (orderId.isNullOrBlank()) return

        isCheckoutInProgress = true
        _uiState.value = PaymentUiState.Summary(
            booking = booking,
            isPayEnabled = false,
            isCheckoutInProgress = true,
            isTestMode = isTestMode
        )

        val email = (sessionManager.sessionState.value as? SessionState.LoggedIn)?.email
        _checkoutRequests.tryEmit(
            CheckoutRequest(
                activityTitle = booking.activityTitle,
                paymentOrderId = orderId,
                email = email
            )
        )
    }

    fun onTryAgain() {
        isCheckoutInProgress = false
        pollingJob?.cancel()
        refreshBooking()
        val booking = latestBooking
        if (booking != null && booking.status == BookingStatus.AWAITING_PAYMENT) {
            _uiState.value = PaymentUiState.Summary(
                booking = booking,
                isPayEnabled = !booking.paymentOrderId.isNullOrBlank(),
                isCheckoutInProgress = false,
                isTestMode = isTestMode
            )
        }
    }

    fun onCheckAgain() {
        val booking = latestBooking ?: return
        _uiState.value = PaymentUiState.Processing(booking)
        startPollingForConfirmation(booking)
    }

    fun refreshBooking() {
        viewModelScope.launch {
            bookingRepository.refreshMyBookings()
        }
    }

    private fun startPollingForConfirmation(booking: Booking) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var confirmed = false
            var cancelled = false

            // Poll every 3 seconds, up to 60 seconds (20 iterations)
            for (attempt in 1..20) {
                delay(3000L)
                bookingRepository.refreshMyBookings()
                val current = latestBooking
                if (current != null) {
                    if (current.status == BookingStatus.CONFIRMED) {
                        confirmed = true
                        _uiState.value = PaymentUiState.Success(current)
                        break
                    } else if (current.status == BookingStatus.CANCELLED) {
                        cancelled = true
                        _uiState.value = PaymentUiState.Failed(
                            booking = current,
                            reason = "Booking was cancelled by the server",
                            canRetry = false
                        )
                        break
                    }
                }
            }

            if (!confirmed && !cancelled) {
                val current = latestBooking ?: booking
                if (current.status == BookingStatus.CONFIRMED) {
                    _uiState.value = PaymentUiState.Success(current)
                } else if (current.status == BookingStatus.CANCELLED) {
                    _uiState.value = PaymentUiState.Failed(
                        booking = current,
                        reason = "Booking was cancelled by the server",
                        canRetry = false
                    )
                } else {
                    _uiState.value = PaymentUiState.Delayed(current)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
