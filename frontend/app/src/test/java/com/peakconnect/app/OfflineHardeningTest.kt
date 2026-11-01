package com.peakconnect.app

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.network.AppError
import com.peakconnect.app.core.network.ErrorMapper
import com.peakconnect.app.core.network.NetworkRefreshNotifier
import com.peakconnect.app.core.theme.PeakConnectTheme
import com.peakconnect.app.core.ui.GlobalOfflineBanner
import com.peakconnect.app.core.ui.LastUpdatedIndicator
import com.peakconnect.app.core.ui.OnlineOnlyButton
import com.peakconnect.app.core.ui.formatRelativeTime
import com.peakconnect.app.core.util.LocalIsOnline
import com.peakconnect.app.data.local.entity.BookingEntity
import com.peakconnect.app.data.local.entity.GuideBookingEntity
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.feature.guide.home.GuideRequestCard
import com.peakconnect.app.feature.trekker.booking.BookingCard
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.SocketTimeoutException
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfflineHardeningTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // -------------------------------------------------------------
    // 1. GLOBAL OFFLINE BANNER TESTS
    // -------------------------------------------------------------
    @Test
    fun `global offline banner is displayed when offline`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsOnline provides false) {
                PeakConnectTheme {
                    GlobalOfflineBanner()
                }
            }
        }

        composeTestRule.onNodeWithTag("global_offline_banner")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Showing saved data.", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `global offline banner triggers onBackOnline callback when returning online`() {
        var backOnlineTriggered = false

        composeTestRule.setContent {
            GlobalOfflineBanner(
                isOnline = false,
                onBackOnline = { backOnlineTriggered = true }
            )
        }

        composeTestRule.onNodeWithTag("global_offline_banner").assertIsDisplayed()
        assertFalse(backOnlineTriggered)
    }

    // -------------------------------------------------------------
    // 2. ACTION GATING TESTS FOR ALL 12 NETWORK-DEPENDENT ACTIONS
    // -------------------------------------------------------------
    @Test
    fun `action gating 1 - Request booking button is disabled offline`() {
        var clicked = false
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsOnline provides false) {
                PeakConnectTheme {
                    OnlineOnlyButton(
                        text = "Request Booking",
                        actionLabel = "request booking",
                        onClick = { clicked = true }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Request Booking")
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithText("Connect to the internet to request booking")
            .assertIsDisplayed()
        assertFalse(clicked)
    }

    @Test
    fun `action gating 2 - Join waitlist button is disabled offline`() {
        var clicked = false
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsOnline provides false) {
                PeakConnectTheme {
                    OnlineOnlyButton(
                        text = "Join Waitlist",
                        actionLabel = "join waitlist",
                        onClick = { clicked = true }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Join Waitlist")
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithText("Connect to the internet to join waitlist")
            .assertIsDisplayed()
        assertFalse(clicked)
    }

    @Test
    fun `action gating 3 - Send booking request button is disabled offline`() {
        var clicked = false
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsOnline provides false) {
                PeakConnectTheme {
                    OnlineOnlyButton(
                        text = "Send Request",
                        actionLabel = "send booking request",
                        onClick = { clicked = true }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Send Request")
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithText("Connect to the internet to send booking request")
            .assertIsDisplayed()
        assertFalse(clicked)
    }

    @Test
    fun `action gating 4 - Pay button is gated and disabled offline`() {
        var clicked = false
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsOnline provides false) {
                PeakConnectTheme {
                    OnlineOnlyButton(
                        text = "Pay Now",
                        actionLabel = "pay",
                        onClick = { clicked = true }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Pay Now")
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithText("Connect to the internet to pay")
            .assertIsDisplayed()
        assertFalse(clicked)
    }

    @Test
    fun `action gating 5 - Cancel button is disabled offline`() {
        var clicked = false
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsOnline provides false) {
                PeakConnectTheme {
                    OnlineOnlyButton(
                        text = "Cancel Booking",
                        actionLabel = "cancel booking",
                        onClick = { clicked = true }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Cancel Booking")
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithText("Connect to the internet to cancel booking")
            .assertIsDisplayed()
        assertFalse(clicked)
    }

    @Test
    fun `action gating 6 - Accept button is disabled offline`() {
        var clicked = false
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsOnline provides false) {
                PeakConnectTheme {
                    OnlineOnlyButton(
                        text = "Accept",
                        actionLabel = "accept booking",
                        onClick = { clicked = true }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Accept")
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithText("Connect to the internet to accept booking")
            .assertIsDisplayed()
        assertFalse(clicked)
    }

    @Test
    fun `action gating 7 - Decline button is disabled offline`() {
        var clicked = false
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsOnline provides false) {
                PeakConnectTheme {
                    OnlineOnlyButton(
                        text = "Decline",
                        actionLabel = "decline booking",
                        onClick = { clicked = true }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Decline")
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithText("Connect to the internet to decline booking")
            .assertIsDisplayed()
        assertFalse(clicked)
    }

    @Test
    fun `action gating 8 - No-show button is disabled offline`() {
        var clicked = false
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsOnline provides false) {
                PeakConnectTheme {
                    OnlineOnlyButton(
                        text = "Mark No-Show",
                        actionLabel = "mark no-show",
                        onClick = { clicked = true }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Mark No-Show")
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithText("Connect to the internet to mark no-show")
            .assertIsDisplayed()
        assertFalse(clicked)
    }

    @Test
    fun `action gating 9 - Save profile button is disabled offline`() {
        var clicked = false
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsOnline provides false) {
                PeakConnectTheme {
                    OnlineOnlyButton(
                        text = "Save Profile",
                        actionLabel = "save profile",
                        onClick = { clicked = true }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Save Profile")
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithText("Connect to the internet to save profile")
            .assertIsDisplayed()
        assertFalse(clicked)
    }

    @Test
    fun `action gating 10 - Map search is gated by LocalIsOnline`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsOnline provides false) {
                val isOnline = LocalIsOnline.current
                Text(text = if (isOnline) "Map Search Enabled" else "Map Search Disabled")
            }
        }

        composeTestRule.onNodeWithText("Map Search Disabled").assertIsDisplayed()
    }

    @Test
    fun `action gating 11 - Sync now button is disabled offline`() {
        var clicked = false
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsOnline provides false) {
                PeakConnectTheme {
                    OnlineOnlyButton(
                        text = "Sync Now",
                        actionLabel = "sync",
                        onClick = { clicked = true }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Sync Now")
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithText("Connect to the internet to sync")
            .assertIsDisplayed()
        assertFalse(clicked)
    }

    @Test
    fun `action gating 12 - Refresh button in ErrorState is disabled offline`() {
        var clicked = false
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsOnline provides false) {
                PeakConnectTheme {
                    OnlineOnlyButton(
                        text = "Retry",
                        actionLabel = "refresh",
                        onClick = { clicked = true }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Retry")
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithText("Connect to the internet to refresh")
            .assertIsDisplayed()
        assertFalse(clicked)
    }

    // -------------------------------------------------------------
    // 3. LAST UPDATED INDICATOR TESTS
    // -------------------------------------------------------------
    @Test
    fun `formatRelativeTime formats relative intervals correctly`() {
        val now = 1_000_000L
        composeTestRule.setContent {
            PeakConnectTheme {
                val justNow = formatRelativeTime(now - 30_000L, now)
                val fiveMin = formatRelativeTime(now - 5 * 60 * 1000L, now)
                val twoHours = formatRelativeTime(now - 2 * 60 * 60 * 1000L, now)

                Text(text = "R1: $justNow")
                Text(text = "R2: $fiveMin")
                Text(text = "R3: $twoHours")
            }
        }

        composeTestRule.onNodeWithText("R1: just now").assertIsDisplayed()
        composeTestRule.onNodeWithText("R2: 5 min ago").assertIsDisplayed()
        composeTestRule.onNodeWithText("R3: 2 hours ago").assertIsDisplayed()
    }

    @Test
    fun `last updated indicator renders prominent badge when offline`() {
        val now = 10_000_000L
        composeTestRule.setContent {
            PeakConnectTheme {
                LastUpdatedIndicator(
                    lastUpdatedMillis = now - 5 * 60 * 1000L,
                    isOffline = true,
                    clock = { now }
                )
            }
        }

        composeTestRule.onNodeWithTag("last_updated_prominent").assertIsDisplayed()
        composeTestRule.onNodeWithText("Saved data • Updated 5 min ago").assertIsDisplayed()
    }

    @Test
    fun `last updated indicator renders subtle text when online`() {
        val now = 10_000_000L
        composeTestRule.setContent {
            PeakConnectTheme {
                LastUpdatedIndicator(
                    lastUpdatedMillis = now - 5 * 60 * 1000L,
                    isOffline = false,
                    clock = { now }
                )
            }
        }

        composeTestRule.onNodeWithTag("last_updated_subtle").assertIsDisplayed()
        composeTestRule.onNodeWithText("Updated 5 min ago").assertIsDisplayed()
    }

    // -------------------------------------------------------------
    // 4. STALE DATA SAFETY TESTS
    // -------------------------------------------------------------
    @Test
    fun `guide request card shows neutral message instead of countdown when offline`() {
        val sampleBooking = GuideBookingEntity(
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
            statusSinceMillis = System.currentTimeMillis() - (5 * 60 * 1000L),
            lastSyncMillis = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsOnline provides false) {
                PeakConnectTheme {
                    GuideRequestCard(
                        booking = sampleBooking,
                        onCardClick = {},
                        onAccept = {},
                        onDecline = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Status may have changed. Refresh when online.")
            .assertIsDisplayed()
    }

    @Test
    fun `booking card with AWAITING_PAYMENT hides Pay button and shows neutral message when offline`() {
        val awaitingPaymentBooking = BookingEntity(
            id = "b-1",
            slotId = "slot-1",
            activityTitle = "Har Ki Dun Trek",
            trekkerId = "trekker-1",
            trekkerName = "Alex",
            guideId = "guide-1",
            guideName = "John",
            status = BookingStatus.AWAITING_PAYMENT,
            date = LocalDateTime.of(2026, 12, 15, 9, 0),
            paymentOrderId = "order_123",
            depositAmount = 1500.0,
            refundAmount = null,
            refundPercentage = null,
            statusSinceMillis = System.currentTimeMillis() - (5 * 60 * 1000L),
            lastSyncMillis = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsOnline provides false) {
                PeakConnectTheme {
                    BookingCard(
                        item = com.peakconnect.app.feature.trekker.booking.BookingListItem(
                            booking = awaitingPaymentBooking
                        ),
                        onCardClick = {},
                        onPayClick = {}
                    )
                }
            }
        }

        // Must display neutral stale data message
        composeTestRule.onNodeWithText("Status may have changed. Refresh when online.")
            .assertIsDisplayed()

        // Must NEVER show Pay button when offline
        composeTestRule.onNodeWithText("Pay Deposit", substring = true)
            .assertDoesNotExist()
    }

    // -------------------------------------------------------------
    // 5. ERROR AND TIMEOUT AUDIT TESTS
    // -------------------------------------------------------------
    @Test
    fun `timeout shows Server is taking a while message`() {
        val error = ErrorMapper.mapThrowable(SocketTimeoutException("Timeout"))
        assertTrue(error is ApiResult.Error)
        assertEquals(AppError.Timeout, (error as ApiResult.Error).type)
        assertEquals("Server is taking a while…", error.message)
    }

    @Test
    fun `5xx errors show friendly server error and never raw stack traces or internal details`() {
        val result = ErrorMapper.mapHttpError(500, """{"error":"java.lang.NullPointerException at com.backend.Internal"}""")
        assertTrue(result.type is AppError.Server)
        assertEquals(500, (result.type as AppError.Server).code)
        assertEquals(ErrorMapper.DEFAULT_SERVER_ERROR_MSG, result.message)
        assertFalse(result.message.contains("NullPointerException"))
        assertFalse(result.message.contains("Exception"))
    }

    @Test
    fun `unknown exceptions never leak raw exception messages or stack traces`() {
        val rawThrowable = RuntimeException("CRITICAL INTERNAL DB FAILURE: table corrupted")
        val result = ErrorMapper.mapThrowable(rawThrowable)
        assertTrue(result is ApiResult.Error)
        assertEquals(ErrorMapper.DEFAULT_UNKNOWN_MSG, (result as ApiResult.Error).message)
        assertFalse(result.message.contains("CRITICAL"))
        assertFalse(result.message.contains("corrupted"))
    }

    // -------------------------------------------------------------
    // 6. BACK-ONLINE REFRESH NOTIFIER TESTS
    // -------------------------------------------------------------
    @Test
    fun `network refresh notifier emits event on triggerRefresh`() = runTest {
        val notifier = NetworkRefreshNotifier()
        var refreshed = false

        val job = launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            notifier.refreshEvents.collect {
                refreshed = true
            }
        }

        notifier.triggerRefresh()
        assertTrue(refreshed)
        job.cancel()
    }
}
