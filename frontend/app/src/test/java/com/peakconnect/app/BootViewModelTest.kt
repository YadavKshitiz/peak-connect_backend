package com.peakconnect.app

import app.cash.turbine.test
import com.peakconnect.app.core.navigation.AppDestination
import com.peakconnect.app.core.network.HealthStatus
import com.peakconnect.app.core.preferences.OnboardingStore
import com.peakconnect.app.core.session.SessionManager
import com.peakconnect.app.core.session.SessionState
import com.peakconnect.app.core.session.TokenStore
import com.peakconnect.app.core.util.NetworkMonitor
import com.peakconnect.app.data.repository.ActivityRepository
import com.peakconnect.app.data.repository.HealthRepository
import com.peakconnect.app.domain.model.Role
import com.peakconnect.app.feature.boot.BootUiState
import com.peakconnect.app.feature.boot.BootViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class BootViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val healthRepository: HealthRepository = mockk(relaxed = true)
    private val networkMonitor: NetworkMonitor = mockk(relaxed = true)
    private val sessionManager: SessionManager = mockk(relaxed = true)
    private val tokenStore: TokenStore = mockk(relaxed = true)
    private val activityRepository: ActivityRepository = mockk(relaxed = true)
    private val onboardingStore: OnboardingStore = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { networkMonitor.isOnline } returns flowOf(true)
        every { onboardingStore.isOnboardingCompleted() } returns flowOf(true)
        coEvery { tokenStore.getToken() } returns "valid_token"
        coEvery { sessionManager.isTokenExpired() } returns false
        every { sessionManager.sessionState } returns MutableStateFlow(
            SessionState.LoggedIn(role = Role.TREKKER, userId = "123", email = "test@trek.com")
        )
        coEvery { activityRepository.hasCachedActivities() } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): BootViewModel {
        return BootViewModel(
            healthRepository = healthRepository,
            networkMonitor = networkMonitor,
            sessionManager = sessionManager,
            tokenStore = tokenStore,
            activityRepository = activityRepository,
            onboardingStore = onboardingStore,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun `fast server answering within 3 seconds routes directly to Ready without waking screen`() = runTest(testDispatcher) {
        coEvery { healthRepository.checkHealth() } returns HealthStatus.Up

        val viewModel = createViewModel()

        viewModel.uiState.test {
            // First state might be Checking
            var item = awaitItem()
            if (item is BootUiState.Checking) {
                item = awaitItem()
            }

            assertTrue("Expected Ready state but got $item", item is BootUiState.Ready)
            val ready = item as BootUiState.Ready
            assertEquals(AppDestination.TrekkerHome, ready.destination)
            assertEquals(false, ready.offlineMode)
        }
    }

    @Test
    fun `slow server enters Waking state and automatically transitions to Ready once Up`() = runTest(testDispatcher) {
        val attempts = AtomicInteger(0)
        coEvery { healthRepository.checkHealth() } answers {
            val count = attempts.incrementAndGet()
            if (count == 1) {
                // Initial check fails / times out
                HealthStatus.Down(503, "Waking")
            } else if (count == 2) {
                // Second check also down
                HealthStatus.Down(503, "Still Waking")
            } else {
                // Third check recovers
                HealthStatus.Up
            }
        }

        val viewModel = createViewModel().apply {
            fastCheckTimeoutMs = 1000L
            pollIntervalMs = 2000L
            maxWaitMs = 10000L
        }

        viewModel.uiState.test {
            var item = awaitItem()
            if (item is BootUiState.Checking) {
                item = awaitItem()
            }

            // Must enter Waking state
            assertTrue("Expected Waking state but got $item", item is BootUiState.Waking)

            // Advance time to allow polling
            advanceTimeBy(4500L)

            // Skip any intermediate Waking timer updates until Ready
            while (item is BootUiState.Waking) {
                item = awaitItem()
            }

            assertTrue("Expected Ready state after recovery but got $item", item is BootUiState.Ready)
            assertEquals(AppDestination.TrekkerHome, (item as BootUiState.Ready).destination)
        }
    }

    @Test
    fun `server failing past 2 minutes transitions to Timeout state`() = runTest(testDispatcher) {
        coEvery { healthRepository.checkHealth() } returns HealthStatus.Down(503, "Down")
        coEvery { activityRepository.hasCachedActivities() } returns true

        val viewModel = createViewModel().apply {
            fastCheckTimeoutMs = 1000L
            pollIntervalMs = 2000L
            maxWaitMs = 5000L
        }

        viewModel.uiState.test {
            var item = awaitItem()
            if (item is BootUiState.Checking) {
                item = awaitItem()
            }

            assertTrue("Expected Waking state but got $item", item is BootUiState.Waking)

            advanceTimeBy(6000L)

            while (item is BootUiState.Waking) {
                item = awaitItem()
            }

            assertTrue("Expected Timeout state but got $item", item is BootUiState.Timeout)
            assertEquals(true, (item as BootUiState.Timeout).hasCachedData)
        }
    }

    @Test
    fun `offline with valid session and cached data continues in offline read-only mode`() = runTest(testDispatcher) {
        every { networkMonitor.isOnline } returns flowOf(false)
        coEvery { activityRepository.hasCachedActivities() } returns true
        coEvery { sessionManager.isTokenExpired() } returns false
        every { sessionManager.sessionState } returns MutableStateFlow(
            SessionState.LoggedIn(role = Role.TREKKER, userId = "123", email = "test@trek.com")
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Ready state with offlineMode = true but was $state", state is BootUiState.Ready)
        val ready = state as BootUiState.Ready
        assertEquals(true, ready.offlineMode)
        assertEquals(AppDestination.TrekkerHome, ready.destination)
    }

    @Test
    fun `offline without cached data shows Offline state`() = runTest(testDispatcher) {
        every { networkMonitor.isOnline } returns flowOf(false)
        coEvery { activityRepository.hasCachedActivities() } returns false

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Offline state but was $state", state is BootUiState.Offline)
        assertEquals(false, (state as BootUiState.Offline).canContinueOffline)
    }
}
