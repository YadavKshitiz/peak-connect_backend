package com.peakconnect.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.peakconnect.app.core.theme.PeakConnectTheme
import com.peakconnect.app.data.local.entity.ActivityEntity
import com.peakconnect.app.data.local.entity.BookingEntity
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.CancellationPolicyType
import com.peakconnect.app.domain.model.DifficultyLevel
import com.peakconnect.app.feature.trekker.booking.BookingStepper
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
class BookingStatusUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createBooking(
        status: BookingStatus,
        guideId: String? = "guide-1",
        guideName: String? = "Tenzing",
        depositAmount: Double? = 500.0,
        refundAmount: Double? = null,
        refundPercentage: Double? = null,
        statusSinceMillis: Long = System.currentTimeMillis()
    ): BookingEntity {
        return BookingEntity(
            id = "b-test-1",
            slotId = "s-test-1",
            activityTitle = "Hampta Pass Trek",
            trekkerId = "t-1",
            trekkerName = "Alex",
            guideId = guideId,
            guideName = guideName,
            status = status,
            date = LocalDateTime.now().plusDays(3),
            paymentOrderId = "order_999",
            depositAmount = depositAmount,
            refundAmount = refundAmount,
            refundPercentage = refundPercentage,
            statusSinceMillis = statusSinceMillis,
            lastSyncMillis = System.currentTimeMillis()
        )
    }

    private val sampleActivity = ActivityEntity(
        id = "act-1",
        title = "Hampta Pass Trek",
        description = "Beautiful pass",
        location = "Manali",
        latitude = 32.24,
        longitude = 77.18,
        difficulty = DifficultyLevel.MODERATE,
        basePrice = BigDecimal("2500.00"),
        cancellationPolicy = CancellationPolicyType.FLEXIBLE,
        minSlotPrice = BigDecimal("2500.00"),
        lastUpdated = 1000L
    )

    @Test
    fun `stepper with guide shows guide accepted step`() {
        composeTestRule.setContent {
            PeakConnectTheme {
                BookingStepper(
                    status = BookingStatus.PENDING_GUIDE_RESPONSE,
                    hasGuide = true
                )
            }
        }

        composeTestRule.onNodeWithText("Request sent").assertIsDisplayed()
        composeTestRule.onNodeWithText("Guide accepted").assertIsDisplayed()
        composeTestRule.onNodeWithText("Deposit paid").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirmed").assertIsDisplayed()
    }

    @Test
    fun `stepper without guide hides guide accepted step`() {
        composeTestRule.setContent {
            PeakConnectTheme {
                BookingStepper(
                    status = BookingStatus.PENDING_GUIDE_RESPONSE,
                    hasGuide = false
                )
            }
        }

        composeTestRule.onNodeWithText("Request sent").assertIsDisplayed()
        composeTestRule.onNodeWithText("Guide accepted").assertDoesNotExist()
        composeTestRule.onNodeWithText("Deposit paid").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirmed").assertIsDisplayed()
    }

    @Test
    fun `stepper on terminal status shows terminal step`() {
        composeTestRule.setContent {
            PeakConnectTheme {
                BookingStepper(
                    status = BookingStatus.CANCELLED,
                    hasGuide = true
                )
            }
        }

        composeTestRule.onNodeWithText("Request sent").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancelled").assertIsDisplayed()
    }
}
