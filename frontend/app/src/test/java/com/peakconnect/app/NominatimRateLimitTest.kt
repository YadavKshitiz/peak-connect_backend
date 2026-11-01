package com.peakconnect.app

import com.peakconnect.app.feature.guide.mappicker.NominatimSearchService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NominatimRateLimitTest {

    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun `search with fewer than 3 characters returns empty list immediately`() = runTest(testDispatcher) {
        val service = NominatimSearchService(testDispatcher)
        var sleptMillis = 0L
        service.sleeper = { sleptMillis += it }

        val result1 = service.search("")
        val result2 = service.search("ab")

        assertTrue(result1.isEmpty())
        assertTrue(result2.isEmpty())
        assertEquals(0L, sleptMillis)
    }

    @Test
    fun `rate limiter enforces minimum 1 second spacing between consecutive requests`() = runTest(testDispatcher) {
        val service = NominatimSearchService(testDispatcher)
        var currentTime = 10000L
        service.clock = { currentTime }
        var totalSleep = 0L
        service.sleeper = {
            totalSleep += it
            currentTime += it
        }

        // First request sets lastRequestTime to 10000ms (will fail network or succeed, but tests rate limiting delay)
        // Since there is no real server running, we wrap in try-catch to observe the sleep behavior
        try {
            service.search("Manali")
        } catch (_: Exception) { }

        // Advance simulated time by only 400ms (< 1000ms)
        currentTime = 10400L

        // Second request should sleep 600ms to respect the 1-second rate limit
        try {
            service.search("Shimla")
        } catch (_: Exception) { }

        assertEquals(600L, totalSleep)
    }
}
