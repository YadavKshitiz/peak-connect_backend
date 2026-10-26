package com.peakconnect.app

import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.network.AppError
import com.peakconnect.app.core.network.HealthStatus
import com.peakconnect.app.core.network.WakeUpRetryHelper
import com.peakconnect.app.data.repository.HealthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class WakeUpRetryTest {

    private val healthRepository: HealthRepository = mockk(relaxed = true)
    private val helper = WakeUpRetryHelper(healthRepository)

    @Test
    fun `withWakeUpRetry returns success immediately without polling health`() = runTest {
        val attempts = AtomicInteger(0)

        val result = helper.withWakeUpRetry {
            attempts.incrementAndGet()
            ApiResult.Success("Data")
        }

        assertTrue(result is ApiResult.Success)
        assertEquals("Data", (result as ApiResult.Success).data)
        assertEquals(1, attempts.get())
        assertFalse(helper.isServerWakingUp.value)
        coVerify(exactly = 0) { healthRepository.checkHealth() }
    }

    @Test
    fun `withWakeUpRetry polls health and retries once after health Up`() = runTest {
        val attempts = AtomicInteger(0)

        // Health check returns Unreachable first, then Up
        val healthAttempts = AtomicInteger(0)
        coEvery { healthRepository.checkHealth() } answers {
            if (healthAttempts.incrementAndGet() == 1) {
                HealthStatus.Unreachable()
            } else {
                HealthStatus.Up
            }
        }

        val result = helper.withWakeUpRetry(pollIntervalMs = 1000L, maxDurationMs = 5000L) {
            val callCount = attempts.incrementAndGet()
            if (callCount == 1) {
                ApiResult.Error(AppError.Timeout, "Initial timeout")
            } else {
                ApiResult.Success("Recovered Data")
            }
        }

        assertTrue(result is ApiResult.Success)
        assertEquals("Recovered Data", (result as ApiResult.Success).data)
        assertEquals(2, attempts.get())
        assertFalse("isServerWakingUp should reset to false", helper.isServerWakingUp.value)
    }

    @Test
    fun `withWakeUpRetry gives up after max duration if server remains Down or Unreachable`() = runTest {
        val attempts = AtomicInteger(0)
        coEvery { healthRepository.checkHealth() } returns HealthStatus.Down()

        val result = helper.withWakeUpRetry(pollIntervalMs = 1000L, maxDurationMs = 3000L) {
            attempts.incrementAndGet()
            ApiResult.Error(AppError.Timeout, "Server is waking up")
        }

        assertTrue(result is ApiResult.Error)
        assertEquals(AppError.Timeout, (result as ApiResult.Error).type)
        assertEquals(1, attempts.get()) // Did not retry block because server never came Up
        assertFalse(helper.isServerWakingUp.value)
        coVerify(atLeast = 3) { healthRepository.checkHealth() }
    }
}
