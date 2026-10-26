package com.peakconnect.app.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class BackendErrorDto(
    val error: String? = null,
    val message: String? = null,
    val details: Map<String, String>? = null
)
