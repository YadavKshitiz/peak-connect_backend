package com.peakconnect.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.peakconnect.app.core.theme.PeakConnectTheme
import com.peakconnect.app.core.ui.EmptyState
import com.peakconnect.app.domain.model.ExperienceLevel
import com.peakconnect.app.domain.model.MatchedGuide
import com.peakconnect.app.feature.trekker.matches.GuideCard
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GuideCardUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleGuide = MatchedGuide(
        guideId = "guide-test-1",
        name = "Kavita Negi",
        skills = listOf("First Aid", "Flora & Fauna", "Navigation", "Camp Setup"),
        languages = listOf("English", "Hindi"),
        location = "Shimla",
        experienceLevel = ExperienceLevel.EXPERT,
        score = 0.88
    )

    @Test
    fun `guide card renders rank 1 best match badge name experience and score`() {
        var clicked = false

        composeTestRule.setContent {
            PeakConnectTheme {
                GuideCard(
                    guide = sampleGuide,
                    rank = 1,
                    onClick = { clicked = true }
                )
            }
        }

        // Rank 1 Best Match badge
        composeTestRule.onNodeWithTag("best_match_badge", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("Best match").assertIsDisplayed()

        // Name and location
        composeTestRule.onNodeWithText("Kavita Negi").assertIsDisplayed()
        composeTestRule.onNodeWithText("Shimla").assertIsDisplayed()

        // Experience Level
        composeTestRule.onNodeWithText("Expert").assertIsDisplayed()

        // Match Score (88%)
        composeTestRule.onNodeWithText("88% match").assertIsDisplayed()

        // Skills (first 3 and +1 for fourth)
        composeTestRule.onNodeWithText("First Aid").assertIsDisplayed()
        composeTestRule.onNodeWithText("Flora & Fauna").assertIsDisplayed()
        composeTestRule.onNodeWithText("Navigation").assertIsDisplayed()
        composeTestRule.onNodeWithText("+1").assertIsDisplayed()

        // Click card
        composeTestRule.onNodeWithTag("guide_card_${sampleGuide.guideId}").performClick()
        assertTrue(clicked)
    }

    @Test
    fun `guide card renders rank 2 with number badge instead of best match`() {
        composeTestRule.setContent {
            PeakConnectTheme {
                GuideCard(
                    guide = sampleGuide,
                    rank = 2,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("best_match_badge").assertDoesNotExist()
        composeTestRule.onNodeWithText("#2").assertIsDisplayed()
    }

    @Test
    fun `empty state renders message and back action button`() {
        var backClicked = false

        composeTestRule.setContent {
            PeakConnectTheme {
                EmptyState(
                    icon = Icons.Default.PersonSearch,
                    title = "No guides are available for this slot right now",
                    message = "",
                    actionLabel = "Go back",
                    onAction = { backClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("No guides are available for this slot right now").assertIsDisplayed()
        composeTestRule.onNodeWithText("Go back").assertIsDisplayed()
        composeTestRule.onNodeWithText("Go back").performClick()
        assertTrue(backClicked)
    }
}
