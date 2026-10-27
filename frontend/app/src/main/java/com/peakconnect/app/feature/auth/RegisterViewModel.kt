package com.peakconnect.app.feature.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakconnect.app.R
import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.network.AppError
import com.peakconnect.app.core.session.SessionManager
import com.peakconnect.app.core.session.SessionState
import com.peakconnect.app.data.dto.RegisterRequest
import com.peakconnect.app.data.repository.AuthRepository
import com.peakconnect.app.domain.model.Role
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val name: String = "",
    val nameErrorRes: Int? = null,
    val nameTouched: Boolean = false,
    val email: String = "",
    val emailErrorRes: Int? = null,
    val emailTouched: Boolean = false,
    val password: String = "",
    val passwordErrorRes: Int? = null,
    val passwordTouched: Boolean = false,
    val confirmPassword: String = "",
    val confirmPasswordErrorRes: Int? = null,
    val confirmPasswordTouched: Boolean = false,
    val selectedRole: Role = Role.TREKKER,
    val isLoading: Boolean = false,
    val backendError: String? = null,
    val successRole: Role? = null
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChanged(value: String) {
        _uiState.update { current ->
            current.copy(
                name = value,
                nameTouched = true,
                nameErrorRes = validateName(value),
                backendError = null
            )
        }
    }

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
            val passwordError = validatePassword(value)
            val confirmError = if (current.confirmPasswordTouched) {
                validateConfirmPassword(value, current.confirmPassword)
            } else {
                current.confirmPasswordErrorRes
            }
            current.copy(
                password = value,
                passwordTouched = true,
                passwordErrorRes = passwordError,
                confirmPasswordErrorRes = confirmError,
                backendError = null
            )
        }
    }

    fun onConfirmPasswordChanged(value: String) {
        _uiState.update { current ->
            current.copy(
                confirmPassword = value,
                confirmPasswordTouched = true,
                confirmPasswordErrorRes = validateConfirmPassword(current.password, value),
                backendError = null
            )
        }
    }

    fun onRoleSelected(role: Role) {
        // NEVER allow selecting ADMIN
        if (role == Role.ADMIN) return

        _uiState.update { current ->
            current.copy(
                selectedRole = role,
                backendError = null
            )
        }
    }

    fun onDismissBackendError() {
        _uiState.update { it.copy(backendError = null) }
    }

    fun onSubmit(onSuccess: (Role) -> Unit = {}) {
        val current = _uiState.value
        val name = current.name.trim()
        val email = current.email.trim()
        val password = current.password
        val confirmPassword = current.confirmPassword

        val nameError = validateName(name)
        val emailError = validateEmail(email)
        val passwordError = validatePassword(password)
        val confirmPasswordError = validateConfirmPassword(password, confirmPassword)

        if (nameError != null || emailError != null || passwordError != null || confirmPasswordError != null) {
            _uiState.update {
                it.copy(
                    nameTouched = true,
                    nameErrorRes = nameError,
                    emailTouched = true,
                    emailErrorRes = emailError,
                    passwordTouched = true,
                    passwordErrorRes = passwordError,
                    confirmPasswordTouched = true,
                    confirmPasswordErrorRes = confirmPasswordError
                )
            }
            return
        }

        val chosenRole = if (current.selectedRole == Role.ADMIN) Role.TREKKER else current.selectedRole

        _uiState.update { it.copy(isLoading = true, backendError = null) }

        viewModelScope.launch {
            val result = authRepository.register(
                RegisterRequest(
                    name = name,
                    email = email,
                    password = password,
                    role = chosenRole
                )
            )

            when (result) {
                is ApiResult.Success -> {
                    val role = (sessionManager.sessionState.value as? SessionState.LoggedIn)?.role ?: chosenRole
                    _uiState.update { it.copy(isLoading = false, successRole = role) }
                    onSuccess(role)
                }
                is ApiResult.Error -> {
                    val errorMsg = when {
                        result.type is AppError.Conflict -> "Email is already registered. Please log in or use a different email."
                        result.type is AppError.Timeout -> "Server is waking up, please try again in a moment"
                        result.type is AppError.NoInternet -> "No internet connection. Please check your network."
                        result.message.contains("Email is already", ignoreCase = true) -> "Email is already registered. Please log in or use a different email."
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

    private fun validateName(name: String): Int? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> R.string.register_error_empty_name
            trimmed.length < 2 -> R.string.register_error_name_too_short
            else -> null
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
            password.length < 6 -> R.string.register_error_password_too_short
            else -> null
        }
    }

    private fun validateConfirmPassword(password: String, confirm: String): Int? {
        return when {
            confirm.isEmpty() -> R.string.login_error_empty_password
            password != confirm -> R.string.register_error_password_mismatch
            else -> null
        }
    }
}
