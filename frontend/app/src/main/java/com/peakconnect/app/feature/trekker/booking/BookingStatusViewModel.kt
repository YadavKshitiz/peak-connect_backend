package com.peakconnect.app.feature.trekker.booking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.network.AppError
import com.peakconnect.app.data.local.dao.ActivityDao
import com.peakconnect.app.data.local.dao.UserContentDao
import com.peakconnect.app.data.local.entity.ActivityEntity
import com.peakconnect.app.data.local.entity.BookingEntity
import com.peakconnect.app.data.local.entity.SlotEntity
import com.peakconnect.app.data.repository.BookingRepository
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServerCancellationResult(
    val refundAmount: Double?,
    val refundPercentage: Double?
)

data class BookingStatusUiState(
    val isLoading: Boolean = true,
    val booking: BookingEntity? = null,
    val slot: SlotEntity? = null,
    val activity: ActivityEntity? = null,
    val isPolling: Boolean = false,
    val isRefreshing: Boolean = false,
    val isCancelling: Boolean = false,
    val cancellationResult: ServerCancellationResult? = null,
    val cancellationError: String? = null,
    val showCancelDialog: Boolean = false,
    val showCancelSheet: Boolean = false,
    val isFromWaitlist: Boolean = false
)

sealed interface BookingStatusEvent {
    data class StatusChanged(val newStatus: BookingStatus) : BookingStatusEvent
}

@HiltViewModel
class BookingStatusViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookingRepository: BookingRepository,
    private val activityDao: ActivityDao,
    private val userContentDao: UserContentDao,
    private val activeScreenTracker: com.peakconnect.app.core.notifications.ActiveScreenTracker = com.peakconnect.app.core.notifications.ActiveScreenTracker(),
    private val networkRefreshNotifier: com.peakconnect.app.core.network.NetworkRefreshNotifier? = null
) : ViewModel() {

    val bookingId: String = checkNotNull(savedStateHandle["bookingId"])

    private val _uiState = MutableStateFlow(BookingStatusUiState())
    val uiState: StateFlow<BookingStatusUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<BookingStatusEvent>()
    val events: SharedFlow<BookingStatusEvent> = _events.asSharedFlow()

    private var pollingJob: Job? = null
    private var lastKnownStatus: BookingStatus? = null

    init {
        activeScreenTracker.onBookingScreenEntered(bookingId)
        observeBooking()
        refresh()

        // Auto-refresh when back online (Requirement 1 & 2)
        networkRefreshNotifier?.refreshEvents?.onEach {
            refresh()
        }?.launchIn(viewModelScope)
    }

    private fun observeBooking() {
        viewModelScope.launch {
            bookingRepository.getBookingFlow(bookingId).collect { booking ->
                if (booking != null) {
                    val prevStatus = lastKnownStatus
                    val currentStatus = booking.status
                    lastKnownStatus = currentStatus

                    if (prevStatus != null && prevStatus != currentStatus) {
                        _events.emit(BookingStatusEvent.StatusChanged(currentStatus))
                        if (isTerminalStatus(currentStatus)) {
                            stopPolling()
                        }
                    }

                    // Load slot & activity for pricing, policy, and location details
                    val slot = activityDao.getSlotById(booking.slotId)
                    val activity = slot?.let { activityDao.getActivityById(it.activityId) }
                        ?: activityDao.getActivityForSlot(booking.slotId)

                    val waitlistEntry = userContentDao.getWaitlistEntryByBookingId(booking.id)
                        ?: userContentDao.getWaitlistEntry(booking.slotId)?.takeIf { it.bookingId == booking.id }
                    val isFromWaitlist = waitlistEntry != null

                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            booking = booking,
                            slot = slot,
                            activity = activity,
                            isFromWaitlist = isFromWaitlist
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        val currentStatus = _uiState.value.booking?.status
        if (currentStatus != null && isTerminalStatus(currentStatus)) {
            return
        }

        _uiState.update { it.copy(isPolling = true) }
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(5000L)
                val status = _uiState.value.booking?.status
                if (status != null && isTerminalStatus(status)) {
                    stopPolling()
                    break
                }
                bookingRepository.refreshMyBookings()
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        _uiState.update { it.copy(isPolling = false) }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            bookingRepository.refreshMyBookings()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun showCancelSheet(show: Boolean) {
        _uiState.update { it.copy(showCancelSheet = show, cancellationError = null) }
    }

    fun showCancelConfirmDialog(show: Boolean) {
        _uiState.update { it.copy(showCancelDialog = show) }
    }

    fun cancelBooking() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCancelling = true,
                    showCancelDialog = false,
                    cancellationError = null
                )
            }
            val result = bookingRepository.cancelBooking(bookingId)
            when (result) {
                is ApiResult.Success -> {
                    val b = result.data
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            cancellationResult = ServerCancellationResult(
                                refundAmount = b.refundAmount,
                                refundPercentage = b.refundPercentage
                            )
                        )
                    }
                    bookingRepository.refreshMyBookings()
                }

                is ApiResult.Error -> {
                    val errorMsg = if (result.type is AppError.Conflict ||
                        result.message.contains("already cancelled", ignoreCase = true)
                    ) {
                        "This booking is already cancelled."
                    } else {
                        result.message
                    }
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            cancellationError = errorMsg
                        )
                    }
                }

                is ApiResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            cancellationError = result.exception.message ?: "Network error"
                        )
                    }
                }
            }
        }
    }

    fun clearCancellationResult() {
        _uiState.update { it.copy(cancellationResult = null, showCancelSheet = false) }
    }

    private fun isTerminalStatus(status: BookingStatus): Boolean {
        return status == BookingStatus.CONFIRMED ||
                status == BookingStatus.CANCELLED ||
                status == BookingStatus.NO_SHOW ||
                status == BookingStatus.REMATCH_FAILED
    }

    override fun onCleared() {
        super.onCleared()
        activeScreenTracker.onBookingScreenExited(bookingId)
    }
}
