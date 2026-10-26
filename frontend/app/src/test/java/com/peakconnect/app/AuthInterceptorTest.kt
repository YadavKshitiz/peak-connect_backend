package com.peakconnect.app

import app.cash.turbine.test
import com.peakconnect.app.core.network.AuthInterceptor
import com.peakconnect.app.core.session.SessionEvent
import com.peakconnect.app.core.session.SessionEvents
import com.peakconnect.app.core.session.TokenStore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private val tokenStore: TokenStore = mockk(relaxed = true)

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        every { tokenStore.getCachedToken() } returns "my_secret_jwt_token"
        every { tokenStore.getTokenFlow() } returns flowOf("my_secret_jwt_token")
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `adds Authorization header to normal API requests`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .build()

        val request = Request.Builder()
            .url(mockWebServer.url("/api/bookings/my"))
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
        }

        val recorded = mockWebServer.takeRequest()
        assertEquals("Bearer my_secret_jwt_token", recorded.getHeader("Authorization"))
    }

    @Test
    fun `omits Authorization header for login register and health endpoints`() {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .build()

        val endpoints = listOf("/api/auth/login", "/api/auth/register", "/actuator/health")
        for (ep in endpoints) {
            mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
            val request = Request.Builder().url(mockWebServer.url(ep)).build()
            client.newCall(request).execute().close()

            val recorded = mockWebServer.takeRequest()
            assertNull("Header should be omitted for $ep", recorded.getHeader("Authorization"))
        }
    }

    @Test
    fun `emits SessionExpired event when protected call returns 401`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"unauthorized"}"""))

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .build()

        SessionEvents.events.test {
            val request = Request.Builder()
                .url(mockWebServer.url("/api/bookings/my"))
                .build()

            client.newCall(request).execute().close()

            val event = awaitItem()
            assertTrue(event is SessionEvent.SessionExpired)
        }
    }
}
