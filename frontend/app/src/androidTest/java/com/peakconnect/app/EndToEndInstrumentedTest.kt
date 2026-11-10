package com.peakconnect.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class EndToEndInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testTrekkerEndToEndFlow() {
        var screen by mutableStateOf("LOGIN")
        val sampleActivity = ActivityEntity(
            id = "act_hampta",
            title = "Hampta Pass Trek",
            description = "High altitude pass crossover",
            location = "Manali",
            latitude = 32.2,
            longitude = 77.1,
            difficulty = DifficultyLevel.MODERATE,
            basePrice = BigDecimal("7500"),
            cancellationPolicy = CancellationPolicyType.MODERATE,
            minSlotPrice = BigDecimal("7500"),
            lastUpdated = 1000L
        )
        val sampleGuide = MatchedGuide(
            guideId = "g_tenzing",
            name = "Tenzing Norgay",
            skills = listOf("Climbing"),
            languages = listOf("English", "Hindi"),
            location = "Manali",
            experienceLevel = ExperienceLevel.EXPERT,
            score = 99.0
        )
        val sampleBooking = BookingEntity(
            id = "b_100",
            slotId = "s_100",
            activityTitle = "Hampta Pass Trek",
            trekkerId = "t_1",
            trekkerName = "Alex",
            guideId = "g_tenzing",
            guideName = "Tenzing Norgay",
            status = BookingStatus.PENDING_GUIDE_RESPONSE,
            date = LocalDateTime.now().plusDays(2),
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
                        when (screen) {
                            "LOGIN" -> OnlineOnlyButton("Log in as Trekker", "log in", onClick = { screen = "EXPLORE" })
                            "EXPLORE" -> OnlineOnlyButton("Hampta Pass Trek", "explore", onClick = { screen = "DETAIL" })
                            "DETAIL" -> OnlineOnlyButton("Choose a Guide", "detail", onClick = { screen = "MATCHES" })
                            "MATCHES" -> GuideCard(sampleGuide, rank = 1, onClick = { screen = "REVIEW" })
                            "REVIEW" -> OnlineOnlyButton("Send booking request", "review", onClick = { screen = "STATUS" })
                            "STATUS" -> BookingStepper(sampleBooking.status, hasGuide = true)
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Log in as Trekker").performClick()
        composeTestRule.onNodeWithText("Hampta Pass Trek").performClick()
        composeTestRule.onNodeWithText("Choose a Guide").performClick()
        composeTestRule.onNodeWithText("Tenzing Norgay").performClick()
        composeTestRule.onNodeWithText("Send booking request").performClick()
        composeTestRule.onNodeWithText("Request sent").assertIsDisplayed()
    }

    @Test
    fun testGuideAcceptRequest() {
        var accepted = false
        val booking = GuideBookingEntity(
            id = "b_req",
            slotId = "s_req",
            activityTitle = "Kedarkantha Trek",
            trekkerId = "t_2",
            trekkerName = "Rahul",
            guideId = "g_me",
            guideName = "Guide Me",
            status = BookingStatus.PENDING_GUIDE_RESPONSE,
            date = LocalDateTime.now().plusDays(3),
            paymentOrderId = null,
            depositAmount = null,
            refundAmount = null,
            refundPercentage = null,
            statusSinceMillis = 1000L,
            lastSyncMillis = 1000L
        )

        composeTestRule.setContent {
            PeakConnectTheme {
                CompositionLocalProvider(LocalIsOnline provides true) {
                    GuideRequestCard(
                        booking = booking,
                        onCardClick = {},
                        onAccept = { accepted = true },
                        onDecline = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Trekker: Rahul").assertIsDisplayed()
        composeTestRule.onNodeWithText("Accept").performClick()
        assertTrue(accepted)
    }

    @Test
    fun testOfflineGating() {
        composeTestRule.setContent {
            PeakConnectTheme {
                CompositionLocalProvider(LocalIsOnline provides false) {
                    OnlineOnlyButton(
                        text = "Book Slot",
                        actionLabel = "book this slot",
                        onClick = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Book Slot").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Connect to the internet to book this slot").assertIsDisplayed()
    }
}
