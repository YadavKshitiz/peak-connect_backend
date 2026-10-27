package com.peakconnect.app.core.network

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.peakconnect.app.core.util.IoDispatcher
import com.peakconnect.app.data.repository.HealthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResumeHealthChecker @Inject constructor(
    private val healthRepository: HealthRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : DefaultLifecycleObserver {

    private val _isServerWakingUp = MutableStateFlow(false)
    val isServerWakingUp: StateFlow<Boolean> = _isServerWakingUp.asStateFlow()

    private var lastBackgroundTimeMs: Long = 0L
    private var healthCheckJob: Job? = null

    var backgroundThresholdMs: Long = 10 * 60 * 1000L // 10 minutes
    var slowThresholdMs: Long = 3000L // 3 seconds
    var pollIntervalMs: Long = 4000L // 4 seconds

    fun onAppBackgrounded(timestamp: Long = System.currentTimeMillis()) {
        lastBackgroundTimeMs = timestamp
    }

    fun onAppForegrounded(scope: CoroutineScope, timestamp: Long = System.currentTimeMillis()) {
        if (lastBackgroundTimeMs > 0L && (timestamp - lastBackgroundTimeMs) >= backgroundThresholdMs) {
            checkHealthSilently(scope)
        }
    }

    fun checkHealthSilently(scope: CoroutineScope) {
        healthCheckJob?.cancel()
        healthCheckJob = scope.launch(ioDispatcher) {
            val timerJob = launch {
                delay(slowThresholdMs)
                _isServerWakingUp.value = true
            }

            val initialResult = healthRepository.checkHealth()
            timerJob.cancel()

            if (initialResult is HealthStatus.Up) {
                _isServerWakingUp.value = false
            } else {
                _isServerWakingUp.value = true
                while (true) {
                    delay(pollIntervalMs)
                    val result = healthRepository.checkHealth()
                    if (result is HealthStatus.Up) {
                        _isServerWakingUp.value = false
                        break
                    }
                }
            }
        }
    }
}
