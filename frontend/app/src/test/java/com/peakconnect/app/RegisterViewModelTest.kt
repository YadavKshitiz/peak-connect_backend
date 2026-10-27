package com.peakconnect.app

import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.network.AppError
import com.peakconnect.app.core.session.SessionManager
import com.peakconnect.app.core.session.SessionState
import com.peakconnect.app.data.repository.AuthRepository
import com.peakconnect.app.domain.model.Role
import com.peakconnect.app.feature.auth.RegisterViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RegisterViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val sessionManager: SessionManager = mockk(relaxed = true)
    private val sessionStateFlow = MutableStateFlow<SessionState>(SessionState.LoggedOut)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { sessionManager.sessionState } returns sessionStateFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun register_validation_empty_fields_triggers_errors() = runTest {
        val viewModel = RegisterViewModel(authRepository, sessionManager)

        viewModel.onSubmit()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.nameTouched)
        assertNotNull(state.nameErrorRes)
        assertTrue(state.emailTouched)
        assertNotNull(state.emailErrorRes)
        assertTrue(state.passwordTouched)
        assertNotNull(state.passwordErrorRes)
        assertTrue(state.confirmPasswordTouched)
        assertNotNull(state.confirmPasswordErrorRes)
        assertFalse(state.isLoading)
    }

    @Test
    fun register_validation_password_too_short_and_mismatch() = runTest {
        val viewModel = RegisterViewModel(authRepository, sessionManager)

        viewModel.onNameChanged("Alex User")
        viewModel.onEmailChanged("alex@example.com")
        viewModel.onPasswordChanged("123") // too short
        viewModel.onConfirmPasswordChanged("123456") // mismatch

        viewModel.onSubmit()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.passwordErrorRes)
        assertEquals(R.string.register_error_password_too_short, state.passwordErrorRes)
        assertNotNull(state.confirmPasswordErrorRes)
        assertEquals(R.string.register_error_password_mismatch, state.confirmPasswordErrorRes)
    }

    @Test
    fun register_role_cannot_be_admin() = runTest {
        val viewModel = RegisterViewModel(authRepository, sessionManager)

        assertEquals(Role.TREKKER, viewModel.uiState.value.selectedRole)

        // Attempting to select ADMIN must be strictly ignored
        viewModel.onRoleSelected(Role.ADMIN)
        assertNotEquals(Role.ADMIN, viewModel.uiState.value.selectedRole)
        assertEquals(Role.TREKKER, viewModel.uiState.value.selectedRole)

        // Select GUIDE
        viewModel.onRoleSelected(Role.GUIDE)
        assertEquals(Role.GUIDE, viewModel.uiState.value.selectedRole)

        // Attempting ADMIN again must be ignored
        viewModel.onRoleSelected(Role.ADMIN)
        assertEquals(Role.GUIDE, viewModel.uiState.value.selectedRole)
    }

    @Test
    fun register_success_auto_login() = runTest {
        val viewModel = RegisterViewModel(authRepository, sessionManager)
        sessionStateFlow.value = SessionState.LoggedIn(role = Role.TREKKER, userId = "10", email = "newuser@example.com")
        coEvery { authRepository.register(any()) } returns ApiResult.Success(Unit)

        var routedRole: Role? = null
        viewModel.onNameChanged("New Trekker")
        viewModel.onEmailChanged("newuser@example.com")
        viewModel.onPasswordChanged("Secret123")
        viewModel.onConfirmPasswordChanged("Secret123")
        viewModel.onSubmit { routedRole = it }

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.backendError)
        assertEquals(Role.TREKKER, state.successRole)
        assertEquals(Role.TREKKER, routedRole)
    }

    @Test
    fun register_conflict_email_already_registered() = runTest {
        val viewModel = RegisterViewModel(authRepository, sessionManager)
        coEvery { authRepository.register(any()) } returns ApiResult.Error(
            AppError.Conflict("Email is already taken"),
            "Email is already taken"
        )

        var routeCalled = false
        viewModel.onNameChanged("Existing User")
        viewModel.onEmailChanged("existing@example.com")
        viewModel.onPasswordChanged("Secret123")
        viewModel.onConfirmPasswordChanged("Secret123")
        viewModel.onSubmit { routeCalled = true }

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(routeCalled)
        assertNull(state.successRole)
        assertTrue(state.backendError?.contains("Email is already registered") == true)
    }
}
