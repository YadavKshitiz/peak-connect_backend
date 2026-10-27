package com.peakconnect.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.session.SessionManager
import com.peakconnect.app.core.theme.PeakConnectTheme
import com.peakconnect.app.data.repository.AuthRepository
import com.peakconnect.app.feature.auth.LoginScreen
import com.peakconnect.app.feature.auth.LoginViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h900dp-xxhdpi")
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val sessionManager: SessionManager = mockk(relaxed = true)

    @Test
    fun `clicking login without filling fields shows inline validation errors`() {
        val viewModel = LoginViewModel(authRepository, sessionManager)

        composeTestRule.setContent {
            PeakConnectTheme {
                LoginScreen(
                    viewModel = viewModel,
                    onSuccess = {},
                    onNavigateToRegister = {}
                )
            }
        }

        // Before click, validation error messages should not exist
        composeTestRule.onNodeWithText("Email is required").assertDoesNotExist()
        composeTestRule.onNodeWithText("Password cannot be blank").assertDoesNotExist()

        // Click submit button
        composeTestRule.onNodeWithText("Log in").performClick()
        composeTestRule.waitForIdle()

        // Validation errors should now be displayed
        composeTestRule.onNodeWithText("Email is required").assertExists().assertIsDisplayed()
        composeTestRule.onNodeWithText("Password cannot be blank").assertExists().assertIsDisplayed()
    }

    @Test
    fun `login button is disabled and in loading state while submitting`() {
        val loginDeferred = CompletableDeferred<ApiResult<Unit>>()
        coEvery { authRepository.login(any()) } coAnswers { loginDeferred.await() }

        val viewModel = LoginViewModel(authRepository, sessionManager)

        composeTestRule.setContent {
            PeakConnectTheme {
                LoginScreen(
                    viewModel = viewModel,
                    onSuccess = {},
                    onNavigateToRegister = {}
                )
            }
        }

        // Populate valid credentials
        viewModel.onEmailChanged("alex@example.com")
        viewModel.onPasswordChanged("securepassword")
        composeTestRule.waitForIdle()

        // Button is initially enabled
        composeTestRule.onNodeWithText("Log in").assertIsEnabled()

        // Click Log in
        composeTestRule.onNodeWithText("Log in").performClick()
        composeTestRule.waitForIdle()

        // When isLoading is true: Login button text is replaced with loading indicator
        // and the "Log in" text does not exist
        composeTestRule.onNodeWithText("Log in").assertDoesNotExist()

        // Finish network call
        loginDeferred.complete(ApiResult.Success(Unit))
        composeTestRule.waitForIdle()
    }
}
