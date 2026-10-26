package com.peakconnect.app.data.repository

import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.network.ErrorMapper
import com.peakconnect.app.core.network.WakeUpRetryHelper
import com.peakconnect.app.core.util.IoDispatcher
import com.peakconnect.app.data.api.PeakConnectApi
import com.peakconnect.app.data.dto.GuideProfileUpdateRequest
import com.peakconnect.app.data.local.dao.GuideDao
import com.peakconnect.app.data.local.dao.syncGuideBookings
import com.peakconnect.app.data.local.entity.GuideBookingEntity
import com.peakconnect.app.data.local.entity.GuideProfileEntity
import com.peakconnect.app.data.local.entity.toDomain
import com.peakconnect.app.domain.model.GuideProfile
import com.peakconnect.app.domain.model.toDomain
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface GuideRepository {
    val lastUpdatedProfileFlow: Flow<Long?>
    val lastUpdatedBookingsFlow: Flow<Long?>
    fun getProfileFlow(): Flow<GuideProfileEntity?>
    suspend fun getProfile(): ApiResult<GuideProfile>
    suspend fun refreshProfile(): ApiResult<Unit>
    suspend fun updateProfile(request: GuideProfileUpdateRequest): ApiResult<GuideProfile>
    fun getGuideBookingsFlow(): Flow<List<GuideBookingEntity>>
    suspend fun refreshGuideBookings(): ApiResult<Unit>
}

@Singleton
class GuideRepositoryImpl @Inject constructor(
    private val api: PeakConnectApi,
    private val guideDao: GuideDao,
    private val wakeUpRetryHelper: WakeUpRetryHelper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : GuideRepository {

    private val _lastUpdatedProfileFlow = MutableStateFlow<Long?>(null)
    override val lastUpdatedProfileFlow: Flow<Long?> = _lastUpdatedProfileFlow.asStateFlow()

    private val _lastUpdatedBookingsFlow = MutableStateFlow<Long?>(null)
    override val lastUpdatedBookingsFlow: Flow<Long?> = _lastUpdatedBookingsFlow.asStateFlow()

    override fun getProfileFlow(): Flow<GuideProfileEntity?> {
        return guideDao.getGuideProfileFlow()
    }

    override suspend fun getProfile(): ApiResult<GuideProfile> = withContext(ioDispatcher) {
        val cached = guideDao.getGuideProfile()
        if (cached != null) {
            ApiResult.Success(cached.toDomain())
        } else {
            val refreshResult = refreshProfile()
            if (refreshResult is ApiResult.Success) {
                val updated = guideDao.getGuideProfile()
                if (updated != null) {
                    ApiResult.Success(updated.toDomain())
                } else {
                    ApiResult.Error(com.peakconnect.app.core.network.AppError.NotFound, "Guide profile not found")
                }
            } else if (refreshResult is ApiResult.Error) {
                refreshResult
            } else {
                ApiResult.Error(com.peakconnect.app.core.network.AppError.NoInternet, "Cannot load guide profile offline")
            }
        }
    }

    override suspend fun refreshProfile(): ApiResult<Unit> = withContext(ioDispatcher) {
        wakeUpRetryHelper.withWakeUpRetry {
            try {
                val response = api.getMyGuideProfile()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val now = System.currentTimeMillis()
                        val entity = GuideProfileEntity(
                            id = body.id,
                            name = body.name,
                            email = body.email,
                            skills = body.skills,
                            languages = body.languages,
                            location = body.location,
                            latitude = body.latitude,
                            longitude = body.longitude,
                            experienceLevel = body.experienceLevel,
                            isVerified = body.isVerified,
                            lastUpdated = now
                        )
                        guideDao.insertOrUpdateProfile(entity)
                        _lastUpdatedProfileFlow.value = now
                        ApiResult.Success(Unit)
                    } else {
                        ErrorMapper.mapHttpError(response.code(), "Empty guide profile response")
                    }
                } else {
                    ErrorMapper.mapHttpError(response.code(), response.errorBody()?.string())
                }
            } catch (e: Exception) {
                // Network failure does NOT wipe cache
                ErrorMapper.mapThrowable(e)
            }
        }
    }

    override suspend fun updateProfile(request: GuideProfileUpdateRequest): ApiResult<GuideProfile> = withContext(ioDispatcher) {
        try {
            val response = api.updateMyGuideProfile(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val now = System.currentTimeMillis()
                    val entity = GuideProfileEntity(
                        id = body.id,
                        name = body.name,
                        email = body.email,
                        skills = body.skills,
                        languages = body.languages,
                        location = body.location,
                        latitude = body.latitude,
                        longitude = body.longitude,
                        experienceLevel = body.experienceLevel,
                        isVerified = body.isVerified,
                        lastUpdated = now
                    )
                    guideDao.insertOrUpdateProfile(entity)
                    _lastUpdatedProfileFlow.value = now
                    ApiResult.Success(entity.toDomain())
                } else {
                    ErrorMapper.mapHttpError(response.code(), "Empty update profile response")
                }
            } else {
                ErrorMapper.mapHttpError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ErrorMapper.mapThrowable(e)
        }
    }

    override fun getGuideBookingsFlow(): Flow<List<GuideBookingEntity>> {
        return guideDao.getGuideBookingsFlow()
    }

    override suspend fun refreshGuideBookings(): ApiResult<Unit> = withContext(ioDispatcher) {
        wakeUpRetryHelper.withWakeUpRetry {
            try {
                val response = api.getGuideBookings()
                if (response.isSuccessful) {
                    val body = response.body() ?: emptyList()
                    val now = System.currentTimeMillis()
                    guideDao.syncGuideBookings(body, now)
                    _lastUpdatedBookingsFlow.value = now
                    ApiResult.Success(Unit)
                } else {
                    ErrorMapper.mapHttpError(response.code(), response.errorBody()?.string())
                }
            } catch (e: Exception) {
                // Network failure does NOT wipe cache
                ErrorMapper.mapThrowable(e)
            }
        }
    }
}
