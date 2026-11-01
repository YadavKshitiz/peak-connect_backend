package com.peakconnect.app.feature.trekker.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakconnect.app.R
import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.data.local.dao.ActivityDao
import com.peakconnect.app.data.local.entity.BookingEntity
import com.peakconnect.app.data.repository.BookingRepository
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.CancellationPolicyType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import com.peakconnect.app.data.local.dao.UserContentDao
import com.peakconnect.app.data.local.entity.WaitlistEntryEntity
import com.peakconnect.app.domain.model.WaitlistStatus
import javax.inject.Inject

enum class BookingTab(val labelRes: Int) {
    ACTIVE(R.string.bookings_tab_active),
    UPCOMING(R.string.bookings_tab_upcoming),
    PAST(R.string.bookings_tab_past),
    CANCELLED(R.string.bookings_tab_cancelled),
    WAITLIST(R.string.bookings_tab_waitlist)
}

data class BookingListItem(
    val booking: BookingEntity,
    val activityLocation: String? = null,
    val activityImageUrl: String? = null,
    val activityCancellationPolicy: CancellationPolicyType? = null
)

data class PromotedBannerState(
    val bookingId: String,
    val guideName: String?
)

data class BookingListUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedTab: BookingTab = BookingTab.ACTIVE,
    val activeBookings: List<BookingListItem> = emptyList(),
    val upcomingBookings: List<BookingListItem> = emptyList(),
    val pastBookings: List<BookingListItem> = emptyList(),
    val cancelledBookings: List<BookingListItem> = emptyList(),
    val waitlistEntries: List<WaitlistEntryEntity> = emptyList(),
    val promotedBanner: PromotedBannerState? = null,
    val activeCount: Int = 0,
    val errorMessage: String? = null
) {
    val currentList: List<BookingListItem>
        get() = when (selectedTab) {
            BookingTab.ACTIVE -> activeBookings
            BookingTab.UPCOMING -> upcomingBookings
            BookingTab.PAST -> pastBookings
            BookingTab.CANCELLED -> cancelledBookings
            BookingTab.WAITLIST -> emptyList()
        }
}

@HiltViewModel
class BookingListViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val activityDao: ActivityDao,
    private val userContentDao: UserContentDao
) : ViewModel() {

    // Clock supplier for deterministic unit testing
    var nowSupplier: () -> LocalDateTime = { LocalDateTime.now() }

    private val dismissedBannerBookingIds = mutableSetOf<String>()

    private val _uiState = MutableStateFlow(BookingListUiState())
    val uiState: StateFlow<BookingListUiState> = _uiState.asStateFlow()

    init {
        observeBookings()
        observeWaitlist()
        refresh()
    }

    private fun observeWaitlist() {
        viewModelScope.launch {
            userContentDao.getWaitlistEntriesFlow().collect { entries ->
                val activePromotion = entries.firstOrNull {
                    it.status == WaitlistStatus.PROMOTED &&
                            it.bookingId != null &&
                            !dismissedBannerBookingIds.contains(it.bookingId)
                }
                val bannerState = activePromotion?.let {
                    PromotedBannerState(
                        bookingId = it.bookingId!!,
                        guideName = it.guideName
                    )
                }
                _uiState.update { current ->
                    current.copy(
                        waitlistEntries = entries,
                        promotedBanner = bannerState
                    )
                }
            }
        }
    }

    fun dismissPromotedBanner(bookingId: String) {
        dismissedBannerBookingIds.add(bookingId)
        val nextPromotion = _uiState.value.waitlistEntries.firstOrNull {
            it.status == WaitlistStatus.PROMOTED &&
                    it.bookingId != null &&
                    !dismissedBannerBookingIds.contains(it.bookingId)
        }
        _uiState.update {
            it.copy(
                promotedBanner = nextPromotion?.let { promo ->
                    PromotedBannerState(promo.bookingId!!, promo.guideName)
                }
            )
        }
    }

    fun removeFromWaitlist(slotId: String) {
        viewModelScope.launch {
            userContentDao.deleteWaitlistEntry(slotId)
        }
    }

    private fun observeBookings() {
        viewModelScope.launch {
            combine(
                bookingRepository.getMyBookingsFlow(),
                activityDao.getAllActivitiesFlow()
            ) { bookings, activities ->
                val activityMap = activities.associateBy { it.title.lowercase().trim() }
                val now = nowSupplier()

                val items = bookings.map { b ->
                    val matchedActivity = activityMap[b.activityTitle.lowercase().trim()]
                    BookingListItem(
                        booking = b,
                        activityLocation = matchedActivity?.location,
                        activityImageUrl = null, // resolved via ImageUrlResolver
                        activityCancellationPolicy = matchedActivity?.cancellationPolicy
                    )
                }

                // Tab grouping
                val active = items.filter { item ->
                    item.booking.status == BookingStatus.PENDING_GUIDE_RESPONSE ||
                            item.booking.status == BookingStatus.AWAITING_PAYMENT
                }.sortedBy { it.booking.date }

                val upcoming = items.filter { item ->
                    item.booking.status == BookingStatus.CONFIRMED && !item.booking.date.isBefore(now)
                }.sortedBy { it.booking.date } // Upcoming ascending

                val past = items.filter { item ->
                    (item.booking.status == BookingStatus.CONFIRMED && item.booking.date.isBefore(now)) ||
                            item.booking.status == BookingStatus.NO_SHOW
                }.sortedByDescending { it.booking.date } // Past descending

                val cancelled = items.filter { item ->
                    item.booking.status == BookingStatus.CANCELLED ||
                            item.booking.status == BookingStatus.REMATCH_FAILED
                }.sortedByDescending { it.booking.date }

                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        activeBookings = active,
                        upcomingBookings = upcoming,
                        pastBookings = past,
                        cancelledBookings = cancelled,
                        activeCount = active.size
                    )
                }
            }.collect {}
        }
    }

    fun selectTab(tab: BookingTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            val result = bookingRepository.refreshMyBookings()
            _uiState.update { current ->
                current.copy(
                    isRefreshing = false,
                    errorMessage = when (result) {
                        is ApiResult.Success -> null
                        is ApiResult.Error -> result.message
                        is ApiResult.NetworkError -> result.exception.message ?: "Network error"
                    }
                )
            }
        }
    }
}
