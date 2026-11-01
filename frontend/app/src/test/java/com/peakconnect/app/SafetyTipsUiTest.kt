package com.peakconnect.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.peakconnect.app.core.theme.PeakConnectTheme
import com.peakconnect.app.feature.safety.SafetyTipsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SafetyTipsUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun safetyTipsScreen_displaysEmergencyAndTips() {
        composeTestRule.setContent {
            PeakConnectTheme {
                SafetyTipsScreen(onBack = {})
            }
        }

        // Title
        composeTestRule.onNodeWithText("Trek safety tips").assertExists()

        // Emergency section & 112 button
        composeTestRule.onNodeWithText("Emergency Assistance (India)").assertExists()
        composeTestRule.onNodeWithText("112").assertExists()
        composeTestRule.onNodeWithText("Call 112 (Emergency)").assertExists()

        // Key tips
        composeTestRule.onNodeWithText("1. Monitor Mountain Weather").assertExists()
        composeTestRule.onNodeWithText("2. Stay Hydrated & Purify Water").assertExists()
        composeTestRule.onNodeWithText("3. Acclimatise Gradually").assertExists()
        composeTestRule.onNodeWithText("4. Share Your Trek Plan").assertExists()
    }
}
