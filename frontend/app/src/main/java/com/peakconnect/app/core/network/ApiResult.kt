package com.peakconnect.app.core.network

sealed interface ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>
    data class Error(val type: AppError, val message: String) : ApiResult<Nothing>
    data class NetworkError(val exception: Throwable) : ApiResult<Nothing>

    val isSuccess: Boolean
        get() = this is Success

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
}
