package com.peakconnect.app.feature.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakconnect.app.R
import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.network.AppError
import com.peakconnect.app.core.session.SessionManager
import com.peakconnect.app.core.session.SessionState
import com.peakconnect.app.data.dto.LoginRequest
import com.peakconnect.app.data.repository.AuthRepository
import com.peakconnect.app.domain.model.Role
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val emailErrorRes: Int? = null,
    val emailTouched: Boolean = false,
    val password: String = "",
    val passwordErrorRes: Int? = null,
    val passwordTouched: Boolean = false,
    val isLoading: Boolean = false,
    val backendError: String? = null,
    val successRole: Role? = null,
    val showSessionExpiredBanner: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChanged(value: String) {
        _uiState.update { current ->
            current.copy(
                email = value,
                emailTouched = true,
                emailErrorRes = validateEmail(value),
                backendError = null
            )
        }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { current ->
            current.copy(
                password = value,
                passwordTouched = true,
                passwordErrorRes = validatePassword(value),
                backendError = null
            )
        }
    }

    fun onDismissBackendError() {
        _uiState.update { it.copy(backendError = null) }
    }

    fun onDismissSessionExpiredBanner() {
        _uiState.update { it.copy(showSessionExpiredBanner = false) }
    }

    fun setSessionExpiredNotice(show: Boolean) {
        _uiState.update { it.copy(showSessionExpiredBanner = show) }
    }

    fun onSubmit(onSuccess: (Role) -> Unit = {}) {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password

        val emailError = validateEmail(email)
        val passwordError = validatePassword(password)

        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(
                    emailTouched = true,
                    emailErrorRes = emailError,
                    passwordTouched = true,
                    passwordErrorRes = passwordError
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, backendError = null) }

        viewModelScope.launch {
            val result = authRepository.login(
                LoginRequest(email = email, password = password)
            )

            when (result) {
                is ApiResult.Success -> {
                    val role = (sessionManager.sessionState.value as? SessionState.LoggedIn)?.role ?: Role.TREKKER
                    _uiState.update { it.copy(isLoading = false, successRole = role) }
                    onSuccess(role)
                }
                is ApiResult.Error -> {
                    val errorMsg = when (result.type) {
                        is AppError.Timeout -> "Server is waking up, please try again in a moment"
                        is AppError.NoInternet -> "No internet connection. Please check your network."
                        else -> result.message
                    }
                    _uiState.update { it.copy(isLoading = false, backendError = errorMsg) }
                }
                is ApiResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            backendError = "No internet connection. Please check your network."
                        )
                    }
                }
            }
        }
    }

    private fun validateEmail(email: String): Int? {
        val trimmed = email.trim()
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        val isValid = try {
            Patterns.EMAIL_ADDRESS?.matcher(trimmed)?.matches() ?: emailRegex.matches(trimmed)
        } catch (_: Exception) {
            emailRegex.matches(trimmed)
        }
        return when {
            trimmed.isEmpty() -> R.string.login_error_empty_email
            !isValid -> R.string.login_error_invalid_email
            else -> null
        }
    }

    private fun validatePassword(password: String): Int? {
        return when {
            password.isEmpty() -> R.string.login_error_empty_password
            else -> null
        }
    }
}
