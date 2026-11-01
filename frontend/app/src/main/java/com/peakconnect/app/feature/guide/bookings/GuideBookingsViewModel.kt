package com.peakconnect.app.feature.guide.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakconnect.app.data.local.entity.GuideBookingEntity
import com.peakconnect.app.data.repository.GuideRepository
import com.peakconnect.app.domain.model.BookingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

enum class GuideBookingFilter {
    ALL,
    PENDING,
    AWAITING_PAYMENT,
    CONFIRMED,
    PAST,
    CANCELLED
}

data class GuideBookingsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedFilter: GuideBookingFilter = GuideBookingFilter.ALL,
    val bookings: List<GuideBookingEntity> = emptyList(),
    val filteredBookings: List<GuideBookingEntity> = emptyList()
)

@HiltViewModel
class GuideBookingsViewModel @Inject constructor(
    private val guideRepository: GuideRepository,
    private val networkRefreshNotifier: com.peakconnect.app.core.network.NetworkRefreshNotifier? = null
) : ViewModel() {

    var nowSupplier: () -> LocalDateTime = { LocalDateTime.now() }

    val lastUpdated: StateFlow<Long?> = guideRepository.lastUpdatedBookingsFlow
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow(GuideBookingsUiState())
    val uiState: StateFlow<GuideBookingsUiState> = _uiState.asStateFlow()

    init {
        observeBookings()
        refresh()

        // Auto-refresh when back online (Requirement 1 & 2)
        networkRefreshNotifier?.refreshEvents?.onEach {
            refresh()
        }?.launchIn(viewModelScope)
    }

    private fun observeBookings() {
        viewModelScope.launch {
            guideRepository.getGuideBookingsFlow().collect { allBookings ->
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        bookings = allBookings,
                        filteredBookings = filterBookings(allBookings, current.selectedFilter)
                    )
                }
            }
        }
    }

    fun setFilter(filter: GuideBookingFilter) {
        _uiState.update { current ->
            current.copy(
                selectedFilter = filter,
                filteredBookings = filterBookings(current.bookings, filter)
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            guideRepository.refreshGuideBookings()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun filterBookings(
        bookings: List<GuideBookingEntity>,
        filter: GuideBookingFilter
    ): List<GuideBookingEntity> {
        val now = nowSupplier()
        return when (filter) {
            GuideBookingFilter.ALL -> bookings.sortedByDescending { it.date }
            GuideBookingFilter.PENDING -> bookings.filter {
                it.status == BookingStatus.PENDING_GUIDE_RESPONSE
            }.sortedBy { it.statusSinceMillis }
            GuideBookingFilter.AWAITING_PAYMENT -> bookings.filter {
                it.status == BookingStatus.AWAITING_PAYMENT
            }.sortedByDescending { it.date }
            GuideBookingFilter.CONFIRMED -> bookings.filter {
                it.status == BookingStatus.CONFIRMED && !it.date.isBefore(now)
            }.sortedBy { it.date }
            GuideBookingFilter.PAST -> bookings.filter {
                (it.status == BookingStatus.CONFIRMED && it.date.isBefore(now)) ||
                    it.status == BookingStatus.NO_SHOW
            }.sortedByDescending { it.date }
            GuideBookingFilter.CANCELLED -> bookings.filter {
                it.status == BookingStatus.CANCELLED || it.status == BookingStatus.REMATCH_FAILED
            }.sortedByDescending { it.date }
        }
    }
}
