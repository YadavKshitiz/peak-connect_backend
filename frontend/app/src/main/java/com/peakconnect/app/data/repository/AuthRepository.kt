package com.peakconnect.app.data.repository

import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.network.ErrorMapper
import com.peakconnect.app.core.network.WakeUpRetryHelper
import com.peakconnect.app.core.session.SessionManager
import com.peakconnect.app.core.session.SessionState
import com.peakconnect.app.core.util.IoDispatcher
import com.peakconnect.app.data.api.PeakConnectApi
import com.peakconnect.app.data.dto.LoginRequest
import com.peakconnect.app.data.dto.RegisterRequest
import com.peakconnect.app.data.local.dao.BookingDao
import com.peakconnect.app.data.local.dao.GuideDao
import com.peakconnect.app.data.local.dao.NotifiedEventDao
import com.peakconnect.app.data.local.dao.UserContentDao
import com.peakconnect.app.data.local.dao.clearUserSpecificData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    val sessionState: StateFlow<SessionState>
    suspend fun login(request: LoginRequest): ApiResult<Unit>
    suspend fun register(request: RegisterRequest): ApiResult<Unit>
    suspend fun logout()
    suspend fun isTokenExpired(): Boolean
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: PeakConnectApi,
    private val sessionManager: SessionManager,
    private val userContentDao: UserContentDao,
    private val bookingDao: BookingDao,
    private val guideDao: GuideDao,
    private val notifiedEventDao: NotifiedEventDao,
    private val wakeUpRetryHelper: WakeUpRetryHelper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AuthRepository {

    init {
        sessionManager.setOnLogoutCallback {
            clearLocalUserData()
        }
    }

    override val sessionState: StateFlow<SessionState> = sessionManager.sessionState

    override suspend fun login(request: LoginRequest): ApiResult<Unit> = withContext(ioDispatcher) {
        wakeUpRetryHelper.withWakeUpRetry {
            try {
                val response = api.login(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.token.isNotBlank()) {
                        sessionManager.onLoginSuccess(body.token)
                        ApiResult.Success(Unit)
                    } else {
                        ErrorMapper.mapHttpError(response.code(), "Empty authentication token returned")
                    }
                } else {
                    ErrorMapper.mapHttpError(response.code(), response.errorBody()?.string())
                }
            } catch (e: Exception) {
                ErrorMapper.mapThrowable(e)
            }
        }
    }

    override suspend fun register(request: RegisterRequest): ApiResult<Unit> = withContext(ioDispatcher) {
        wakeUpRetryHelper.withWakeUpRetry {
            try {
                val response = api.register(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.token.isNotBlank()) {
                        sessionManager.onLoginSuccess(body.token)
                        ApiResult.Success(Unit)
                    } else {
                        ErrorMapper.mapHttpError(response.code(), "Empty authentication token returned")
                    }
                } else {
                    ErrorMapper.mapHttpError(response.code(), response.errorBody()?.string())
                }
            } catch (e: Exception) {
                ErrorMapper.mapThrowable(e)
            }
        }
    }

    override suspend fun logout() = withContext(ioDispatcher) {
        sessionManager.logout()
    }

    override suspend fun isTokenExpired(): Boolean = withContext(ioDispatcher) {
        sessionManager.isTokenExpired()
    }

    private suspend fun clearLocalUserData() {
        try {
            bookingDao.clearBookings()
            guideDao.clearGuideBookings()
            guideDao.clearGuideProfile()
            notifiedEventDao.clearAll()
            userContentDao.clearUserSpecificData()
            // TODO: Cancel any scheduled WorkManager periodic sync jobs (Task 13/14)
        } catch (_: Exception) {
        }
    }
}
