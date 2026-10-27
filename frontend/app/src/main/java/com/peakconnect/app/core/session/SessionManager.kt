package com.peakconnect.app.core.session

import com.peakconnect.app.domain.model.Role
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SessionState {
    data object LoggedOut : SessionState
    data class LoggedIn(
        val role: Role,
        val userId: String,
        val email: String,
        val name: String? = null
    ) : SessionState
}

@Singleton
class SessionManager @Inject constructor(
    private val tokenStore: TokenStore
) {
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.LoggedOut)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private var onLogoutCallback: (suspend () -> Unit)? = null

    fun setOnLogoutCallback(callback: suspend () -> Unit) {
        this.onLogoutCallback = callback
    }

    suspend fun initialize() {
        val token = tokenStore.getToken()
        if (token != null && !JwtDecoder.isExpired(token)) {
            val payload = JwtDecoder.decode(token)
            if (payload != null) {
                _sessionState.value = SessionState.LoggedIn(
                    role = payload.role,
                    userId = payload.userId,
                    email = payload.email,
                    name = payload.name
                )
                return
            }
        }
        _sessionState.value = SessionState.LoggedOut
    }

    suspend fun onLoginSuccess(token: String) {
        tokenStore.saveToken(token)
        val payload = JwtDecoder.decode(token)
        if (payload != null) {
            _sessionState.value = SessionState.LoggedIn(
                role = payload.role,
                userId = payload.userId,
                email = payload.email,
                name = payload.name
            )
        }
    }

    suspend fun isTokenExpired(): Boolean {
        val token = tokenStore.getToken() ?: return true
        return JwtDecoder.isExpired(token)
    }

    suspend fun logout() {
        tokenStore.clearToken()
        _sessionState.value = SessionState.LoggedOut
        onLogoutCallback?.invoke()
    }
}
