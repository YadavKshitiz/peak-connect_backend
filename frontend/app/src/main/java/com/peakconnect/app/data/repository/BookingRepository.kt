package com.peakconnect.app.data.repository

import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.network.ErrorMapper
import com.peakconnect.app.core.network.WakeUpRetryHelper
import com.peakconnect.app.core.util.IoDispatcher
import com.peakconnect.app.data.api.PeakConnectApi
import com.peakconnect.app.data.dto.BookingConfirmDto
import com.peakconnect.app.data.dto.BookingRequestDto
import com.peakconnect.app.data.local.dao.BookingDao
import com.peakconnect.app.data.local.dao.GuideDao
import com.peakconnect.app.data.local.dao.UserContentDao
import com.peakconnect.app.data.local.dao.syncBookings
import com.peakconnect.app.data.local.dao.syncGuideBookings
import com.peakconnect.app.data.local.entity.BookingEntity
import com.peakconnect.app.data.local.entity.GuideBookingEntity
import com.peakconnect.app.data.local.entity.WaitlistEntryEntity
import com.peakconnect.app.data.local.entity.toDomain
import com.peakconnect.app.domain.model.Booking
import com.peakconnect.app.domain.model.MatchedGuide
import com.peakconnect.app.domain.model.toDomain
import com.peakconnect.app.domain.model.toLocalDateTimeOrNow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

interface BookingRepository {
    val lastUpdatedFlow: Flow<Long?>
    fun getMyBookingsFlow(): Flow<List<BookingEntity>>
    fun getBookingFlow(id: String): Flow<BookingEntity?>
    suspend fun refreshMyBookings(): ApiResult<Unit>
    suspend fun requestMatches(
        slotId: String,
        skillWeight: Double? = null,
        locationWeight: Double? = null,
        languageWeight: Double? = null
    ): ApiResult<List<MatchedGuide>>
    suspend fun confirmBooking(slotId: String, guideId: String): ApiResult<Booking>
    suspend fun cancelBooking(bookingId: String): ApiResult<Booking>
    suspend fun acceptBooking(bookingId: String): ApiResult<Booking>
    suspend fun declineBooking(bookingId: String): ApiResult<Booking>
    suspend fun markNoShow(bookingId: String): ApiResult<Booking>
    suspend fun joinWaitlist(
        slotId: String,
        guideId: String,
        activityTitle: String,
        slotDate: LocalDateTime
    ): ApiResult<String>
}

