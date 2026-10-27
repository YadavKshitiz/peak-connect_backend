package com.peakconnect.app.feature.trekker.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.network.AppError
import com.peakconnect.app.data.local.entity.ActivityEntity
import com.peakconnect.app.data.local.entity.SlotEntity
import com.peakconnect.app.data.repository.ActivityRepository
import com.peakconnect.app.data.repository.BookingRepository
import com.peakconnect.app.domain.model.Booking
import com.peakconnect.app.domain.model.CancellationPolicyInfo
import com.peakconnect.app.domain.model.MatchedGuide
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject

data class ReviewUiState(
    val slotId: String = "",
    val guideId: String = "",
    val slot: SlotEntity? = null,
    val activity: ActivityEntity? = null,
    val guide: MatchedGuide? = null,
    val estimatedDeposit: BigDecimal = BigDecimal.ZERO,
    val depositPercentage: Int = CancellationPolicyInfo.DEPOSIT_PERCENTAGE_INT,
    val isLoadingData: Boolean = false,
    val isSubmitting: Boolean = false,
    val isWaitlistSubmitting: Boolean = false,
    val showSlotFullDialog: Boolean = false,
    val showWaitlistConfirmDialog: Boolean = false,
    val showGeneralErrorDialog: Boolean = false,
    val generalErrorMessage: String? = null,
    val networkTimeoutError: Boolean = false,
    val recoveryMessage: String? = null
)

sealed interface ReviewUiEvent {
    data class BookingSuccess(val bookingId: String) : ReviewUiEvent
    data class WaitlistSuccess(val message: String) : ReviewUiEvent
    data object WaitlistSlotNotFull409 : ReviewUiEvent
    data object WaitlistTimeout : ReviewUiEvent
    data object NavigateBackToActivity : ReviewUiEvent
    data object NavigateBackToMatches : ReviewUiEvent
    data class ShowSnackbar(val message: String) : ReviewUiEvent
}

