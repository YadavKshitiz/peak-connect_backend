package com.peakconnect.app.core.network

import com.peakconnect.app.data.dto.BackendErrorDto
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorMapper {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    const val DEFAULT_BAD_CREDENTIALS_MSG = "Incorrect email or password"
    const val DEFAULT_UNAUTHORIZED_MSG = "Please log in to continue."
    const val DEFAULT_FORBIDDEN_MSG = "You do not have permission to perform this action."
    const val DEFAULT_NOT_FOUND_MSG = "Requested item was not found."
    const val DEFAULT_SERVER_ERROR_MSG = "Server error occurred. Please try again later."
    const val DEFAULT_TIMEOUT_MSG = "Connection timed out. Please try again."
    const val DEFAULT_NO_INTERNET_MSG = "No internet connection. Please check your network."
    const val DEFAULT_UNKNOWN_MSG = "An unexpected error occurred."

    fun mapHttpError(code: Int, rawBody: String?): ApiResult.Error {
        val parsed = parseBackendError(rawBody)
        val rawMessage = parsed?.error ?: parsed?.message ?: ""

        // Special handling for Bad credentials
        val isBadCredentials = rawMessage.contains("Bad credentials", ignoreCase = true)
        val finalMessage = if (isBadCredentials) {
            DEFAULT_BAD_CREDENTIALS_MSG
        } else if (rawMessage.isNotBlank()) {
            rawMessage
        } else {
            getDefaultMessageForCode(code)
        }

        val appError = when (code) {
            400 -> AppError.Validation(backendMessage = finalMessage, details = parsed?.details)
            401 -> AppError.Unauthorized
            403 -> AppError.Forbidden
            404 -> AppError.NotFound
            409 -> AppError.Conflict(backendMessage = finalMessage)
            in 500..599 -> AppError.Server(code)
            else -> AppError.Unknown(code = code, rawMessage = rawMessage)
        }

        return ApiResult.Error(type = appError, message = finalMessage)
    }

    fun mapThrowable(throwable: Throwable): ApiResult<Nothing> {
        return when (throwable) {
            is SocketTimeoutException -> ApiResult.Error(
                type = AppError.Timeout,
                message = DEFAULT_TIMEOUT_MSG
            )
            is ConnectException, is UnknownHostException -> ApiResult.Error(
                type = AppError.NoInternet,
                message = DEFAULT_NO_INTERNET_MSG
            )
            is IOException -> ApiResult.NetworkError(throwable)
            else -> ApiResult.Error(
                type = AppError.Unknown(rawMessage = throwable.message),
                message = throwable.message ?: DEFAULT_UNKNOWN_MSG
            )
        }
    }

    fun parseBackendError(rawBody: String?): BackendErrorDto? {
        if (rawBody.isNullOrBlank()) return null
        return try {
            json.decodeFromString<BackendErrorDto>(rawBody)
        } catch (_: Exception) {
            null
        }
    }

    private fun getDefaultMessageForCode(code: Int): String = when (code) {
        401 -> DEFAULT_UNAUTHORIZED_MSG
        403 -> DEFAULT_FORBIDDEN_MSG
        404 -> DEFAULT_NOT_FOUND_MSG
        in 500..599 -> DEFAULT_SERVER_ERROR_MSG
        else -> DEFAULT_UNKNOWN_MSG
    }
}
