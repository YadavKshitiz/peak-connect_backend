package com.peakconnect.app

import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.network.AppError
import com.peakconnect.app.core.session.SessionManager
import com.peakconnect.app.core.session.SessionState
import com.peakconnect.app.data.dto.LoginRequest
import com.peakconnect.app.data.repository.AuthRepository
import com.peakconnect.app.domain.model.Role
import com.peakconnect.app.feature.auth.LoginViewModel
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
class LoginViewModelTest {

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
    fun login_success_trekker() = runTest {
        val viewModel = LoginViewModel(authRepository, sessionManager)
        sessionStateFlow.value = SessionState.LoggedIn(role = Role.TREKKER, userId = "1", email = "alex@example.com")
        coEvery { authRepository.login(any()) } returns ApiResult.Success(Unit)

        var routedRole: Role? = null
        viewModel.onEmailChanged("alex@example.com")
        viewModel.onPasswordChanged("password123")
        viewModel.onSubmit { routedRole = it }

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.backendError)
        assertEquals(Role.TREKKER, viewModel.uiState.value.successRole)
        assertEquals(Role.TREKKER, routedRole)
    }

    @Test
    fun login_success_guide() = runTest {
        val viewModel = LoginViewModel(authRepository, sessionManager)
        sessionStateFlow.value = SessionState.LoggedIn(role = Role.GUIDE, userId = "2", email = "guide@example.com")
        coEvery { authRepository.login(any()) } returns ApiResult.Success(Unit)

        var routedRole: Role? = null
        viewModel.onEmailChanged("guide@example.com")
        viewModel.onPasswordChanged("guidepass")
        viewModel.onSubmit { routedRole = it }

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.backendError)
        assertEquals(Role.GUIDE, viewModel.uiState.value.successRole)
        assertEquals(Role.GUIDE, routedRole)
    }

    @Test
    fun login_bad_credentials() = runTest {
        val viewModel = LoginViewModel(authRepository, sessionManager)
        coEvery { authRepository.login(any()) } returns ApiResult.Error(
            AppError.Unauthorized,
            "Incorrect email or password"
        )

        var routeCalled = false
        viewModel.onEmailChanged("alex@example.com")
        viewModel.onPasswordChanged("wrongpassword")
        viewModel.onSubmit { routeCalled = true }

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(routeCalled)
        assertNull(viewModel.uiState.value.successRole)
        assertEquals("Incorrect email or password", viewModel.uiState.value.backendError)
    }

    @Test
    fun login_timeout() = runTest {
        val viewModel = LoginViewModel(authRepository, sessionManager)
        coEvery { authRepository.login(any()) } returns ApiResult.Error(
            AppError.Timeout,
            "Timeout"
        )

        viewModel.onEmailChanged("alex@example.com")
        viewModel.onPasswordChanged("mypassword")
        viewModel.onSubmit()

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Server is waking up, please try again in a moment", viewModel.uiState.value.backendError)
    }

    @Test
    fun login_no_internet() = runTest {
        val viewModel = LoginViewModel(authRepository, sessionManager)
        coEvery { authRepository.login(any()) } returns ApiResult.NetworkError(
            java.io.IOException("No internet")
        )

        viewModel.onEmailChanged("alex@example.com")
        viewModel.onPasswordChanged("mypassword")
        viewModel.onSubmit()

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.backendError?.contains("No internet connection") == true)
    }

    @Test
    fun login_validation_empty_fields_show_errors() = runTest {
        val viewModel = LoginViewModel(authRepository, sessionManager)

        viewModel.onSubmit()

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.emailTouched)
        assertNotNull(viewModel.uiState.value.emailErrorRes)
        assertTrue(viewModel.uiState.value.passwordTouched)
        assertNotNull(viewModel.uiState.value.passwordErrorRes)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
