package com.peakconnect.app.feature.boot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakconnect.app.core.navigation.AppDestination
import com.peakconnect.app.core.navigation.resolveInitialDestination
import com.peakconnect.app.core.network.HealthStatus
import com.peakconnect.app.core.preferences.OnboardingStore
import com.peakconnect.app.core.session.SessionManager
import com.peakconnect.app.core.session.SessionState
import com.peakconnect.app.core.session.TokenStore
import com.peakconnect.app.core.util.IoDispatcher
import com.peakconnect.app.core.util.NetworkMonitor
import com.peakconnect.app.data.repository.ActivityRepository
import com.peakconnect.app.data.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

sealed interface BootUiState {
    data object Checking : BootUiState
    data class Waking(val elapsedSeconds: Long) : BootUiState
    data class Timeout(val hasCachedData: Boolean) : BootUiState
    data class Offline(val canContinueOffline: Boolean) : BootUiState
    data class Ready(val destination: AppDestination, val offlineMode: Boolean = false) : BootUiState
}

@HiltViewModel
class BootViewModel @Inject constructor(
    private val healthRepository: HealthRepository,
    private val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager,
    private val tokenStore: TokenStore,
    private val activityRepository: ActivityRepository,
    private val onboardingStore: OnboardingStore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow<BootUiState>(BootUiState.Checking)
    val uiState: StateFlow<BootUiState> = _uiState.asStateFlow()

    private var checkJob: Job? = null
    private var timerJob: Job? = null

    var fastCheckTimeoutMs: Long = 3000L
    var pollIntervalMs: Long = 4000L
    var maxWaitMs: Long = 120_000L

    init {
        startBootCheck()
    }

    fun retry() {
        startBootCheck()
    }

    fun continueOffline() {
        viewModelScope.launch(ioDispatcher) {
            val destination = resolveCurrentDestination()
            _uiState.value = BootUiState.Ready(destination = destination, offlineMode = true)
        }
    }

    fun startBootCheck() {
        checkJob?.cancel()
        timerJob?.cancel()

        checkJob = viewModelScope.launch(ioDispatcher) {
            _uiState.value = BootUiState.Checking

            // Ensure session state is initialized from stored token
            sessionManager.initialize()

            val isOnline = networkMonitor.isOnline.first()
            if (!isOnline) {
                handleOfflineState()
                return@launch
            }

            // Fast health check within 3 seconds
            val fastResult = withTimeoutOrNull(fastCheckTimeoutMs) {
                healthRepository.checkHealth()
            }

            if (fastResult is HealthStatus.Up) {
                val destination = resolveCurrentDestination()
                _uiState.value = BootUiState.Ready(destination = destination, offlineMode = false)
                return@launch
            }

            // Server is sleeping / taking longer than 3 seconds: Enter Waking state
            var elapsedSeconds = (fastCheckTimeoutMs / 1000)
            _uiState.value = BootUiState.Waking(elapsedSeconds = elapsedSeconds)

            // Start timer job to update elapsed seconds every second
            timerJob = launch {
                while (true) {
                    delay(1000L)
                    elapsedSeconds++
                    if (_uiState.value is BootUiState.Waking) {
                        _uiState.value = BootUiState.Waking(elapsedSeconds = elapsedSeconds)
                    }
                }
            }

            // Poll health check every 4 seconds until Up or maxWaitMs (2 minutes)
            var elapsedMs = fastCheckTimeoutMs
            var serverUp = false

            while (elapsedMs < maxWaitMs) {
                delay(pollIntervalMs)
                elapsedMs += pollIntervalMs

                val pollResult = healthRepository.checkHealth()
                if (pollResult is HealthStatus.Up) {
                    serverUp = true
                    break
                }
            }

            timerJob?.cancel()

            if (serverUp) {
                val destination = resolveCurrentDestination()
                _uiState.value = BootUiState.Ready(destination = destination, offlineMode = false)
            } else {
                val hasCached = activityRepository.hasCachedActivities()
                _uiState.value = BootUiState.Timeout(hasCachedData = hasCached)
            }
        }
    }

    private suspend fun handleOfflineState() {
        val hasCached = activityRepository.hasCachedActivities()
        val isSessionValid = sessionManager.sessionState.value is SessionState.LoggedIn && !sessionManager.isTokenExpired()
        if (hasCached && isSessionValid) {
            val destination = resolveCurrentDestination()
            _uiState.value = BootUiState.Ready(destination = destination, offlineMode = true)
        } else {
            _uiState.value = BootUiState.Offline(canContinueOffline = false)
        }
    }

    private suspend fun resolveCurrentDestination(): AppDestination {
        val onboardingCompleted = onboardingStore.isOnboardingCompleted().first()
        val token = tokenStore.getToken()
        val isExpired = sessionManager.isTokenExpired()
        val role = (sessionManager.sessionState.value as? SessionState.LoggedIn)?.role

        return resolveInitialDestination(
            onboardingCompleted = onboardingCompleted,
            token = token,
            isTokenExpired = isExpired,
            role = role
        )
    }

    override fun onCleared() {
        super.onCleared()
        checkJob?.cancel()
        timerJob?.cancel()
    }
}
