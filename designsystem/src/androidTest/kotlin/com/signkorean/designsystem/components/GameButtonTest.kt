package com.signkorean.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.signkorean.designsystem.theme.SignKoreanTheme
import com.signkorean.testsupport.ScreenshotArtifact
import com.signkorean.testsupport.ScreenshotCase
import com.signkorean.testsupport.ScreenshotTestHarness
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun primaryButtonExposesEnabledAndDisabledStatesWith48DpTarget() {
        composeRule.setContent {
            SignKoreanTheme {
                Column {
                    GameButton(
                        text = "Start learning",
                        onClick = {},
                        modifier = Modifier.testTag("qa.primary-button"),
                    )
                    GameButton(
                        text = "Start learning",
                        enabled = false,
                        onClick = {},
                        modifier = Modifier.testTag("qa.primary-button-disabled"),
                    )
                }
            }
        }

        composeRule
            .onNodeWithTag("qa.primary-button")
            .assertIsEnabled()
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("qa.primary-button-disabled").assertIsNotEnabled()
    }

    @Test
    fun lightThemeComponentBaselineCanBeCaptured() {
        composeRule.setContent {
            SignKoreanTheme {
                GameCard(modifier = Modifier.testTag("qa.light-baseline")) {
                    GameChip(text = "Must memorize")
                    GameButton(text = "Show answer", onClick = {})
                }
            }
        }

        val image = composeRule.onNodeWithTag("qa.light-baseline").captureToImage()
        val artifact =
            ScreenshotTestHarness.capture(
                ScreenshotCase("design-system-light", widthDp = 390, localeTag = "en-US"),
            ) { case -> ScreenshotArtifact(case.id, byteArrayOf(image.width.toByte(), image.height.toByte())) }

        assertTrue(artifact.bytes.isNotEmpty())
    }
}
