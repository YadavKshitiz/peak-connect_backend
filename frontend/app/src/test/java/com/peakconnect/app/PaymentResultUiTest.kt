package com.peakconnect.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.peakconnect.app.core.theme.PeakConnectTheme
import com.peakconnect.app.domain.model.Booking
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.feature.payment.DelayedStateContent
import com.peakconnect.app.feature.payment.FailedStateContent
import com.peakconnect.app.feature.payment.PaymentResultScreen
import com.peakconnect.app.feature.payment.PaymentUiState
import com.peakconnect.app.feature.payment.ProcessingStateContent
import com.peakconnect.app.feature.payment.SuccessStateContent
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PaymentResultUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleBooking(
        status: BookingStatus = BookingStatus.CONFIRMED,
        guideName: String? = "Tenzing Norgay"
    ) = Booking(
        id = "booking-456",
        slotId = "slot-1",
        activityTitle = "Kuari Pass Trek",
        trekkerId = "user-1",
        trekkerName = "Trekker One",
        guideId = "guide-1",
        guideName = guideName,
        status = status,
        date = LocalDateTime.of(2026, 12, 10, 8, 30),
        paymentOrderId = "order_123",
        depositAmount = 1800.0,
        refundAmount = null,
        refundPercentage = null,
        statusSinceMillis = 1000L,
        lastSyncMillis = 1000L
    )

    @Test
    fun `processing state displays guidance and spinner`() {
        val booking = sampleBooking(status = BookingStatus.AWAITING_PAYMENT)

        composeTestRule.setContent {
            PeakConnectTheme {
                ProcessingStateContent(booking = booking)
            }
        }

        composeTestRule.onNodeWithText("Confirming your payment…").assertIsDisplayed()
        composeTestRule.onNodeWithText("Please don't close the app").assertIsDisplayed()
        composeTestRule.onNodeWithTag("payment_result_processing").assertIsDisplayed()
    }

    @Test
    fun `success state displays confirmation summary and handles button clicks`() {
        val booking = sampleBooking(status = BookingStatus.CONFIRMED)
        var viewBookingClicked = false
        var exploreClicked = false

        composeTestRule.setContent {
            PeakConnectTheme {
                SuccessStateContent(
                    booking = booking,
                    onViewBooking = { viewBookingClicked = true },
                    onExplore = { exploreClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Booking confirmed!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kuari Pass Trek").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tenzing Norgay").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹1,800").assertIsDisplayed()

        // Check Buttons
        composeTestRule.onNodeWithText("View booking").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Back to explore").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("payment_button_view_booking").performScrollTo().performClick()
        assertTrue(viewBookingClicked)

        composeTestRule.onNodeWithTag("payment_button_back_explore").performScrollTo().performClick()
        assertTrue(exploreClicked)
    }

    @Test
    fun `success state with unassigned guide shows guide fallback`() {
        val booking = sampleBooking(status = BookingStatus.CONFIRMED, guideName = null)

        composeTestRule.setContent {
            PeakConnectTheme {
                SuccessStateContent(
                    booking = booking,
                    onViewBooking = {},
                    onExplore = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Guide will be assigned").assertIsDisplayed()
    }

    @Test
    fun `failed state with retry displays error and retry button`() {
        var tryAgainClicked = false
        var backToBookingClicked = false

        composeTestRule.setContent {
            PeakConnectTheme {
                FailedStateContent(
                    reason = "Card transaction declined by issuing bank",
                    canRetry = true,
                    onTryAgain = { tryAgainClicked = true },
                    onBackToBooking = { backToBookingClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Payment didn't go through").assertIsDisplayed()
        composeTestRule.onNodeWithText("Card transaction declined by issuing bank").assertIsDisplayed()
        composeTestRule.onNodeWithText("Try again").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back to booking").assertIsDisplayed()

        composeTestRule.onNodeWithTag("payment_button_try_again").performClick()
        assertTrue(tryAgainClicked)

        composeTestRule.onNodeWithTag("payment_button_back_booking").performClick()
        assertTrue(backToBookingClicked)
    }

    @Test
    fun `failed state without retry hides try again button`() {
        composeTestRule.setContent {
            PeakConnectTheme {
                FailedStateContent(
                    reason = "Booking was cancelled by the server",
                    canRetry = false,
                    onTryAgain = {},
                    onBackToBooking = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Payment didn't go through").assertIsDisplayed()
        composeTestRule.onNodeWithText("Booking was cancelled by the server").assertIsDisplayed()
        composeTestRule.onNodeWithTag("payment_button_try_again").assertDoesNotExist()
        composeTestRule.onNodeWithText("Back to booking").assertIsDisplayed()
    }

    @Test
    fun `delayed state displays explanation and actions`() {
        var checkAgainClicked = false
        var myBookingsClicked = false

        composeTestRule.setContent {
            PeakConnectTheme {
                DelayedStateContent(
                    onCheckAgain = { checkAgainClicked = true },
                    onMyBookings = { myBookingsClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Confirmation taking longer").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "We've received your payment details but confirmation is taking longer than usual. This can take a few minutes."
        ).assertIsDisplayed()

        composeTestRule.onNodeWithText("Check again").assertIsDisplayed()
        composeTestRule.onNodeWithText("Go to my bookings").assertIsDisplayed()

        composeTestRule.onNodeWithTag("payment_button_check_again").performClick()
        assertTrue(checkAgainClicked)

        composeTestRule.onNodeWithTag("payment_button_my_bookings").performClick()
        assertTrue(myBookingsClicked)
    }

    @Test
    fun `payment result screen routes to correct state composable`() {
        val booking = sampleBooking(status = BookingStatus.CONFIRMED)

        composeTestRule.setContent {
            PeakConnectTheme {
                PaymentResultScreen(
                    state = PaymentUiState.Success(booking),
                    onTryAgain = {},
                    onCheckAgain = {},
                    onViewBooking = {},
                    onBackToBooking = {},
                    onExplore = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Booking confirmed!").assertIsDisplayed()
    }
}
