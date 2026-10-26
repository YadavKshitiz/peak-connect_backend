package com.peakconnect.app.core.network

sealed interface HealthStatus {
    data object Up : HealthStatus
    data class Down(val statusCode: Int? = null, val message: String? = null) : HealthStatus
    data class Unreachable(val exception: Throwable? = null) : HealthStatus
}
