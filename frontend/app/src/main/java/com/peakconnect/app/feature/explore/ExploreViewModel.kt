package com.peakconnect.app.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.session.SessionManager
import com.peakconnect.app.core.session.SessionState
import com.peakconnect.app.data.local.dao.ActivityFilter
import com.peakconnect.app.data.local.dao.ActivitySortOption
import com.peakconnect.app.data.local.entity.ActivityEntity
import com.peakconnect.app.data.local.entity.SearchHistoryEntity
import com.peakconnect.app.data.repository.ActivityRepository
import com.peakconnect.app.domain.model.DifficultyLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchFocused = MutableStateFlow(false)
    val isSearchFocused: StateFlow<Boolean> = _isSearchFocused.asStateFlow()

    private val _filter = MutableStateFlow(ActivityFilter())
    val filter: StateFlow<ActivityFilter> = _filter.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isFirstLoading = MutableStateFlow(false)
    val isFirstLoading: StateFlow<Boolean> = _isFirstLoading.asStateFlow()

    private val _hasCachedActivities = MutableStateFlow(false)
    val hasCachedActivities: StateFlow<Boolean> = _hasCachedActivities.asStateFlow()

    private val _snackBarMessage = MutableSharedFlow<String>()
    val snackBarMessage: SharedFlow<String> = _snackBarMessage.asSharedFlow()

    private var lastRefreshTimestamp: Long = 0L

    val userFirstName: StateFlow<String> = sessionManager.sessionState.map { state ->
        when (state) {
            is SessionState.LoggedIn -> {
                val full = state.name?.takeIf { it.isNotBlank() }
                    ?: state.email.substringBefore('@')
                full.trim().split(" ").firstOrNull() ?: "Trekker"
            }
            else -> "Trekker"
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "Trekker")

    val pagedActivities: Flow<PagingData<ActivityEntity>> = _filter
        .flatMapLatest { f ->
            Pager(
                config = PagingConfig(
                    pageSize = 20,
                    enablePlaceholders = false,
                    prefetchDistance = 6
                )
            ) {
                activityRepository.getActivitiesPagingSource(f)
            }.flow
        }
        .cachedIn(viewModelScope)

    val distinctLocations: StateFlow<List<String>> = activityRepository.getDistinctLocations()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentlyViewed: StateFlow<List<ActivityEntity>> = activityRepository.getRecentlyViewedActivities()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val searchHistory: StateFlow<List<SearchHistoryEntity>> = activityRepository.getSearchHistory()
        .map { it.take(8) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favouritesSet: StateFlow<Set<String>> = activityRepository.getFavouritesSetFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    init {
        // Setup 300ms debounce on search query
        _searchQuery
            .debounce(300L)
            .distinctUntilChanged()
            .onEach { query ->
                _filter.value = _filter.value.copy(
                    query = if (query.isBlank()) null else query.trim()
                )
            }
            .launchIn(viewModelScope)

        // Initial check and background refresh
        viewModelScope.launch {
            val cached = activityRepository.hasCachedActivities()
            _hasCachedActivities.value = cached
            if (!cached) {
                _isFirstLoading.value = true
            }
            refreshActivities(isManual = false)
            _isFirstLoading.value = false
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onSearchFocusChanged(focused: Boolean) {
        _isSearchFocused.value = focused
    }

    fun onSearchSubmitted(term: String) {
        if (term.isNotBlank()) {
            _searchQuery.value = term
            _filter.value = _filter.value.copy(query = term.trim())
            viewModelScope.launch {
                activityRepository.saveSearchTerm(term)
            }
        }
    }

    fun onSearchTermClicked(term: String) {
        _searchQuery.value = term
        _filter.value = _filter.value.copy(query = term.trim())
        viewModelScope.launch {
            activityRepository.saveSearchTerm(term)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            activityRepository.clearSearchHistory()
        }
    }

    fun applyFilter(newFilter: ActivityFilter) {
        _filter.value = newFilter
    }

    fun removeDifficultyFilter(level: DifficultyLevel) {
        val currentDifficulties = _filter.value.difficulties ?: return
        val newSet = currentDifficulties - level
        _filter.value = _filter.value.copy(
            difficulties = if (newSet.isEmpty()) null else newSet
        )
    }

    fun removeLocationFilter() {
        _filter.value = _filter.value.copy(location = null)
    }

    fun removeDateFilter() {
        _filter.value = _filter.value.copy(hasSlotOnDate = null)
    }

    fun resetSortOption() {
        _filter.value = _filter.value.copy(sortOption = ActivitySortOption.TITLE)
    }

    fun clearAllFilters() {
        _searchQuery.value = ""
        _filter.value = ActivityFilter()
    }

    suspend fun getMatchingCount(filter: ActivityFilter): Int {
        return activityRepository.countActivities(filter)
    }

    fun toggleFavourite(activityId: String) {
        viewModelScope.launch {
            activityRepository.toggleFavourite(activityId)
        }
    }

    fun onActivityClicked(activityId: String) {
        viewModelScope.launch {
            activityRepository.recordRecentlyViewed(activityId)
            val currentQuery = _searchQuery.value
            if (currentQuery.isNotBlank()) {
                activityRepository.saveSearchTerm(currentQuery)
            }
        }
    }

    fun refreshActivities(isManual: Boolean = false) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = activityRepository.refreshActivities()
            _isRefreshing.value = false

            when (result) {
                is ApiResult.Success -> {
                    lastRefreshTimestamp = System.currentTimeMillis()
                    _hasCachedActivities.value = true
                }
                is ApiResult.Error, is ApiResult.NetworkError -> {
                    val cached = activityRepository.hasCachedActivities()
                    _hasCachedActivities.value = cached
                    if (cached && isManual) {
                        _snackBarMessage.emit("Couldn't refresh")
                    }
                }
            }
        }
    }

    fun onResume() {
        val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000L)
        if (lastRefreshTimestamp in 1..fiveMinutesAgo) {
            refreshActivities(isManual = false)
        }
    }
}
