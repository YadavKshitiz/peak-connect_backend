package com.peakconnect.app.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetryInterceptor @Inject constructor() : Interceptor {

    var delayMillis: Long = 2000L

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Never retry non-GET requests (e.g. POST, PUT, DELETE)
        if (!request.method.equals("GET", ignoreCase = true)) {
            return chain.proceed(request)
        }

        return try {
            chain.proceed(request)
        } catch (e: IOException) {
            if (e is SocketTimeoutException || e is ConnectException) {
                if (delayMillis > 0) {
                    try {
                        Thread.sleep(delayMillis)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                    }
                }
                chain.proceed(request)
            } else {
                throw e
            }
        }
    }
}
