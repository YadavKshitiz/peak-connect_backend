package com.peakconnect.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.peakconnect.app.core.theme.PeakConnectTheme
import com.peakconnect.app.core.util.LocalIsOnline
import com.peakconnect.app.data.local.entity.SlotEntity
import com.peakconnect.app.domain.model.RiskLevel
import com.peakconnect.app.domain.model.Season
import com.peakconnect.app.feature.trekker.detail.SlotCard
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SlotCardStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val availableSlot = SlotEntity(
        id = "slot-avail",
        activityId = "act-1",
        date = LocalDateTime.of(2026, 11, 20, 6, 0),
        capacity = 10,
        currentOccupancy = 4, // 6 spots left
        season = Season.PEAK,
        currentPrice = BigDecimal("1500.00"),
        riskLevel = RiskLevel.LOW
    )

    private val fullSlot = SlotEntity(
        id = "slot-full",
        activityId = "act-1",
        date = LocalDateTime.of(2026, 11, 25, 7, 30),
        capacity = 8,
        currentOccupancy = 8, // 0 spots left
        season = Season.SHOULDER,
        currentPrice = BigDecimal("1200.00"),
        riskLevel = RiskLevel.MODERATE
    )

    @Test
    fun `available slot shows Request booking button and spots left`() {
        var bookingRequested = false

        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsOnline provides true) {
                PeakConnectTheme {
                    SlotCard(
                        slot = availableSlot,
                        isOnWaitlist = false,
                        onRiskClick = {},
                        onRequestBooking = { bookingRequested = true },
                        onJoinWaitlist = {}
                    )
                }
            }
        }

        // Spot count is displayed
        composeTestRule.onNodeWithText("6 of 10 spots left").assertExists().assertIsDisplayed()

        // Request booking button is displayed and enabled
        composeTestRule.onNodeWithText("Request booking")
            .assertExists()
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        assertTrue(bookingRequested)
    }

    @Test
    fun `full slot not on waitlist shows Join waitlist button and waitlist explanation`() {
        var waitlistJoined = false

        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsOnline provides true) {
                PeakConnectTheme {
                    SlotCard(
                        slot = fullSlot,
                        isOnWaitlist = false,
                        onRiskClick = {},
                        onRequestBooking = {},
                        onJoinWaitlist = { waitlistJoined = true }
                    )
                }
            }
        }

        // Spot count indicates full
        composeTestRule.onNodeWithText("Full · 0 of 8 spots left").assertExists().assertIsDisplayed()

        // Join waitlist button is displayed and clickable
        composeTestRule.onNodeWithText("Join waitlist")
            .assertExists()
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        assertTrue(waitlistJoined)

        // Waitlist explanation is displayed
        composeTestRule.onNodeWithText(
            "If someone cancels, a booking request is sent automatically to the guide you choose. You’ll be asked to pay after they accept."
        ).assertExists().assertIsDisplayed()
    }

    @Test
    fun `full slot already on waitlist shows disabled On the waitlist state`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsOnline provides true) {
                PeakConnectTheme {
                    SlotCard(
                        slot = fullSlot,
                        isOnWaitlist = true,
                        onRiskClick = {},
                        onRequestBooking = {},
                        onJoinWaitlist = {}
                    )
                }
            }
        }

        // Disabled "On the waitlist" state is displayed
        composeTestRule.onNodeWithText("On the waitlist")
            .assertExists()
            .assertIsDisplayed()
            .assertIsNotEnabled()

        // Join waitlist button should not exist
        composeTestRule.onNodeWithText("Join waitlist").assertDoesNotExist()
    }

    @Test
    fun `offline mode disables booking and shows helper message`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsOnline provides false) {
                PeakConnectTheme {
                    SlotCard(
                        slot = availableSlot,
                        isOnWaitlist = false,
                        onRiskClick = {},
                        onRequestBooking = {},
                        onJoinWaitlist = {}
                    )
                }
            }
        }

        // Button is disabled
        composeTestRule.onNodeWithText("Request booking")
            .assertExists()
            .assertIsNotEnabled()

        // Offline helper text is displayed
        composeTestRule.onNodeWithText("Connect to the internet to book")
            .assertExists()
            .assertIsDisplayed()
    }
}
