package com.peakconnect.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.peakconnect.app.core.theme.PeakConnectTheme
import com.peakconnect.app.data.local.entity.GuideBookingEntity
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.feature.guide.home.GuideRequestCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GuideRequestCardUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleBooking = GuideBookingEntity(
        id = "req-1",
        slotId = "slot-1",
        activityTitle = "Kedarkantha Winter Trek",
        trekkerId = "trekker-99",
        trekkerName = "Ananya Roy",
        guideId = "guide-1",
        guideName = "Kshitiz",
        status = BookingStatus.PENDING_GUIDE_RESPONSE,
        date = LocalDateTime.of(2026, 12, 15, 9, 0),
        paymentOrderId = null,
        depositAmount = 1000.0,
        refundAmount = null,
        refundPercentage = null,
        statusSinceMillis = System.currentTimeMillis() - (5 * 60 * 1000L), // 5 min ago (15 min left)
        lastSyncMillis = System.currentTimeMillis()
    )

    @Test
    fun `guide request card displays trekker activity and triggers accept and decline actions`() {
        var clickedCardId: String? = null
        var acceptedCardId: String? = null
        var declinedCardId: String? = null

        composeTestRule.setContent {
            PeakConnectTheme {
                GuideRequestCard(
                    booking = sampleBooking,
                    onCardClick = { clickedCardId = it },
                    onAccept = { acceptedCardId = it },
                    onDecline = { declinedCardId = it }
                )
            }
        }

        // Trekker name
        composeTestRule.onNodeWithText("Trekker: Ananya Roy").assertIsDisplayed()

        // Activity title
        composeTestRule.onNodeWithText("Kedarkantha Winter Trek").assertIsDisplayed()

        // Approximate countdown (about 15 min left)
        composeTestRule.onNodeWithText("about 15 min left").assertIsDisplayed()

        // Accept and Decline buttons
        composeTestRule.onNodeWithText("Accept").assertIsDisplayed()
        composeTestRule.onNodeWithText("Decline").assertIsDisplayed()

        // Click Accept
        composeTestRule.onNodeWithText("Accept").performClick()
        assertEquals("req-1", acceptedCardId)

        // Click Decline
        composeTestRule.onNodeWithText("Decline").performClick()
        assertEquals("req-1", declinedCardId)

        // Click Card
        composeTestRule.onNodeWithText("Kedarkantha Winter Trek").performClick()
        assertEquals("req-1", clickedCardId)
    }

    @Test
    fun `guide request card shows expired window when time runs out`() {
        val expiredBooking = sampleBooking.copy(
            statusSinceMillis = System.currentTimeMillis() - (25 * 60 * 1000L) // 25 min ago (> 20 min)
        )

        composeTestRule.setContent {
            PeakConnectTheme {
                GuideRequestCard(
                    booking = expiredBooking,
                    onCardClick = {},
                    onAccept = {},
                    onDecline = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Window expired").assertIsDisplayed()
    }
}
