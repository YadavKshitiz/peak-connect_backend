package com.peakconnect.app.core.network

sealed interface AppError {
    data object Unauthorized : AppError // 401
    data object Forbidden : AppError // 403
    data object NotFound : AppError // 404
    data class Conflict(val backendMessage: String) : AppError // 409
    data class Validation(val backendMessage: String, val details: Map<String, String>? = null) : AppError // 400
    data class Server(val code: Int) : AppError // 5xx
    data object Timeout : AppError
    data object NoInternet : AppError
    data class Unknown(val code: Int? = null, val rawMessage: String? = null) : AppError
}
