package com.peakconnect.app

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.network.WakeUpRetryHelper
import com.peakconnect.app.core.session.SessionManager
import com.peakconnect.app.core.session.SessionState
import com.peakconnect.app.core.theme.PeakConnectTheme
import com.peakconnect.app.data.api.PeakConnectApi
import com.peakconnect.app.data.local.PeakConnectDatabase
import com.peakconnect.app.data.local.entity.ActivityEntity
import com.peakconnect.app.data.local.entity.SlotEntity
import com.peakconnect.app.data.repository.ActivityRepositoryImpl
import com.peakconnect.app.domain.model.CancellationPolicyType
import com.peakconnect.app.domain.model.DifficultyLevel
import com.peakconnect.app.domain.model.RiskLevel
import com.peakconnect.app.domain.model.Role
import com.peakconnect.app.domain.model.Season
import com.peakconnect.app.feature.explore.ExploreScreen
import com.peakconnect.app.feature.explore.ExploreViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h900dp-xxhdpi")
class ExploreScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var db: PeakConnectDatabase
    private val api: PeakConnectApi = mockk(relaxed = true)
    private val wakeUpRetryHelper: WakeUpRetryHelper = mockk(relaxed = true)
    private val sessionManager: SessionManager = mockk(relaxed = true)

    private val sessionStateFlow = MutableStateFlow<SessionState>(
        SessionState.LoggedIn(Role.TREKKER, "1", "alex@example.com", "Alex Hunter")
    )

    private lateinit var repository: ActivityRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PeakConnectDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        every { sessionManager.sessionState } returns sessionStateFlow

        repository = ActivityRepositoryImpl(
            api = api,
            activityDao = db.activityDao(),
            userContentDao = db.userContentDao(),
            wakeUpRetryHelper = wakeUpRetryHelper,
            ioDispatcher = Dispatchers.Unconfined
        )

        // Seed activities into database
        runBlocking {
            val a1 = ActivityEntity(
                id = "act-1",
                title = "Triund Sunset Trek",
                description = "Popular scenic trek in Dharamshala",
                location = "McLeod Ganj",
                latitude = 32.24,
                longitude = 76.32,
                difficulty = DifficultyLevel.EASY,
                basePrice = BigDecimal("1200.00"),
                cancellationPolicy = CancellationPolicyType.FLEXIBLE,
                minSlotPrice = BigDecimal("1500.00"),
                lastUpdated = 1000L
            )
            val a2 = ActivityEntity(
                id = "act-2",
                title = "Hampta Pass Trek",
                description = "Cross-over trek from Kullu to Spiti",
                location = "Manali",
                latitude = 32.27,
                longitude = 77.20,
                difficulty = DifficultyLevel.MODERATE,
                basePrice = BigDecimal("8500.00"),
                cancellationPolicy = CancellationPolicyType.MODERATE,
                minSlotPrice = BigDecimal("9000.00"),
                lastUpdated = 2000L
            )
            db.activityDao().insertActivities(listOf(a1, a2))
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `explore screen displays cards in grid and greeting`() {
        val viewModel = ExploreViewModel(repository, sessionManager)

        composeTestRule.setContent {
            PeakConnectTheme {
                ExploreScreen(
                    onNavigateToActivityDetail = {},
                    viewModel = viewModel
                )
            }
        }

        composeTestRule.waitForIdle()

        // Greeting shows first name
        composeTestRule.onNodeWithText("Hi, Alex").assertExists().assertIsDisplayed()

        // Activity titles appear on cards
        composeTestRule.onNodeWithText("Triund Sunset Trek").assertExists().assertIsDisplayed()
        composeTestRule.onNodeWithText("Hampta Pass Trek").assertExists().assertIsDisplayed()
    }

    @Test
    fun `empty state appears for non-matching query`() {
        val viewModel = ExploreViewModel(repository, sessionManager)

        composeTestRule.setContent {
            PeakConnectTheme {
                ExploreScreen(
                    onNavigateToActivityDetail = {},
                    viewModel = viewModel
                )
            }
        }

        composeTestRule.waitForIdle()

        // Submit a non-matching query directly via ViewModel
        viewModel.onSearchSubmitted("NonExistentTrek123")
        composeTestRule.waitForIdle()

        // Empty state is shown
        composeTestRule.onNodeWithText("No activities match your search").assertExists().assertIsDisplayed()
        composeTestRule.onNodeWithText("Clear filters").assertExists().assertIsDisplayed()
    }
}
