package com.peakconnect.app

import androidx.lifecycle.SavedStateHandle
import com.peakconnect.app.feature.guide.mappicker.MapPickerViewModel
import com.peakconnect.app.feature.guide.mappicker.NominatimPlace
import com.peakconnect.app.feature.guide.mappicker.NominatimSearchService
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapPickerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val nominatimSearchService: NominatimSearchService = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state with arguments sets initial pin and zoom`() = runTest {
        val savedStateHandle = SavedStateHandle(
            mapOf("initialLat" to 32.2396, "initialLng" to 77.1887)
        )
        val viewModel = MapPickerViewModel(savedStateHandle, nominatimSearchService)
        advanceUntilIdle()

        assertEquals(32.2396, viewModel.uiState.value.currentLat, 0.0001)
        assertEquals(77.1887, viewModel.uiState.value.currentLng, 0.0001)
        assertTrue(viewModel.uiState.value.hasPin)
        assertEquals(14.0, viewModel.uiState.value.zoom, 0.01)
    }

    @Test
    fun `initial state without arguments sets default India center and zoom`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = MapPickerViewModel(savedStateHandle, nominatimSearchService)
        advanceUntilIdle()

        assertEquals(22.5, viewModel.uiState.value.currentLat, 0.0001)
        assertEquals(79.0, viewModel.uiState.value.currentLng, 0.0001)
        assertFalse(viewModel.uiState.value.hasPin)
        assertEquals(4.5, viewModel.uiState.value.zoom, 0.01)
    }

    @Test
    fun `onMapTapped places pin at tapped coordinates`() = runTest {
        val viewModel = MapPickerViewModel(SavedStateHandle(), nominatimSearchService)
        advanceUntilIdle()

        viewModel.onMapTapped(30.0, 78.0)
        advanceUntilIdle()

        assertEquals(30.0, viewModel.uiState.value.currentLat, 0.0001)
        assertEquals(78.0, viewModel.uiState.value.currentLng, 0.0001)
        assertTrue(viewModel.uiState.value.hasPin)
    }

    @Test
    fun `onMyLocationObtained updates pin and zooms in`() = runTest {
        val viewModel = MapPickerViewModel(SavedStateHandle(), nominatimSearchService)
        advanceUntilIdle()

        viewModel.onMyLocationObtained(32.0, 77.0)
        advanceUntilIdle()

        assertEquals(32.0, viewModel.uiState.value.currentLat, 0.0001)
        assertEquals(77.0, viewModel.uiState.value.currentLng, 0.0001)
        assertEquals(14.0, viewModel.uiState.value.zoom, 0.01)
        assertTrue(viewModel.uiState.value.hasPin)
    }

    @Test
    fun `onPlaceSelected updates location, pin, and clears search`() = runTest {
        val viewModel = MapPickerViewModel(SavedStateHandle(), nominatimSearchService)
        advanceUntilIdle()

        val place = NominatimPlace(
            placeId = 12345,
            displayName = "Manali, Himachal Pradesh",
            latitude = 32.2396,
            longitude = 77.1887
        )
        viewModel.onPlaceSelected(place)
        advanceUntilIdle()

        assertEquals(32.2396, viewModel.uiState.value.currentLat, 0.0001)
        assertEquals(77.1887, viewModel.uiState.value.currentLng, 0.0001)
        assertTrue(viewModel.uiState.value.hasPin)
        assertEquals("", viewModel.uiState.value.searchQuery)
        assertTrue(viewModel.uiState.value.searchResults.isEmpty())
    }

    @Test
    fun `clearSearch resets query and search results`() = runTest {
        val viewModel = MapPickerViewModel(SavedStateHandle(), nominatimSearchService)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("Manali")
        advanceUntilIdle()
        assertEquals("Manali", viewModel.uiState.value.searchQuery)

        viewModel.clearSearch()
        advanceUntilIdle()
        assertEquals("", viewModel.uiState.value.searchQuery)
        assertTrue(viewModel.uiState.value.searchResults.isEmpty())
    }
}

