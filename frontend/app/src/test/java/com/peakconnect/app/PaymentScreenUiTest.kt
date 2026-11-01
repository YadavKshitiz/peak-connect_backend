package com.peakconnect.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.peakconnect.app.core.theme.PeakConnectTheme
import com.peakconnect.app.domain.model.Booking
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.feature.payment.PaymentScreen
import com.peakconnect.app.feature.payment.PaymentUiState
import com.peakconnect.app.feature.payment.PaymentViewModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PaymentScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleBooking(
        status: BookingStatus = BookingStatus.AWAITING_PAYMENT,
        guideName: String? = "Tenzing Norgay",
        orderId: String? = "order_rzp_99"
    ) = Booking(
        id = "booking-789",
        slotId = "slot-1",
        activityTitle = "Kedarkantha Trek",
        trekkerId = "user-1",
        trekkerName = "Trekker One",
        guideId = "guide-1",
        guideName = guideName,
        status = status,
        date = LocalDateTime.of(2026, 12, 15, 9, 0),
        paymentOrderId = orderId,
        depositAmount = 1200.0,
        refundAmount = null,
        refundPercentage = null,
        statusSinceMillis = 1000L,
        lastSyncMillis = 1000L
    )

    @Test
    fun `payment summary shows details test banner and active pay button`() {
        val booking = sampleBooking()
        val viewModel: PaymentViewModel = io.mockk.mockk(relaxed = true)
        io.mockk.every { viewModel.uiState } returns kotlinx.coroutines.flow.MutableStateFlow(
            PaymentUiState.Summary(
                booking = booking,
                isPayEnabled = true,
                isCheckoutInProgress = false,
                isTestMode = true
            )
        )
        io.mockk.every { viewModel.checkoutRequests } returns kotlinx.coroutines.flow.MutableSharedFlow()

        composeTestRule.setContent {
            PeakConnectTheme {
                PaymentScreen(
                    onBack = {},
                    onViewBooking = {},
                    onExplore = {},
                    viewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText("Payment").assertIsDisplayed()
        composeTestRule.onNodeWithTag("payment_test_banner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kedarkantha Trek").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tenzing Norgay").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹1,200").assertIsDisplayed()
        composeTestRule.onNodeWithText("This deposit is non-refundable").assertIsDisplayed()

        val payButton = composeTestRule.onNodeWithTag("payment_pay_button")
        payButton.assertIsDisplayed()
        payButton.assertIsEnabled()
    }

    @Test
    fun `payment summary disables pay button when not enabled`() {
        val booking = sampleBooking(orderId = "")
        val viewModel: PaymentViewModel = io.mockk.mockk(relaxed = true)
        io.mockk.every { viewModel.uiState } returns kotlinx.coroutines.flow.MutableStateFlow(
            PaymentUiState.Summary(
                booking = booking,
                isPayEnabled = false,
                isCheckoutInProgress = false,
                isTestMode = false
            )
        )
        io.mockk.every { viewModel.checkoutRequests } returns kotlinx.coroutines.flow.MutableSharedFlow()

        composeTestRule.setContent {
            PeakConnectTheme {
                PaymentScreen(
                    onBack = {},
                    onViewBooking = {},
                    onExplore = {},
                    viewModel = viewModel
                )
            }
        }

        val payButton = composeTestRule.onNodeWithText("Pay ₹1,200")
        payButton.assertIsDisplayed()
        payButton.assertIsNotEnabled()
    }

    @Test
    fun `invalid status displays explanation and back button`() {
        val booking = sampleBooking(status = BookingStatus.CANCELLED)
        var backClicked = false
        val viewModel: PaymentViewModel = io.mockk.mockk(relaxed = true)
        io.mockk.every { viewModel.uiState } returns kotlinx.coroutines.flow.MutableStateFlow(
            PaymentUiState.InvalidStatus(
                booking = booking,
                reason = "Booking is cancelled"
            )
        )
        io.mockk.every { viewModel.checkoutRequests } returns kotlinx.coroutines.flow.MutableSharedFlow()

        composeTestRule.setContent {
            PeakConnectTheme {
                PaymentScreen(
                    onBack = { backClicked = true },
                    onViewBooking = {},
                    onExplore = {},
                    viewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText("This booking is no longer awaiting payment (CANCELLED).").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back to booking").assertIsDisplayed().performClick()
        assertTrue(backClicked)
    }
}
