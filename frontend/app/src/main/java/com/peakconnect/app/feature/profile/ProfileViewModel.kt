package com.peakconnect.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakconnect.app.core.preferences.NotificationPreferences
import com.peakconnect.app.core.preferences.ThemePreferences
import com.peakconnect.app.core.session.SessionManager
import com.peakconnect.app.core.session.SessionState
import com.peakconnect.app.core.theme.ThemeMode
import com.peakconnect.app.data.repository.AuthRepository
import com.peakconnect.app.data.repository.GuideRepository
import com.peakconnect.app.domain.model.Role
import com.peakconnect.app.feature.notifications.sync.NotificationWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.peakconnect.app.core.locale.AppLanguage
import com.peakconnect.app.core.locale.AppLocaleManager
import java.util.Locale
import javax.inject.Inject

data class ProfileUiState(
    val email: String = "",
    val name: String = "",
    val role: Role = Role.TREKKER,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val remindersEnabled: Boolean = true,
    val lastSyncTimestamp: Long? = null,
    val selectedLanguage: AppLanguage = AppLanguage.SYSTEM_DEFAULT,
    val showAppearanceDialog: Boolean = false,
    val showNotificationSettingsDialog: Boolean = false,
    val showLanguageDialog: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val isLoggingOut: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val guideRepository: GuideRepository,
    private val authRepository: AuthRepository,
    private val themePreferences: ThemePreferences,
    private val notificationPreferences: NotificationPreferences,
    private val notificationWorkScheduler: NotificationWorkScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(selectedLanguage = AppLocaleManager.getCurrentLanguage()) }
        observeSession()
        observeTheme()
        observeNotifications()
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionManager.sessionState.collect { session ->
                if (session is SessionState.LoggedIn) {
                    val initialName = session.name?.takeIf { it.isNotBlank() }
                        ?: extractNameFromEmail(session.email)

                    _uiState.update { current ->
                        current.copy(
                            email = session.email,
                            role = session.role,
                            name = if (current.name.isBlank()) initialName else current.name
                        )
                    }

                    if (session.role == Role.GUIDE) {
                        observeGuideProfile()
                    }
                }
            }
        }
    }

    private fun observeGuideProfile() {
        viewModelScope.launch {
            guideRepository.getProfileFlow().collect { cached ->
                if (cached != null && cached.name.isNotBlank()) {
                    _uiState.update { it.copy(name = cached.name) }
                }
            }
        }

        viewModelScope.launch {
            // Attempt to refresh guide profile in background
            guideRepository.refreshProfile()
        }
    }

    private fun observeTheme() {
        viewModelScope.launch {
            themePreferences.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            notificationPreferences.masterEnabled.collect { enabled ->
                _uiState.update { it.copy(notificationsEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            notificationPreferences.remindersEnabled.collect { enabled ->
                _uiState.update { it.copy(remindersEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            notificationPreferences.lastSyncTimestamp.collect { ts ->
                _uiState.update { it.copy(lastSyncTimestamp = ts) }
            }
        }
    }

    fun onAppearanceClicked() {
        _uiState.update { it.copy(showAppearanceDialog = true) }
    }

    fun onDismissAppearanceDialog() {
        _uiState.update { it.copy(showAppearanceDialog = false) }
    }

    fun onThemeSelected(mode: ThemeMode) {
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
            _uiState.update { it.copy(themeMode = mode, showAppearanceDialog = false) }
        }
    }

    fun onNotificationsClicked() {
        _uiState.update { it.copy(showNotificationSettingsDialog = true) }
    }

    fun onDismissNotificationSettingsDialog() {
        _uiState.update { it.copy(showNotificationSettingsDialog = false) }
    }

    fun onLanguageClicked() {
        _uiState.update {
            it.copy(
                showLanguageDialog = true,
                selectedLanguage = AppLocaleManager.getCurrentLanguage()
            )
        }
    }

    fun onDismissLanguageDialog() {
        _uiState.update { it.copy(showLanguageDialog = false) }
    }

    fun onLanguageSelected(language: AppLanguage) {
        AppLocaleManager.setLanguage(language)
        _uiState.update {
            it.copy(
                selectedLanguage = language,
                showLanguageDialog = false
            )
        }
    }

    fun onNotificationsToggled(enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferences.setMasterEnabled(enabled)
            if (enabled) {
                notificationWorkScheduler.schedulePeriodicSync()
            } else {
                notificationWorkScheduler.cancelAllWork()
            }
        }
    }

    fun onRemindersToggled(enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferences.setRemindersEnabled(enabled)
        }
    }

    fun onSyncNow() {
        notificationWorkScheduler.scheduleOneTimeSync()
    }

    fun onLogoutClicked() {
        _uiState.update { it.copy(showLogoutDialog = true) }
    }

    fun onDismissLogoutDialog() {
        _uiState.update { it.copy(showLogoutDialog = false) }
    }

    fun onConfirmLogout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true, showLogoutDialog = false) }
            authRepository.logout()
            _uiState.update { it.copy(isLoggingOut = false) }
            onLoggedOut()
        }
    }

    private fun extractNameFromEmail(email: String): String {
        val prefix = email.substringBefore("@").replace('.', ' ').replace('_', ' ').trim()
        return prefix.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }.ifBlank { "User" }
    }
}
