package com.signkorean.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.signkorean.designsystem.theme.SignKoreanTheme
import com.signkorean.feature.onboarding.OnboardingChoiceTestTags
import com.signkorean.feature.onboarding.OnboardingChoiceUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingDestinationsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun level_skip_selects_beginner_and_continues_to_interests() {
        var skipped = false
        var continueCount = 0

        composeRule.setContent {
            SignKoreanTheme {
                LevelDestination(
                    state = OnboardingChoiceUiState(),
                    onSelectLevel = {},
                    onSkipLevel = { skipped = true },
                    onContinue = { continueCount++ },
                )
            }
        }

        composeRule.onNodeWithTag(OnboardingChoiceTestTags.LEVEL_SKIP).performClick()

        composeRule.runOnIdle {
            assertTrue(skipped)
            assertEquals(1, continueCount)
        }
    }
}
