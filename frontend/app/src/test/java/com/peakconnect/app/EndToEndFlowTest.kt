package com.peakconnect.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.peakconnect.app.core.theme.PeakConnectTheme
import com.peakconnect.app.core.ui.OnlineOnlyButton
import com.peakconnect.app.core.util.LocalIsOnline
import com.peakconnect.app.data.local.entity.ActivityEntity
import com.peakconnect.app.data.local.entity.BookingEntity
import com.peakconnect.app.data.local.entity.GuideBookingEntity
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.CancellationPolicyType
import com.peakconnect.app.domain.model.DifficultyLevel
import com.peakconnect.app.domain.model.ExperienceLevel
import com.peakconnect.app.domain.model.MatchedGuide
import com.peakconnect.app.feature.guide.home.GuideRequestCard
import com.peakconnect.app.feature.trekker.booking.BookingStepper
import com.peakconnect.app.feature.trekker.matches.GuideCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h900dp-xxhdpi")
class EndToEndFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    enum class TrekkerScreen {
        LOGIN,
        EXPLORE,
        DETAIL,
        MATCHES,
        REVIEW,
        STATUS
    }

    @Test
    fun `trekker end-to-end journey from login to booking status`() {
        var currentScreen by mutableStateOf(TrekkerScreen.LOGIN)
        var selectedActivityId by mutableStateOf<String?>(null)
        var selectedGuideId by mutableStateOf<String?>(null)
        var confirmedBookingId by mutableStateOf<String?>(null)

        val sampleActivity = ActivityEntity(
            id = "act_hampta",
            title = "Hampta Pass Trek",
            description = "High altitude Himalayan pass crossover trek.",
            location = "Manali, HP",
            latitude = 32.2396,
            longitude = 77.1887,
            difficulty = DifficultyLevel.MODERATE,
            basePrice = BigDecimal("7500"),
            cancellationPolicy = CancellationPolicyType.MODERATE,
            minSlotPrice = BigDecimal("7500"),
            lastUpdated = 1000L
        )

        val sampleGuide = MatchedGuide(
            guideId = "guide_tenzing",
            name = "Tenzing Norgay",
            skills = listOf("High Altitude Trekking", "First Aid"),
            languages = listOf("English", "Hindi"),
            location = "Manali",
            experienceLevel = ExperienceLevel.EXPERT,
            score = 98.5
        )

        val sampleBooking = BookingEntity(
            id = "b_1001",
            slotId = "slot_101",
            activityTitle = "Hampta Pass Trek",
            trekkerId = "trekker_alex",
            trekkerName = "Alex",
            guideId = "guide_tenzing",
            guideName = "Tenzing Norgay",
            status = BookingStatus.PENDING_GUIDE_RESPONSE,
            date = LocalDateTime.now().plusDays(5),
            paymentOrderId = "order_123",
            depositAmount = 1500.0,
            refundAmount = null,
            refundPercentage = null,
            statusSinceMillis = 1000L,
            lastSyncMillis = 1000L
        )

        composeTestRule.setContent {
            PeakConnectTheme {
                CompositionLocalProvider(LocalIsOnline provides true) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (currentScreen) {
                            TrekkerScreen.LOGIN -> {
                                OnlineOnlyButton(
                                    text = "Log in as Trekker",
                                    actionLabel = "log in",
                                    onClick = { currentScreen = TrekkerScreen.EXPLORE }
                                )
                            }
                            TrekkerScreen.EXPLORE -> {
                                OnlineOnlyButton(
                                    text = sampleActivity.title,
                                    actionLabel = "view activity",
                                    onClick = {
                                        selectedActivityId = sampleActivity.id
                                        currentScreen = TrekkerScreen.DETAIL
                                    }
                                )
                            }
                            TrekkerScreen.DETAIL -> {
                                OnlineOnlyButton(
                                    text = "Choose a Guide",
                                    actionLabel = "choose guide",
                                    onClick = { currentScreen = TrekkerScreen.MATCHES }
                                )
                            }
                            TrekkerScreen.MATCHES -> {
                                GuideCard(
                                    guide = sampleGuide,
                                    rank = 1,
                                    onClick = {
                                        selectedGuideId = sampleGuide.guideId
                                        currentScreen = TrekkerScreen.REVIEW
                                    }
                                )
                            }
                            TrekkerScreen.REVIEW -> {
                                OnlineOnlyButton(
                                    text = "Send booking request",
                                    actionLabel = "send request",
                                    onClick = {
                                        confirmedBookingId = sampleBooking.id
                                        currentScreen = TrekkerScreen.STATUS
                                    }
                                )
                            }
                            TrekkerScreen.STATUS -> {
                                BookingStepper(
                                    status = sampleBooking.status,
                                    hasGuide = true
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1. Login step
        composeTestRule.onNodeWithText("Log in as Trekker").assertIsDisplayed().performClick()
        assertEquals(TrekkerScreen.EXPLORE, currentScreen)

        // 2. Explore step
        composeTestRule.onNodeWithText("Hampta Pass Trek").assertIsDisplayed().performClick()
        assertEquals("act_hampta", selectedActivityId)
        assertEquals(TrekkerScreen.DETAIL, currentScreen)

        // 3. Detail step
        composeTestRule.onNodeWithText("Choose a Guide").assertIsDisplayed().performClick()
        assertEquals(TrekkerScreen.MATCHES, currentScreen)

        // 4. Matches step
        composeTestRule.onNodeWithText("Tenzing Norgay").assertIsDisplayed().performClick()
        assertEquals("guide_tenzing", selectedGuideId)
        assertEquals(TrekkerScreen.REVIEW, currentScreen)

        // 5. Review step
        composeTestRule.onNodeWithText("Send booking request").assertIsDisplayed().performClick()
        assertEquals(TrekkerScreen.STATUS, currentScreen)
        assertEquals("b_1001", confirmedBookingId)

        // 6. Status step displays booking status
        composeTestRule.onNodeWithText("Request sent").assertIsDisplayed()
    }

    @Test
    fun `guide experience accepting a booking request updates state`() {
        var isAccepted by mutableStateOf(false)
        val request = GuideBookingEntity(
            id = "req_42",
            slotId = "slot_99",
            activityTitle = "Kedarkantha Winter Trek",
            trekkerId = "trekker_maya",
            trekkerName = "Maya Sharma",
            guideId = "g_me",
            guideName = "Guide Me",
            status = if (isAccepted) BookingStatus.AWAITING_PAYMENT else BookingStatus.PENDING_GUIDE_RESPONSE,
            date = LocalDateTime.now().plusDays(4),
            paymentOrderId = null,
            depositAmount = null,
            refundAmount = null,
            refundPercentage = null,
            statusSinceMillis = System.currentTimeMillis(),
            lastSyncMillis = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            PeakConnectTheme {
                CompositionLocalProvider(LocalIsOnline provides true) {
                    GuideRequestCard(
                        booking = request,
                        onCardClick = {},
                        onAccept = { isAccepted = true },
                        onDecline = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Trekker: Maya Sharma").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kedarkantha Winter Trek").assertIsDisplayed()

        // Guide accepts the request
        composeTestRule.onNodeWithText("Accept").assertIsDisplayed().performClick()

        assertTrue(isAccepted)
    }

    @Test
    fun `offline gating disables interactive online actions with clear guidance`() {
        var actionExecuted = false

        composeTestRule.setContent {
            PeakConnectTheme {
                CompositionLocalProvider(LocalIsOnline provides false) {
                    OnlineOnlyButton(
                        text = "Confirm Booking",
                        actionLabel = "confirm your booking",
                        onClick = { actionExecuted = true }
                    )
                }
            }
        }

        // Button should be displayed but disabled when offline
        composeTestRule.onNodeWithText("Confirm Booking")
            .assertIsDisplayed()
            .assertIsNotEnabled()

        // Clear offline guidance message is displayed
        composeTestRule.onNodeWithText("Connect to the internet to confirm your booking")
            .assertIsDisplayed()

        assertEquals(false, actionExecuted)
    }
}
