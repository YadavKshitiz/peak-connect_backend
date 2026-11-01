package com.peakconnect.app.feature.guide.mappicker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapPickerUiState(
    val currentLat: Double,
    val currentLng: Double,
    val zoom: Double,
    val hasPin: Boolean,
    val searchQuery: String = "",
    val searchResults: List<NominatimPlace> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null
)

@OptIn(FlowPreview::class)
@HiltViewModel
class MapPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val nominatimSearchService: NominatimSearchService
) : ViewModel() {

    private val initialLat: Double? = savedStateHandle["initialLat"]
    private val initialLng: Double? = savedStateHandle["initialLng"]

    private val defaultLat = 22.5
    private val defaultLng = 79.0
    private val defaultZoom = 4.5

    private val _uiState = MutableStateFlow(
        MapPickerUiState(
            currentLat = initialLat ?: defaultLat,
            currentLng = initialLng ?: defaultLng,
            zoom = if (initialLat != null && initialLng != null) 14.0 else defaultZoom,
            hasPin = initialLat != null && initialLng != null
        )
    )
    val uiState: StateFlow<MapPickerUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        // Debounce place searches at 800ms
        queryFlow
            .debounce(800L)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.trim().length >= 3) {
                    performSearch(query)
                } else {
                    _uiState.update { it.copy(searchResults = emptyList(), isSearching = false, searchError = null) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onMapTapped(lat: Double, lng: Double) {
        _uiState.update {
            it.copy(
                currentLat = lat,
                currentLng = lng,
                hasPin = true,
                searchResults = emptyList()
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        queryFlow.value = query
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList(), isSearching = false, searchError = null) }
        queryFlow.value = ""
    }

    fun onPlaceSelected(place: NominatimPlace) {
        _uiState.update {
            it.copy(
                currentLat = place.latitude,
                currentLng = place.longitude,
                zoom = 14.0,
                hasPin = true,
                searchQuery = "",
                searchResults = emptyList(),
                isSearching = false,
                searchError = null
            )
        }
        queryFlow.value = ""
    }

    fun onMyLocationObtained(lat: Double, lng: Double) {
        _uiState.update {
            it.copy(
                currentLat = lat,
                currentLng = lng,
                zoom = 14.0,
                hasPin = true,
                searchResults = emptyList()
            )
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchError = null) }
            val results = nominatimSearchService.search(query)
            _uiState.update {
                it.copy(
                    isSearching = false,
                    searchResults = results,
                    searchError = if (results.isEmpty()) "No places found" else null
                )
            }
        }
    }
}
