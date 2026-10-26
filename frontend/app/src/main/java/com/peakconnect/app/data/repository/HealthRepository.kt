package com.peakconnect.app.data.repository

import com.peakconnect.app.core.network.HealthStatus
import com.peakconnect.app.data.api.HealthApi
import javax.inject.Inject
import javax.inject.Singleton

interface HealthRepository {
    suspend fun checkHealth(): HealthStatus
}

@Singleton
class HealthRepositoryImpl @Inject constructor(
    private val healthApi: HealthApi
) : HealthRepository {

    override suspend fun checkHealth(): HealthStatus {
        return try {
            val response = healthApi.getHealth()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.status.equals("UP", ignoreCase = true)) {
                    HealthStatus.Up
                } else {
                    HealthStatus.Down(statusCode = response.code(), message = body?.status)
                }
            } else {
                HealthStatus.Down(statusCode = response.code(), message = response.message())
            }
        } catch (e: Exception) {
            HealthStatus.Unreachable(exception = e)
        }
    }
}
