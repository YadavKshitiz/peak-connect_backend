package com.peakconnect.app

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.peakconnect.app.core.locale.AppLanguage
import com.peakconnect.app.core.locale.AppLocaleManager
import com.peakconnect.app.core.preferences.NotificationPreferences
import com.peakconnect.app.core.preferences.ThemePreferences
import com.peakconnect.app.core.session.SessionManager
import com.peakconnect.app.core.session.SessionState
import com.peakconnect.app.core.theme.ThemeMode
import com.peakconnect.app.data.repository.AuthRepository
import com.peakconnect.app.data.repository.GuideRepository
import com.peakconnect.app.domain.model.Role
import com.peakconnect.app.feature.notifications.sync.NotificationWorkScheduler
import com.peakconnect.app.feature.profile.ProfileViewModel
import io.mockk.every
import io.mockk.mockk
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LanguageSwitchTest {

    private val testDispatcher = StandardTestDispatcher()
    private val sessionManager: SessionManager = mockk(relaxed = true)
    private val guideRepository: GuideRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val themePreferences: ThemePreferences = mockk(relaxed = true)
    private val notificationPreferences: NotificationPreferences = mockk(relaxed = true)
    private val notificationWorkScheduler: NotificationWorkScheduler = mockk(relaxed = true)

    private val sessionStateFlow = MutableStateFlow<SessionState>(
        SessionState.LoggedIn(
            role = Role.TREKKER,
            userId = "trekker-1",
            email = "alex.smith@example.com",
            name = "Alex Smith"
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { sessionManager.sessionState } returns sessionStateFlow
        every { themePreferences.themeMode } returns MutableStateFlow(ThemeMode.SYSTEM)
        every { notificationPreferences.masterEnabled } returns MutableStateFlow(true)
        every { notificationPreferences.remindersEnabled } returns MutableStateFlow(true)
        every { notificationPreferences.lastSyncTimestamp } returns MutableStateFlow(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        // Reset to system default after tests
        AppLocaleManager.setLanguage(AppLanguage.SYSTEM_DEFAULT)
    }

    @Test
    fun testAppLocaleManagerLanguageSwitchPersistence() {
        // Switch to Hindi
        AppLocaleManager.setLanguage(AppLanguage.HINDI)
        assertEquals(AppLanguage.HINDI, AppLocaleManager.getCurrentLanguage())

        // Switch to English
        AppLocaleManager.setLanguage(AppLanguage.ENGLISH)
        assertEquals(AppLanguage.ENGLISH, AppLocaleManager.getCurrentLanguage())

        // Switch to System Default
        AppLocaleManager.setLanguage(AppLanguage.SYSTEM_DEFAULT)
        assertEquals(AppLanguage.SYSTEM_DEFAULT, AppLocaleManager.getCurrentLanguage())
    }

    @Test
    fun testProfileViewModelLanguageSelectionFlow() = runTest(testDispatcher) {
        val viewModel = ProfileViewModel(
            sessionManager = sessionManager,
            guideRepository = guideRepository,
            authRepository = authRepository,
            themePreferences = themePreferences,
            notificationPreferences = notificationPreferences,
            notificationWorkScheduler = notificationWorkScheduler
        )
        advanceUntilIdle()

        // Initially dialog is not showing
        assertFalse(viewModel.uiState.value.showLanguageDialog)

        // Open language dialog
        viewModel.onLanguageClicked()
        assertTrue(viewModel.uiState.value.showLanguageDialog)

        // Dismiss dialog without selecting
        viewModel.onDismissLanguageDialog()
        assertFalse(viewModel.uiState.value.showLanguageDialog)

        // Open again and select Hindi
        viewModel.onLanguageClicked()
        assertTrue(viewModel.uiState.value.showLanguageDialog)

        viewModel.onLanguageSelected(AppLanguage.HINDI)
        advanceUntilIdle()

        // Verify state is updated and dialog dismissed
        assertFalse(viewModel.uiState.value.showLanguageDialog)
        assertEquals(AppLanguage.HINDI, viewModel.uiState.value.selectedLanguage)
        assertEquals(AppLanguage.HINDI, AppLocaleManager.getCurrentLanguage())
    }

    @Test
    fun testAppLanguageFromTagHelper() {
        assertEquals(AppLanguage.SYSTEM_DEFAULT, AppLanguage.fromTag(null))
        assertEquals(AppLanguage.SYSTEM_DEFAULT, AppLanguage.fromTag(""))
        assertEquals(AppLanguage.HINDI, AppLanguage.fromTag("hi"))
        assertEquals(AppLanguage.HINDI, AppLanguage.fromTag("hi-IN"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en-US"))
        assertEquals(AppLanguage.SYSTEM_DEFAULT, AppLanguage.fromTag("unknown"))
    }
}
