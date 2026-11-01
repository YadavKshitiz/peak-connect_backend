package com.peakconnect.app.feature.guide.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.network.AppError
import com.peakconnect.app.data.local.dao.ActivityDao
import com.peakconnect.app.data.local.entity.GuideBookingEntity
import com.peakconnect.app.data.repository.BookingRepository
import com.peakconnect.app.data.repository.GuideRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class GuideBookingDetailUiState(
    val isLoading: Boolean = true,
    val isActing: Boolean = false,
    val booking: GuideBookingEntity? = null,
    val activityLocation: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showDeclineDialog: Boolean = false,
    val showNoShowDialog: Boolean = false
)

@HiltViewModel
class GuideBookingDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val guideRepository: GuideRepository,
    private val bookingRepository: BookingRepository,
    private val activityDao: ActivityDao,
    private val activeScreenTracker: com.peakconnect.app.core.notifications.ActiveScreenTracker = com.peakconnect.app.core.notifications.ActiveScreenTracker()
) : ViewModel() {

    val bookingId: String = checkNotNull(savedStateHandle["bookingId"])

    var nowSupplier: () -> LocalDateTime = { LocalDateTime.now() }

    private val _uiState = MutableStateFlow(GuideBookingDetailUiState())
    val uiState: StateFlow<GuideBookingDetailUiState> = _uiState.asStateFlow()

    init {
        activeScreenTracker.onBookingScreenEntered(bookingId)
        observeBooking()
        refresh()
    }

    private fun observeBooking() {
        viewModelScope.launch {
            guideRepository.getGuideBookingFlow(bookingId).collect { booking ->
                if (booking != null) {
                    val slot = activityDao.getSlotById(booking.slotId)
                    val activity = slot?.let { activityDao.getActivityById(it.activityId) }
                        ?: activityDao.getActivityForSlot(booking.slotId)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            booking = booking,
                            activityLocation = activity?.location
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            guideRepository.refreshGuideBookings()
        }
    }

    fun showDeclineDialog(show: Boolean) {
        _uiState.update { it.copy(showDeclineDialog = show) }
    }

    fun showNoShowDialog(show: Boolean) {
        _uiState.update { it.copy(showNoShowDialog = show) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun acceptBooking() {
        if (_uiState.value.isActing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isActing = true, errorMessage = null) }
            val result = bookingRepository.acceptBooking(bookingId)
            guideRepository.refreshGuideBookings()
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isActing = false,
                            successMessage = "Booking request accepted"
                        )
                    }
                }
                is ApiResult.Error -> {
                    val msg = if (result.type is AppError.Conflict || result.type is AppError.Validation) {
                        "This booking request has expired or has already been responded to."
                    } else {
                        result.message
                    }
                    _uiState.update {
                        it.copy(
                            isActing = false,
                            errorMessage = msg
                        )
                    }
                }
                is ApiResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            isActing = false,
                            errorMessage = result.exception.message ?: "Network error"
                        )
                    }
                }
            }
        }
    }

    fun declineBooking() {
        if (_uiState.value.isActing) return
        _uiState.update { it.copy(showDeclineDialog = false) }
        viewModelScope.launch {
            _uiState.update { it.copy(isActing = true, errorMessage = null) }
            val result = bookingRepository.declineBooking(bookingId)
            guideRepository.refreshGuideBookings()
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isActing = false,
                            successMessage = "Booking request declined"
                        )
                    }
                }
                is ApiResult.Error -> {
                    val msg = if (result.type is AppError.Conflict || result.type is AppError.Validation) {
                        "This booking request has expired or has already been responded to."
                    } else {
                        result.message
                    }
                    _uiState.update {
                        it.copy(
                            isActing = false,
                            errorMessage = msg
                        )
                    }
                }
                is ApiResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            isActing = false,
                            errorMessage = result.exception.message ?: "Network error"
                        )
                    }
                }
            }
        }
    }

    fun markNoShow() {
        if (_uiState.value.isActing) return
        _uiState.update { it.copy(showNoShowDialog = false) }
        viewModelScope.launch {
            _uiState.update { it.copy(isActing = true, errorMessage = null) }
            val result = bookingRepository.markNoShow(bookingId)
            guideRepository.refreshGuideBookings()
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isActing = false,
                            successMessage = "Trekker marked as no-show"
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isActing = false,
                            errorMessage = result.message
                        )
                    }
                }
                is ApiResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            isActing = false,
                            errorMessage = result.exception.message ?: "Network error"
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        activeScreenTracker.onBookingScreenExited(bookingId)
    }
}
