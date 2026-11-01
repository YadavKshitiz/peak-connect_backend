package com.peakconnect.app.feature.trekker.booking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.peakconnect.app.core.ui.formatCountdown
import com.peakconnect.app.domain.model.BookingStatus
import kotlinx.coroutines.delay

/**
 * Pure calculation logic for approximate booking countdown windows.
 * Values verified against backend application.yml and timeout jobs:
 * - guide.response-timeout-minutes: 20
 * - booking.payment-timeout-minutes: 15
 */
object BookingCountdownCalculator {
    const val GUIDE_RESPONSE_WINDOW_MINUTES: Long = 20L
    const val PAYMENT_WINDOW_MINUTES: Long = 15L

    const val GUIDE_RESPONSE_WINDOW_MILLIS: Long = GUIDE_RESPONSE_WINDOW_MINUTES * 60 * 1000L
    const val PAYMENT_WINDOW_MILLIS: Long = PAYMENT_WINDOW_MINUTES * 60 * 1000L

    fun getWindowDurationMillis(status: BookingStatus): Long {
        return when (status) {
            BookingStatus.PENDING_GUIDE_RESPONSE -> GUIDE_RESPONSE_WINDOW_MILLIS
            BookingStatus.AWAITING_PAYMENT -> PAYMENT_WINDOW_MILLIS
            else -> 0L
        }
    }

    fun calculateRemainingMillis(
        statusSinceMillis: Long,
        windowDurationMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        if (windowDurationMillis <= 0L || statusSinceMillis <= 0L) return 0L
        val elapsed = maxOf(0L, nowMillis - statusSinceMillis)
        return maxOf(0L, windowDurationMillis - elapsed)
    }

    fun isExpired(
        statusSinceMillis: Long,
        windowDurationMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        return calculateRemainingMillis(statusSinceMillis, windowDurationMillis, nowMillis) <= 0L
    }

    fun formatMinutesRemaining(remainingMillis: Long): Int {
        if (remainingMillis <= 0L) return 0
        val totalSeconds = remainingMillis / 1000
        return ((totalSeconds + 59) / 60).toInt()
    }

    fun calculateState(
        statusSinceMillis: Long,
        windowDurationMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): BookingCountdownState {
        val remaining = calculateRemainingMillis(statusSinceMillis, windowDurationMillis, nowMillis)
        val expired = remaining <= 0L
        val minutes = formatMinutesRemaining(remaining)
        val formatted = formatCountdown(remaining)
        return BookingCountdownState(
            remainingMillis = remaining,
            isExpired = expired,
            minutesRemaining = minutes,
            formattedTime = formatted
        )
    }
}

data class BookingCountdownState(
    val remainingMillis: Long,
    val isExpired: Boolean,
    val minutesRemaining: Int,
    val formattedTime: String
)

/**
 * Lifecycle-aware ticking countdown state holder.
 * Ticks every 1 second until remainingMillis reaches zero, then stops ticking.
 * Automatically cleans up on disposal without memory leaks.
 */
@Composable
fun rememberBookingCountdown(
    statusSinceMillis: Long,
    windowDurationMillis: Long,
    clock: () -> Long = { System.currentTimeMillis() }
): State<BookingCountdownState> {
    val state = remember(statusSinceMillis, windowDurationMillis) {
        mutableStateOf(
            BookingCountdownCalculator.calculateState(
                statusSinceMillis = statusSinceMillis,
                windowDurationMillis = windowDurationMillis,
                nowMillis = clock()
            )
        )
    }

    LaunchedEffect(statusSinceMillis, windowDurationMillis) {
        while (true) {
            val currentState = BookingCountdownCalculator.calculateState(
                statusSinceMillis = statusSinceMillis,
                windowDurationMillis = windowDurationMillis,
                nowMillis = clock()
            )
            state.value = currentState
            if (currentState.isExpired) {
                // STOP counting at zero
                break
            }
            delay(1000L)
        }
    }

    return state
}
