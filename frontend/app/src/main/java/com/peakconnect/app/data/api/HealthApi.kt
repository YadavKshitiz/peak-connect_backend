package com.peakconnect.app.data.api

import com.peakconnect.app.data.dto.HealthResponseDto
import retrofit2.Response
import retrofit2.http.GET

interface HealthApi {
    @GET("actuator/health")
    suspend fun getHealth(): Response<HealthResponseDto>
}
