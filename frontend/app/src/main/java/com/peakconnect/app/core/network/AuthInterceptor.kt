package com.peakconnect.app.core.network

import com.peakconnect.app.core.session.SessionEvents
import com.peakconnect.app.core.session.TokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        val isPublicAuthOrHealth = path.contains("api/auth/login") ||
                path.contains("api/auth/register") ||
                path.contains("actuator/health")

        val request = if (isPublicAuthOrHealth) {
            originalRequest
        } else {
            val token = tokenStore.getCachedToken()
            if (!token.isNullOrBlank()) {
                originalRequest.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                originalRequest
            }
        }

        val response = chain.proceed(request)

        if (response.code == 401 && !isPublicAuthOrHealth) {
            SessionEvents.emitSessionExpired()
        }

        return response
    }
}
