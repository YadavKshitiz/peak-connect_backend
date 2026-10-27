package com.peakconnect.app.data.repository

import androidx.paging.PagingSource
import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.network.ErrorMapper
import com.peakconnect.app.core.network.WakeUpRetryHelper
import com.peakconnect.app.core.util.IoDispatcher
import com.peakconnect.app.data.api.PeakConnectApi
import com.peakconnect.app.data.local.dao.ActivityDao
import com.peakconnect.app.data.local.dao.ActivityFilter
import com.peakconnect.app.data.local.dao.getActivitiesPaged
import com.peakconnect.app.data.local.dao.syncActivitiesWithSlots
import com.peakconnect.app.data.local.entity.ActivityEntity
import com.peakconnect.app.data.local.entity.ActivityWithSlots
import com.peakconnect.app.data.local.entity.SlotEntity
import com.peakconnect.app.data.local.entity.toDomain
import com.peakconnect.app.domain.model.Activity
import com.peakconnect.app.domain.model.parseRiskLevel
import com.peakconnect.app.domain.model.toDomain
import com.peakconnect.app.domain.model.toLocalDateTimeOrNow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface ActivityRepository {
    val lastUpdatedFlow: Flow<Long?>
    fun getActivitiesPagingSource(filter: ActivityFilter = ActivityFilter()): PagingSource<Int, ActivityEntity>
    fun getActivityWithSlots(id: String): Flow<ActivityWithSlots?>
    suspend fun getActivity(id: String): ApiResult<Activity>
    suspend fun refreshActivities(location: String? = null, date: String? = null): ApiResult<Unit>
    suspend fun hasCachedActivities(): Boolean
}

@Singleton
class ActivityRepositoryImpl @Inject constructor(
    private val api: PeakConnectApi,
    private val activityDao: ActivityDao,
    private val wakeUpRetryHelper: WakeUpRetryHelper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ActivityRepository {

    override suspend fun hasCachedActivities(): Boolean = withContext(ioDispatcher) {
        activityDao.getActivityCount() > 0
    }

    private val _lastUpdatedFlow = MutableStateFlow<Long?>(null)
    override val lastUpdatedFlow: Flow<Long?> = _lastUpdatedFlow.asStateFlow()

    override fun getActivitiesPagingSource(filter: ActivityFilter): PagingSource<Int, ActivityEntity> {
        return activityDao.getActivitiesPaged(filter)
    }

    override fun getActivityWithSlots(id: String): Flow<ActivityWithSlots?> {
        return activityDao.getActivityWithSlotsFlow(id)
    }

    override suspend fun getActivity(id: String): ApiResult<Activity> = withContext(ioDispatcher) {
        wakeUpRetryHelper.withWakeUpRetry {
            try {
                val response = api.getActivity(id)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val activityDomain = body.toDomain()
                        val now = System.currentTimeMillis()
                        val activityEntity = ActivityEntity(
                            id = body.id,
                            title = body.title,
                            description = body.description,
                            location = body.location,
                            latitude = body.latitude,
                            longitude = body.longitude,
                            difficulty = body.difficultyLevel,
                            basePrice = body.basePrice,
                            cancellationPolicy = body.cancellationPolicy,
                            minSlotPrice = activityDomain.minSlotPrice,
                            lastUpdated = now
                        )
                        val slotEntities = body.slots.map { slotDto ->
                            SlotEntity(
                                id = slotDto.id,
                                activityId = slotDto.activityId,
                                date = slotDto.date.toLocalDateTimeOrNow(),
                                capacity = slotDto.capacity,
                                currentOccupancy = slotDto.currentOccupancy,
                                season = slotDto.season,
                                currentPrice = slotDto.currentPrice,
                                riskLevel = parseRiskLevel(slotDto.riskLevel)
                            )
                        }
                        activityDao.syncActivitiesWithSlots(listOf(activityEntity), slotEntities)
                        ApiResult.Success(activityDomain)
                    } else {
                        ErrorMapper.mapHttpError(response.code(), "Empty activity response")
                    }
                } else {
                    // Check local cache before returning error
                    val local = activityDao.getActivityWithSlots(id)
                    if (local != null) {
                        ApiResult.Success(local.toDomain())
                    } else {
                        ErrorMapper.mapHttpError(response.code(), response.errorBody()?.string())
                    }
                }
            } catch (e: Exception) {
                val local = activityDao.getActivityWithSlots(id)
                if (local != null) {
                    ApiResult.Success(local.toDomain())
                } else {
                    ErrorMapper.mapThrowable(e)
                }
            }
        }
    }

    override suspend fun refreshActivities(location: String?, date: String?): ApiResult<Unit> = withContext(ioDispatcher) {
        wakeUpRetryHelper.withWakeUpRetry {
            try {
                val response = api.getActivities(location = location, date = date)
                if (response.isSuccessful) {
                    val body = response.body() ?: emptyList()
                    val now = System.currentTimeMillis()

                    val activityEntities = mutableListOf<ActivityEntity>()
                    val slotEntities = mutableListOf<SlotEntity>()

                    body.forEach { activityDto ->
                        val slots = activityDto.slots.map { slotDto ->
                            SlotEntity(
                                id = slotDto.id,
                                activityId = slotDto.activityId,
                                date = slotDto.date.toLocalDateTimeOrNow(),
                                capacity = slotDto.capacity,
                                currentOccupancy = slotDto.currentOccupancy,
                                season = slotDto.season,
                                currentPrice = slotDto.currentPrice,
                                riskLevel = parseRiskLevel(slotDto.riskLevel)
                            )
                        }
                        slotEntities.addAll(slots)

                        val minPrice = slots.minOfOrNull { it.currentPrice } ?: activityDto.basePrice
                        activityEntities.add(
                            ActivityEntity(
                                id = activityDto.id,
                                title = activityDto.title,
                                description = activityDto.description,
                                location = activityDto.location,
                                latitude = activityDto.latitude,
                                longitude = activityDto.longitude,
                                difficulty = activityDto.difficultyLevel,
                                basePrice = activityDto.basePrice,
                                cancellationPolicy = activityDto.cancellationPolicy,
                                minSlotPrice = minPrice,
                                lastUpdated = now
                            )
                        )
                    }

                    activityDao.syncActivitiesWithSlots(activityEntities, slotEntities)
                    _lastUpdatedFlow.value = now
                    ApiResult.Success(Unit)
                } else {
                    ErrorMapper.mapHttpError(response.code(), response.errorBody()?.string())
                }
            } catch (e: Exception) {
                // Failure does NOT wipe cache
                ErrorMapper.mapThrowable(e)
            }
        }
    }
}
