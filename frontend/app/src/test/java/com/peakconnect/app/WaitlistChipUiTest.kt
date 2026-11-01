package com.peakconnect.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.peakconnect.app.core.theme.PeakConnectTheme
import com.peakconnect.app.data.local.entity.WaitlistEntryEntity
import com.peakconnect.app.domain.model.WaitlistStatus
import com.peakconnect.app.feature.trekker.waitlist.WaitlistCard
import com.peakconnect.app.feature.trekker.waitlist.WaitlistStatusChip
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
class WaitlistChipUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `waiting chip renders Waiting text`() {
        composeTestRule.setContent {
            PeakConnectTheme {
                WaitlistStatusChip(status = WaitlistStatus.WAITING)
            }
        }

        composeTestRule.onNodeWithTag("waitlist_chip_waiting").assertIsDisplayed()
        composeTestRule.onNodeWithText("Waiting").assertIsDisplayed()
    }

    @Test
    fun `promoted chip renders Spot opened text`() {
        composeTestRule.setContent {
            PeakConnectTheme {
                WaitlistStatusChip(status = WaitlistStatus.PROMOTED)
            }
        }

        composeTestRule.onNodeWithTag("waitlist_chip_promoted").assertIsDisplayed()
        composeTestRule.onNodeWithText("Spot opened").assertIsDisplayed()
    }

    @Test
    fun `closed chip renders Closed text`() {
        composeTestRule.setContent {
            PeakConnectTheme {
                WaitlistStatusChip(status = WaitlistStatus.CLOSED)
            }
        }

        composeTestRule.onNodeWithTag("waitlist_chip_closed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Closed").assertIsDisplayed()
    }

    @Test
    fun `waitlist card renders all details and actions for waiting entry`() {
        var removedEntry: WaitlistEntryEntity? = null

        val entry = WaitlistEntryEntity(
            slotId = "slot-sample-1",
            activityTitle = "Kuari Pass Winter Trek",
            slotDate = LocalDateTime.of(2026, 12, 15, 8, 0),
            joinedAt = 1762000000000L,
            status = WaitlistStatus.WAITING,
            guideName = "Tenzing Norgay"
        )

        composeTestRule.setContent {
            PeakConnectTheme {
                WaitlistCard(
                    entry = entry,
                    onRemoveClick = { removedEntry = it },
                    onViewBookingClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Kuari Pass Winter Trek").assertIsDisplayed()
        composeTestRule.onNodeWithText("Waiting").assertIsDisplayed()
        composeTestRule.onNodeWithText("Guide: Tenzing Norgay").assertIsDisplayed()
        composeTestRule.onNodeWithText("Remove from list").assertIsDisplayed()

        composeTestRule.onNodeWithTag("waitlist_remove_btn").performClick()
        assertEquals(entry.slotId, removedEntry?.slotId)
    }

    @Test
    fun `waitlist card for promoted entry shows View booking button`() {
        var viewedBookingId: String? = null

        val entry = WaitlistEntryEntity(
            slotId = "slot-sample-2",
            activityTitle = "Hampta Pass Trek",
            slotDate = LocalDateTime.of(2026, 12, 20, 9, 30),
            joinedAt = 1762000000000L,
            status = WaitlistStatus.PROMOTED,
            bookingId = "booking-promo-xyz",
            guideName = "Dorje"
        )

        composeTestRule.setContent {
            PeakConnectTheme {
                WaitlistCard(
                    entry = entry,
                    onRemoveClick = {},
                    onViewBookingClick = { viewedBookingId = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Spot opened").assertIsDisplayed()
        composeTestRule.onNodeWithText("View booking").assertIsDisplayed()

        composeTestRule.onNodeWithTag("waitlist_view_booking_btn").performClick()
        assertEquals("booking-promo-xyz", viewedBookingId)
    }

    @Test
    fun `waitlist card for closed entry shows closure reason`() {
        val entry = WaitlistEntryEntity(
            slotId = "slot-sample-3",
            activityTitle = "Roopkund Trek",
            slotDate = LocalDateTime.of(2026, 10, 1, 7, 0),
            joinedAt = 1762000000000L,
            status = WaitlistStatus.CLOSED,
            closureReason = "No guide available"
        )

        composeTestRule.setContent {
            PeakConnectTheme {
                WaitlistCard(
                    entry = entry,
                    onRemoveClick = {},
                    onViewBookingClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Closed").assertIsDisplayed()
        composeTestRule.onNodeWithText("No guide available").assertIsDisplayed()
    }
}
