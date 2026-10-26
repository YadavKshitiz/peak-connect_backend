package com.peakconnect.app

import com.peakconnect.app.core.network.RetryInterceptor
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class RetryInterceptorTest {

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `retries GET once on SocketTimeoutException`() {
        val attempts = AtomicInteger(0)
        val interceptor = RetryInterceptor().apply { delayMillis = 10L }

        val failingInterceptor = Interceptor { chain ->
            val count = attempts.incrementAndGet()
            if (count == 1) {
                throw SocketTimeoutException("Simulated timeout on first attempt")
            }
            chain.proceed(chain.request())
        }

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("OK"))

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor(failingInterceptor)
            .build()

        val request = Request.Builder()
            .url(mockWebServer.url("/api/activities"))
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            assertEquals("OK", response.body?.string())
        }

        assertEquals(2, attempts.get())
    }

    @Test
    fun `never retries POST requests on network timeout`() {
        val attempts = AtomicInteger(0)
        val interceptor = RetryInterceptor().apply { delayMillis = 10L }

        val failingInterceptor = Interceptor { chain ->
            attempts.incrementAndGet()
            throw SocketTimeoutException("Simulated timeout on POST")
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor(failingInterceptor)
            .build()

        val request = Request.Builder()
            .url(mockWebServer.url("/api/bookings/confirm"))
            .post("{}".toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute()
            fail("Expected SocketTimeoutException")
        } catch (_: SocketTimeoutException) {
            // Expected
        }

        assertEquals("POST must not be retried", 1, attempts.get())
    }
}