@Singleton
class BookingRepositoryImpl @Inject constructor(
    private val api: PeakConnectApi,
    private val bookingDao: BookingDao,
    private val guideDao: GuideDao,
    private val userContentDao: UserContentDao,
    private val wakeUpRetryHelper: WakeUpRetryHelper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BookingRepository {

    private val _lastUpdatedFlow = MutableStateFlow<Long?>(null)
    override val lastUpdatedFlow: Flow<Long?> = _lastUpdatedFlow.asStateFlow()

    override fun getMyBookingsFlow(): Flow<List<BookingEntity>> {
        return bookingDao.getMyBookingsFlow()
    }

    override fun getBookingFlow(id: String): Flow<BookingEntity?> {
        return bookingDao.getBookingFlow(id)
    }

    override suspend fun refreshMyBookings(): ApiResult<Unit> = withContext(ioDispatcher) {
        wakeUpRetryHelper.withWakeUpRetry {
            try {
                val response = api.getMyBookings()
                if (response.isSuccessful) {
                    val body = response.body() ?: emptyList()
                    val now = System.currentTimeMillis()
                    bookingDao.syncBookings(body, now)
                    _lastUpdatedFlow.value = now
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

    override suspend fun requestMatches(
        slotId: String,
        skillWeight: Double?,
        locationWeight: Double?,
        languageWeight: Double?
    ): ApiResult<List<MatchedGuide>> = withContext(ioDispatcher) {
        wakeUpRetryHelper.withWakeUpRetry {
            try {
                val response = api.requestBooking(
                    BookingRequestDto(
                        slotId = slotId,
                        skillWeight = skillWeight,
                        locationWeight = locationWeight,
                        languageWeight = languageWeight
                    )
                )
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    ApiResult.Success(list.map { it.toDomain() })
                } else {
                    ErrorMapper.mapHttpError(response.code(), response.errorBody()?.string())
                }
            } catch (e: Exception) {
                ErrorMapper.mapThrowable(e)
            }
        }
    }

    override suspend fun confirmBooking(slotId: String, guideId: String): ApiResult<Booking> = withContext(ioDispatcher) {
        try {
            val response = api.confirmBooking(BookingConfirmDto(slotId = slotId, guideId = guideId))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val now = System.currentTimeMillis()
                    val entity = BookingEntity(
                        id = body.id,
                        slotId = body.slotId,
                        activityTitle = body.activityTitle,
                        trekkerId = body.trekkerId,
                        trekkerName = body.trekkerName,
                        guideId = body.guideId,
                        guideName = body.guideName,
                        status = body.status,
                        date = body.date.toLocalDateTimeOrNow(),
                        paymentOrderId = body.paymentOrderId,
                        depositAmount = body.depositAmount,
                        refundAmount = body.refundAmount,
                        refundPercentage = body.refundPercentage,
                        statusSinceMillis = now,
                        lastSyncMillis = now
                    )
                    bookingDao.insertBooking(entity)
                    ApiResult.Success(entity.toDomain())
                } else {
                    ErrorMapper.mapHttpError(response.code(), "Empty confirmation response")
                }
            } else {
                ErrorMapper.mapHttpError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ErrorMapper.mapThrowable(e)
        }
    }

    override suspend fun cancelBooking(bookingId: String): ApiResult<Booking> = withContext(ioDispatcher) {
        try {
            val response = api.cancelBooking(bookingId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val now = System.currentTimeMillis()
                    val existing = bookingDao.getBookingById(body.id)
                    val statusSince = if (existing != null && existing.status == body.status) {
                        existing.statusSinceMillis
                    } else {
                        now
                    }
                    val entity = BookingEntity(
                        id = body.id,
                        slotId = body.slotId,
                        activityTitle = body.activityTitle,
                        trekkerId = body.trekkerId,
                        trekkerName = body.trekkerName,
                        guideId = body.guideId,
                        guideName = body.guideName,
                        status = body.status,
                        date = body.date.toLocalDateTimeOrNow(),
                        paymentOrderId = body.paymentOrderId,
                        depositAmount = body.depositAmount,
                        refundAmount = body.refundAmount,
                        refundPercentage = body.refundPercentage,
                        statusSinceMillis = statusSince,
                        lastSyncMillis = now
                    )
                    bookingDao.insertBooking(entity)
                    ApiResult.Success(entity.toDomain())
                } else {
                    ErrorMapper.mapHttpError(response.code(), "Empty cancellation response")
                }
            } else {
                ErrorMapper.mapHttpError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ErrorMapper.mapThrowable(e)
        }
    }

    override suspend fun acceptBooking(bookingId: String): ApiResult<Booking> = withContext(ioDispatcher) {
        try {
            val response = api.acceptBooking(bookingId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val now = System.currentTimeMillis()
                    val entity = GuideBookingEntity(
                        id = body.id,
                        slotId = body.slotId,
                        activityTitle = body.activityTitle,
                        trekkerId = body.trekkerId,
                        trekkerName = body.trekkerName,
                        guideId = body.guideId,
                        guideName = body.guideName,
                        status = body.status,
                        date = body.date.toLocalDateTimeOrNow(),
                        paymentOrderId = body.paymentOrderId,
                        depositAmount = body.depositAmount,
                        refundAmount = body.refundAmount,
                        refundPercentage = body.refundPercentage,
                        statusSinceMillis = now,
                        lastSyncMillis = now
                    )
                    guideDao.insertGuideBooking(entity)
                    ApiResult.Success(entity.toDomain())
                } else {
                    ErrorMapper.mapHttpError(response.code(), "Empty accept response")
                }
            } else {
                ErrorMapper.mapHttpError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ErrorMapper.mapThrowable(e)
        }
    }

    override suspend fun declineBooking(bookingId: String): ApiResult<Booking> = withContext(ioDispatcher) {
        try {
            val response = api.declineBooking(bookingId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val now = System.currentTimeMillis()
                    val entity = GuideBookingEntity(
                        id = body.id,
                        slotId = body.slotId,
                        activityTitle = body.activityTitle,
                        trekkerId = body.trekkerId,
                        trekkerName = body.trekkerName,
                        guideId = body.guideId,
                        guideName = body.guideName,
                        status = body.status,
                        date = body.date.toLocalDateTimeOrNow(),
                        paymentOrderId = body.paymentOrderId,
                        depositAmount = body.depositAmount,
                        refundAmount = body.refundAmount,
                        refundPercentage = body.refundPercentage,
                        statusSinceMillis = now,
                        lastSyncMillis = now
                    )
                    guideDao.insertGuideBooking(entity)
                    ApiResult.Success(entity.toDomain())
                } else {
                    ErrorMapper.mapHttpError(response.code(), "Empty decline response")
                }
            } else {
                ErrorMapper.mapHttpError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ErrorMapper.mapThrowable(e)
        }
    }

    override suspend fun markNoShow(bookingId: String): ApiResult<Booking> = withContext(ioDispatcher) {
        try {
            val response = api.markNoShow(bookingId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val now = System.currentTimeMillis()
                    val entity = GuideBookingEntity(
                        id = body.id,
                        slotId = body.slotId,
                        activityTitle = body.activityTitle,
                        trekkerId = body.trekkerId,
                        trekkerName = body.trekkerName,
                        guideId = body.guideId,
                        guideName = body.guideName,
                        status = body.status,
                        date = body.date.toLocalDateTimeOrNow(),
                        paymentOrderId = body.paymentOrderId,
                        depositAmount = body.depositAmount,
                        refundAmount = body.refundAmount,
                        refundPercentage = body.refundPercentage,
                        statusSinceMillis = now,
                        lastSyncMillis = now
                    )
                    guideDao.insertGuideBooking(entity)
                    ApiResult.Success(entity.toDomain())
                } else {
                    ErrorMapper.mapHttpError(response.code(), "Empty no-show response")
                }
            } else {
                ErrorMapper.mapHttpError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ErrorMapper.mapThrowable(e)
        }
    }

    override suspend fun joinWaitlist(
        slotId: String,
        guideId: String,
        activityTitle: String,
        slotDate: LocalDateTime
    ): ApiResult<String> = withContext(ioDispatcher) {
        try {
            val response = api.joinWaitlist(BookingConfirmDto(slotId = slotId, guideId = guideId))
            if (response.isSuccessful) {
                val body = response.body()
                val message = body?.message ?: "Successfully joined waitlist"
                userContentDao.insertWaitlistEntry(
                    WaitlistEntryEntity(
                        slotId = slotId,
                        activityTitle = activityTitle,
                        slotDate = slotDate,
                        joinedAt = System.currentTimeMillis()
                    )
                )
                ApiResult.Success(message)
            } else {
                ErrorMapper.mapHttpError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            ErrorMapper.mapThrowable(e)
        }
    }
}
