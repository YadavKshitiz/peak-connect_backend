package com.peakconnect.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.peakconnect.app.core.theme.PeakConnectTheme
import com.peakconnect.app.core.ui.OnlineOnlyButton
import com.peakconnect.app.core.util.LocalIsOnline
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnlineOnlyButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `when LocalIsOnline provides true button is enabled and helper text is hidden`() {
        var clicked = false

        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsOnline provides true) {
                PeakConnectTheme {
                    OnlineOnlyButton(
                        text = "Book Now",
                        actionLabel = "book trek",
                        onClick = { clicked = true }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Book Now")
            .assertIsDisplayed()
            .assertIsEnabled()

        composeTestRule.onNodeWithText("Connect to the internet to book trek")
            .assertDoesNotExist()
    }

    @Test
    fun `when LocalIsOnline provides false button is disabled and helper text is shown`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsOnline provides false) {
                PeakConnectTheme {
                    OnlineOnlyButton(
                        text = "Book Now",
                        actionLabel = "book trek",
                        onClick = { }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Book Now")
            .assertIsDisplayed()
            .assertIsNotEnabled()

        composeTestRule.onNodeWithText("Connect to the internet to book trek")
            .assertIsDisplayed()
    }
}
