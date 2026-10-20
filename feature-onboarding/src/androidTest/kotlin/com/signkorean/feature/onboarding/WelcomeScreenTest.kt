package com.signkorean.feature.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.signkorean.designsystem.theme.SignKoreanTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WelcomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun welcome_shows_value_copy_collage_account_notice_and_visible_get_started_action() {
        composeRule.setContent {
            SignKoreanTheme {
                Box(modifier = Modifier.height(360.dp)) {
                    WelcomeScreen(onGetStarted = {})
                }
            }
        }

        composeRule.onNodeWithText("Read Korea in the real world").assertIsDisplayed()
        composeRule.onNodeWithText("You can use the app without an account.").assertIsDisplayed()
        composeRule.onNodeWithTag(WelcomeTestTags.COLLAGE).assertIsDisplayed()
        composeRule.onNodeWithTag(WelcomeTestTags.GET_STARTED).assertIsDisplayed()
    }

    @Test
    fun get_started_reports_one_continue_action() {
        var started = 0
        composeRule.setContent {
            SignKoreanTheme {
                WelcomeScreen(onGetStarted = { started += 1 })
            }
        }

        composeRule.onNodeWithTag(WelcomeTestTags.GET_STARTED).performClick()

        assertEquals(1, started)
    }
}
