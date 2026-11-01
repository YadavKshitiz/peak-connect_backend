package com.peakconnect.app.feature.guide.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.network.AppError
import com.peakconnect.app.data.local.entity.GuideBookingEntity
import com.peakconnect.app.data.local.entity.GuideProfileEntity
import com.peakconnect.app.data.repository.BookingRepository
import com.peakconnect.app.data.repository.GuideRepository
import com.peakconnect.app.domain.model.BookingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class GuideHomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val profile: GuideProfileEntity? = null,
    val pendingBookings: List<GuideBookingEntity> = emptyList(),
    val upcomingBookings: List<GuideBookingEntity> = emptyList(),
    val pendingCount: Int = 0,
    val upcomingCount: Int = 0,
    val completedCount: Int = 0,
    val isActingBookingId: String? = null,
    val declineDialogBookingId: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val isVerified: Boolean
        get() = profile?.isVerified == true

    val isProfileIncomplete: Boolean
        get() = profile != null && (profile.skills.isEmpty() || profile.languages.isEmpty() || profile.location.isNullOrBlank())
}

@HiltViewModel
class GuideHomeViewModel @Inject constructor(
    private val guideRepository: GuideRepository,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    var nowSupplier: () -> LocalDateTime = { LocalDateTime.now() }

    private val _uiState = MutableStateFlow(GuideHomeUiState())
    val uiState: StateFlow<GuideHomeUiState> = _uiState.asStateFlow()

    init {
        observeData()
        refresh()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                guideRepository.getProfileFlow(),
                guideRepository.getGuideBookingsFlow()
            ) { profile, bookings ->
                val now = nowSupplier()
                val pending = bookings.filter { it.status == BookingStatus.PENDING_GUIDE_RESPONSE }
                    .sortedBy { it.statusSinceMillis }
                val upcoming = bookings.filter { it.status == BookingStatus.CONFIRMED && !it.date.isBefore(now) }
                    .sortedBy { it.date }
                val completed = bookings.filter { it.status == BookingStatus.CONFIRMED && it.date.isBefore(now) }

                GuideHomeUiState(
                    isLoading = false,
                    isRefreshing = _uiState.value.isRefreshing,
                    profile = profile,
                    pendingBookings = pending,
                    upcomingBookings = upcoming,
                    pendingCount = pending.size,
                    upcomingCount = upcoming.size,
                    completedCount = completed.size,
                    isActingBookingId = _uiState.value.isActingBookingId,
                    declineDialogBookingId = _uiState.value.declineDialogBookingId,
                    errorMessage = _uiState.value.errorMessage,
                    successMessage = _uiState.value.successMessage
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            guideRepository.refreshProfile()
            guideRepository.refreshGuideBookings()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun showDeclineDialog(bookingId: String?) {
        _uiState.update { it.copy(declineDialogBookingId = bookingId) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun acceptBooking(bookingId: String) {
        if (_uiState.value.isActingBookingId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isActingBookingId = bookingId, errorMessage = null) }
            val result = bookingRepository.acceptBooking(bookingId)
            guideRepository.refreshGuideBookings()
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isActingBookingId = null,
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
                            isActingBookingId = null,
                            errorMessage = msg
                        )
                    }
                }
                is ApiResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            isActingBookingId = null,
                            errorMessage = result.exception.message ?: "Network error"
                        )
                    }
                }
            }
        }
    }

    fun declineBooking(bookingId: String) {
        if (_uiState.value.isActingBookingId != null) return
        _uiState.update { it.copy(declineDialogBookingId = null) }
        viewModelScope.launch {
            _uiState.update { it.copy(isActingBookingId = bookingId, errorMessage = null) }
            val result = bookingRepository.declineBooking(bookingId)
            guideRepository.refreshGuideBookings()
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isActingBookingId = null,
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
                            isActingBookingId = null,
                            errorMessage = msg
                        )
                    }
                }
                is ApiResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            isActingBookingId = null,
                            errorMessage = result.exception.message ?: "Network error"
                        )
                    }
                }
            }
        }
    }
}
