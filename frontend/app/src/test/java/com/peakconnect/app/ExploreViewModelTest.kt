package com.peakconnect.app

import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.session.SessionManager
import com.peakconnect.app.core.session.SessionState
import com.peakconnect.app.data.local.dao.ActivityFilter
import com.peakconnect.app.data.local.dao.ActivitySortOption
import com.peakconnect.app.data.local.entity.SearchHistoryEntity
import com.peakconnect.app.data.repository.ActivityRepository
import com.peakconnect.app.domain.model.DifficultyLevel
import com.peakconnect.app.domain.model.Role
import com.peakconnect.app.feature.explore.ExploreViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExploreViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val activityRepository: ActivityRepository = mockk(relaxed = true)
    private val sessionManager: SessionManager = mockk(relaxed = true)

    private val sessionStateFlow = MutableStateFlow<SessionState>(
        SessionState.LoggedIn(
            role = Role.TREKKER,
            userId = "trekker-1",
            email = "alex.smith@example.com",
            name = "Alex Smith"
        )
    )
    private val favouritesFlow = MutableStateFlow(setOf("act-1"))
    private val searchHistoryFlow = MutableStateFlow(listOf(SearchHistoryEntity("manali", 1000L)))

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { sessionManager.sessionState } returns sessionStateFlow
        every { activityRepository.getFavouritesSetFlow() } returns favouritesFlow
        every { activityRepository.getSearchHistory() } returns searchHistoryFlow
        every { activityRepository.getDistinctLocations() } returns MutableStateFlow(listOf("Manali", "Pokhara"))
        every { activityRepository.getRecentlyViewedActivities() } returns MutableStateFlow(emptyList())
        coEvery { activityRepository.hasCachedActivities() } returns true
        coEvery { activityRepository.refreshActivities() } returns ApiResult.Success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `greeting extracts first name from logged in session`() = runTest(testDispatcher) {
        val viewModel = ExploreViewModel(activityRepository, sessionManager)
        advanceUntilIdle()

        assertEquals("Alex", viewModel.userFirstName.value)
    }

    @Test
    fun `search query is debounced by 300ms before updating filter`() = runTest(testDispatcher) {
        val viewModel = ExploreViewModel(activityRepository, sessionManager)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("Triund")
        // Before 300ms, filter query is still not updated
        advanceTimeBy(100L)
        assertNull(viewModel.filter.value.query)

        // After 300ms elapses, filter query updates
        advanceTimeBy(250L)
        assertEquals("Triund", viewModel.filter.value.query)
    }

    @Test
    fun `search submission updates filter immediately and records search history`() = runTest(testDispatcher) {
        val viewModel = ExploreViewModel(activityRepository, sessionManager)
        advanceUntilIdle()

        viewModel.onSearchSubmitted("Hampta Pass")
        advanceUntilIdle()

        assertEquals("Hampta Pass", viewModel.filter.value.query)
        coVerify { activityRepository.saveSearchTerm("Hampta Pass") }
    }

    @Test
    fun `clicking a search chip updates query and records term`() = runTest(testDispatcher) {
        val viewModel = ExploreViewModel(activityRepository, sessionManager)
        advanceUntilIdle()

        viewModel.onSearchTermClicked("manali")
        advanceUntilIdle()

        assertEquals("manali", viewModel.filter.value.query)
        coVerify { activityRepository.saveSearchTerm("manali") }
    }

    @Test
    fun `filters applied, removed and reset properly`() = runTest(testDispatcher) {
        val viewModel = ExploreViewModel(activityRepository, sessionManager)
        advanceUntilIdle()

        val complexFilter = ActivityFilter(
            difficulties = setOf(DifficultyLevel.EASY, DifficultyLevel.MODERATE),
            location = "Manali",
            hasSlotOnDate = "2026-07-01",
            sortOption = ActivitySortOption.PRICE_LOW_TO_HIGH
        )
        viewModel.applyFilter(complexFilter)
        assertEquals(complexFilter, viewModel.filter.value)

        // Remove single difficulty
        viewModel.removeDifficultyFilter(DifficultyLevel.EASY)
        assertEquals(setOf(DifficultyLevel.MODERATE), viewModel.filter.value.difficulties)

        // Remove location
        viewModel.removeLocationFilter()
        assertNull(viewModel.filter.value.location)

        // Remove date
        viewModel.removeDateFilter()
        assertNull(viewModel.filter.value.hasSlotOnDate)

        // Reset sort
        viewModel.resetSortOption()
        assertEquals(ActivitySortOption.TITLE, viewModel.filter.value.sortOption)

        // Clear all
        viewModel.clearAllFilters()
        assertEquals(ActivityFilter(), viewModel.filter.value)
        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun `toggleFavourite calls repository`() = runTest(testDispatcher) {
        val viewModel = ExploreViewModel(activityRepository, sessionManager)
        advanceUntilIdle()

        viewModel.toggleFavourite("act-2")
        advanceUntilIdle()

        coVerify { activityRepository.toggleFavourite("act-2") }
    }

    @Test
    fun `getMatchingCount queries repository`() = runTest(testDispatcher) {
        val viewModel = ExploreViewModel(activityRepository, sessionManager)
        coEvery { activityRepository.countActivities(any()) } returns 5

        val count = viewModel.getMatchingCount(ActivityFilter(location = "Manali"))
        assertEquals(5, count)
    }
}
