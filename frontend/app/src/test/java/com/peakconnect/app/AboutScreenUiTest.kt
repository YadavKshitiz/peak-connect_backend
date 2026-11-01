package com.peakconnect.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.peakconnect.app.core.theme.PeakConnectTheme
import com.peakconnect.app.feature.about.AboutScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AboutScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aboutScreen_displaysAllRequiredElements() {
        var backClicked = false

        composeTestRule.setContent {
            PeakConnectTheme {
                AboutScreen(
                    onBack = { backClicked = true }
                )
            }
        }

        // App name
        composeTestRule.onNodeWithText("PeakConnect").assertExists()

        // Attribution & Payments
        composeTestRule.onNodeWithText("Map data © OpenStreetMap contributors").assertExists()
        composeTestRule.onNodeWithText("Payments by Razorpay (test mode)").assertExists()

        // Links & actions
        composeTestRule.onNodeWithText("GitHub").assertExists()
        composeTestRule.onNodeWithText("Send feedback").assertExists()

        // Open Source Libraries title
        composeTestRule.onNodeWithText("Open Source Libraries").assertExists()
    }
}
