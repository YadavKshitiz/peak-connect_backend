package com.peakconnect.app

import com.peakconnect.app.core.preferences.NotificationPreferences
import com.peakconnect.app.core.preferences.ThemePreferences
import com.peakconnect.app.core.session.SessionManager
import com.peakconnect.app.core.session.SessionState
import com.peakconnect.app.core.theme.ThemeMode
import com.peakconnect.app.data.local.entity.GuideProfileEntity
import com.peakconnect.app.data.repository.AuthRepository
import com.peakconnect.app.data.repository.GuideRepository
import com.peakconnect.app.domain.model.ExperienceLevel
import com.peakconnect.app.domain.model.Role
import com.peakconnect.app.feature.notifications.sync.NotificationWorkScheduler
import com.peakconnect.app.feature.profile.ProfileViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val sessionManager: SessionManager = mockk(relaxed = true)
    private val guideRepository: GuideRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val themePreferences: ThemePreferences = mockk(relaxed = true)
    private val notificationPreferences: NotificationPreferences = mockk(relaxed = true)
    private val notificationWorkScheduler: NotificationWorkScheduler = mockk(relaxed = true)

    private val sessionFlow = MutableStateFlow<SessionState>(SessionState.LoggedOut)
    private val themeFlow = MutableStateFlow(ThemeMode.SYSTEM)
    private val masterEnabledFlow = MutableStateFlow(true)
    private val remindersEnabledFlow = MutableStateFlow(true)
    private val lastSyncFlow = MutableStateFlow<Long?>(1000L)
    private val guideProfileFlow = MutableStateFlow<GuideProfileEntity?>(null)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { sessionManager.sessionState } returns sessionFlow
        every { themePreferences.themeMode } returns themeFlow
        every { notificationPreferences.masterEnabled } returns masterEnabledFlow
        every { notificationPreferences.remindersEnabled } returns remindersEnabledFlow
        every { notificationPreferences.lastSyncTimestamp } returns lastSyncFlow
        every { guideRepository.getProfileFlow() } returns guideProfileFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `trekker session populates name and email in state`() = runTest {
        sessionFlow.value = SessionState.LoggedIn(
            role = Role.TREKKER,
            userId = "u1",
            email = "john.doe@example.com",
            name = "John Doe"
        )

        val viewModel = ProfileViewModel(
            sessionManager,
            guideRepository,
            authRepository,
            themePreferences,
            notificationPreferences,
            notificationWorkScheduler
        )
        advanceUntilIdle()

        assertEquals("john.doe@example.com", viewModel.uiState.value.email)
        assertEquals("John Doe", viewModel.uiState.value.name)
        assertEquals(Role.TREKKER, viewModel.uiState.value.role)
    }

    @Test
    fun `guide session updates name from guide repository profile`() = runTest {
        sessionFlow.value = SessionState.LoggedIn(
            role = Role.GUIDE,
            userId = "g1",
            email = "guide@example.com",
            name = null
        )
        guideProfileFlow.value = GuideProfileEntity(
            id = "g1",
            name = "Tenzing Norgay",
            email = "guide@example.com",
            skills = listOf("Climbing"),
            languages = listOf("Hindi", "English"),
            location = "Manali",
            latitude = 32.2,
            longitude = 77.1,
            experienceLevel = ExperienceLevel.EXPERT,
            isVerified = true,
            lastUpdated = 1000L
        )

        val viewModel = ProfileViewModel(
            sessionManager,
            guideRepository,
            authRepository,
            themePreferences,
            notificationPreferences,
            notificationWorkScheduler
        )
        advanceUntilIdle()

        assertEquals("Tenzing Norgay", viewModel.uiState.value.name)
        assertEquals(Role.GUIDE, viewModel.uiState.value.role)
    }

    @Test
    fun `appearance dialog open, dismiss, and theme selection`() = runTest {
        val viewModel = ProfileViewModel(
            sessionManager,
            guideRepository,
            authRepository,
            themePreferences,
            notificationPreferences,
            notificationWorkScheduler
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showAppearanceDialog)
        viewModel.onAppearanceClicked()
        assertTrue(viewModel.uiState.value.showAppearanceDialog)

        viewModel.onDismissAppearanceDialog()
        assertFalse(viewModel.uiState.value.showAppearanceDialog)

        viewModel.onThemeSelected(ThemeMode.DARK)
        advanceUntilIdle()
        coVerify { themePreferences.setThemeMode(ThemeMode.DARK) }
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    }

    @Test
    fun `notification toggles call preferences and scheduler`() = runTest {
        val viewModel = ProfileViewModel(
            sessionManager,
            guideRepository,
            authRepository,
            themePreferences,
            notificationPreferences,
            notificationWorkScheduler
        )
        advanceUntilIdle()

        viewModel.onNotificationsToggled(false)
        advanceUntilIdle()
        coVerify { notificationPreferences.setMasterEnabled(false) }
        verify { notificationWorkScheduler.cancelAllWork() }

        viewModel.onNotificationsToggled(true)
        advanceUntilIdle()
        coVerify { notificationPreferences.setMasterEnabled(true) }
        verify { notificationWorkScheduler.schedulePeriodicSync() }
    }

    @Test
    fun `confirming logout calls authRepository logout and executes callback`() = runTest {
        val viewModel = ProfileViewModel(
            sessionManager,
            guideRepository,
            authRepository,
            themePreferences,
            notificationPreferences,
            notificationWorkScheduler
        )
        advanceUntilIdle()

        var loggedOutCalled = false
        viewModel.onConfirmLogout { loggedOutCalled = true }
        advanceUntilIdle()

        coVerify { authRepository.logout() }
        assertTrue(loggedOutCalled)
        assertFalse(viewModel.uiState.value.isLoggingOut)
    }
}
