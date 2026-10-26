package com.peakconnect.app.core.network

import com.peakconnect.app.data.repository.HealthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WakeUpRetryHelper @Inject constructor(
    private val healthRepository: HealthRepository
) {
    private val _isServerWakingUp = MutableStateFlow(false)
    val isServerWakingUp: StateFlow<Boolean> = _isServerWakingUp.asStateFlow()

    suspend fun <T> withWakeUpRetry(
        pollIntervalMs: Long = 4000L,
        maxDurationMs: Long = 120000L,
        block: suspend () -> ApiResult<T>
    ): ApiResult<T> {
        val initialResult = block()

        val isTimeoutOrNetworkError = when (initialResult) {
            is ApiResult.NetworkError -> true
            is ApiResult.Error -> initialResult.type is AppError.Timeout || initialResult.type is AppError.NoInternet
            is ApiResult.Success -> false
        }

        if (!isTimeoutOrNetworkError) {
            return initialResult
        }

        _isServerWakingUp.value = true
        try {
            var elapsed = 0L
            var isUp = false

            while (elapsed < maxDurationMs) {
                delay(pollIntervalMs)
                elapsed += pollIntervalMs

                val health = healthRepository.checkHealth()
                if (health is HealthStatus.Up) {
                    isUp = true
                    break
                }
            }

            return if (isUp) {
                block()
            } else {
                initialResult
            }
        } finally {
            _isServerWakingUp.value = false
        }
    }
}