@HiltViewModel
class BookingReviewViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val activityRepository: ActivityRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val slotId: String = checkNotNull(savedStateHandle["slotId"]) { "slotId is required" }
    val guideId: String = checkNotNull(savedStateHandle["guideId"]) { "guideId is required" }

    private val _uiState = MutableStateFlow(
        ReviewUiState(
            slotId = slotId,
            guideId = guideId
        )
    )
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ReviewUiEvent>()
    val events: SharedFlow<ReviewUiEvent> = _events.asSharedFlow()

    init {
        loadData()
    }

    fun loadData() {
        _uiState.update { it.copy(isLoadingData = true) }
        viewModelScope.launch {
            val slot = activityRepository.getSlotById(slotId)
            val activity = activityRepository.getActivityForSlot(slotId)

            var guide = bookingRepository.getCachedMatchedGuide(guideId)
            if (guide == null) {
                // Reload from network if not in memory
                val matchesResult = bookingRepository.requestMatches(slotId)
                if (matchesResult is ApiResult.Success) {
                    guide = matchesResult.data.find { it.guideId == guideId }
                }
            }

            val deposit = if (slot != null) {
                CancellationPolicyInfo.calculateEstimatedDeposit(slot.currentPrice)
            } else {
                BigDecimal.ZERO
            }

            _uiState.update {
                it.copy(
                    isLoadingData = false,
                    slot = slot,
                    activity = activity,
                    guide = guide,
                    estimatedDeposit = deposit
                )
            }
        }
    }

    fun confirmBooking() {
        if (_uiState.value.isSubmitting) return

        _uiState.update {
            it.copy(
                isSubmitting = true,
                networkTimeoutError = false,
                recoveryMessage = null
            )
        }

        viewModelScope.launch {
            val result = bookingRepository.confirmBooking(slotId = slotId, guideId = guideId)
            _uiState.update { it.copy(isSubmitting = false) }

            when (result) {
                is ApiResult.Success -> {
                    _events.emit(ReviewUiEvent.BookingSuccess(result.data.id))
                }
                is ApiResult.Error -> {
                    handleBookingError(result)
                }
                is ApiResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            networkTimeoutError = true,
                            recoveryMessage = result.exception.message ?: "Network error"
                        )
                    }
                }
            }
        }
    }

    private suspend fun handleBookingError(errorResult: ApiResult.Error) {
        val appError = errorResult.type
        val message = errorResult.message.lowercase()

        if (appError is AppError.Conflict && (message.contains("full") || message.contains("waitlist"))) {
            // Slot is full -> show dialog offering waitlist
            _uiState.update { it.copy(showSlotFullDialog = true) }
        } else if (appError is AppError.Conflict || appError is AppError.Validation) {
            // Other 409/400 (guide not available, already booked, etc.)
            _uiState.update {
                it.copy(
                    showGeneralErrorDialog = true,
                    generalErrorMessage = errorResult.message
                )
            }
        } else if (appError is AppError.Timeout || appError is AppError.NoInternet) {
            // Timeout/network -> Show retry with recovery check
            _uiState.update {
                it.copy(
                    networkTimeoutError = true,
                    recoveryMessage = errorResult.message
                )
            }
        } else {
            _events.emit(ReviewUiEvent.ShowSnackbar(errorResult.message))
        }
    }

    fun retryConfirmWithRecoveryCheck() {
        if (_uiState.value.isSubmitting) return

        _uiState.update {
            it.copy(
                isSubmitting = true,
                networkTimeoutError = false
            )
        }

        viewModelScope.launch {
            // Recovery check: Before retrying, refresh /api/bookings/my and check if booking already exists
            bookingRepository.refreshMyBookings()
            val existing = bookingRepository.getBookingForSlot(slotId)
            if (existing != null) {
                _uiState.update { it.copy(isSubmitting = false) }
                _events.emit(ReviewUiEvent.BookingSuccess(existing.id))
                return@launch
            }

            // No existing booking -> proceed to call confirmBooking
            val result = bookingRepository.confirmBooking(slotId = slotId, guideId = guideId)
            _uiState.update { it.copy(isSubmitting = false) }

            when (result) {
                is ApiResult.Success -> {
                    _events.emit(ReviewUiEvent.BookingSuccess(result.data.id))
                }
                is ApiResult.Error -> {
                    handleBookingError(result)
                }
                is ApiResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            networkTimeoutError = true,
                            recoveryMessage = result.exception.message ?: "Network error"
                        )
                    }
                }
            }
        }
    }

    fun onSlotFullDismiss(action: SlotFullAction) {
        _uiState.update { it.copy(showSlotFullDialog = false) }
        when (action) {
            SlotFullAction.JOIN_WAITLIST -> {
                _uiState.update { it.copy(showWaitlistConfirmDialog = true) }
            }
            SlotFullAction.BACK_TO_ACTIVITY -> {
                viewModelScope.launch {
                    _events.emit(ReviewUiEvent.NavigateBackToActivity)
                }
            }
        }
    }

    fun dismissWaitlistConfirmation() {
        _uiState.update { it.copy(showWaitlistConfirmDialog = false) }
    }

    fun confirmJoinWaitlist() {
        if (_uiState.value.isWaitlistSubmitting) return
        val guide = _uiState.value.guide ?: return

        _uiState.update { it.copy(isWaitlistSubmitting = true) }

        viewModelScope.launch {
            val slot = _uiState.value.slot
            val activity = _uiState.value.activity

            val result = bookingRepository.joinWaitlist(
                slotId = slotId,
                guideId = guide.guideId,
                activityTitle = activity?.title ?: "Trek Activity",
                slotDate = slot?.date ?: LocalDateTime.now()
            )

            _uiState.update {
                it.copy(
                    isWaitlistSubmitting = false,
                    showWaitlistConfirmDialog = false
                )
            }

            when (result) {
                is ApiResult.Success -> {
                    _events.emit(ReviewUiEvent.WaitlistSuccess(result.data))
                }
                is ApiResult.Error -> {
                    val error = result.type
                    if (error is AppError.Conflict) {
                        _events.emit(ReviewUiEvent.WaitlistSlotNotFull409)
                    } else if (error is AppError.Timeout) {
                        _events.emit(ReviewUiEvent.WaitlistTimeout)
                    } else {
                        _events.emit(ReviewUiEvent.ShowSnackbar(result.message))
                    }
                }
                is ApiResult.NetworkError -> {
                    _events.emit(ReviewUiEvent.WaitlistTimeout)
                }
            }
        }
    }

    fun dismissGeneralErrorDialog() {
        _uiState.update { it.copy(showGeneralErrorDialog = false, generalErrorMessage = null) }
        viewModelScope.launch {
            _events.emit(ReviewUiEvent.NavigateBackToMatches)
        }
    }
}

enum class SlotFullAction {
    JOIN_WAITLIST,
    BACK_TO_ACTIVITY
}
