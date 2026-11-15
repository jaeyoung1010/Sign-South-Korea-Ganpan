package com.signkorean.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.signkorean.designsystem.testing.SignKoreanTestTags
import com.signkorean.designsystem.theme.SignKoreanTheme
import com.signkorean.domain.model.CardReportReason
import com.signkorean.feature.learn.ReportSheet
import com.signkorean.feature.learn.ReportUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReportSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun all_five_reasons_are_shown_and_send_stays_disabled_until_one_is_selected() {
        var selectedReason by mutableStateOf<CardReportReason?>(null)
        composeRule.setContent {
            SignKoreanTheme {
                ReportSheet(
                    state = ReportUiState(selectedReason = selectedReason),
                    onReasonSelected = { selectedReason = it },
                    onDetailsChanged = {},
                    onSubmit = {},
                    onCancel = {},
                )
            }
        }

        CardReportReason.entries.forEach { reason ->
            composeRule.onNodeWithTag(SignKoreanTestTags.learnReportReason(reason.name)).assertIsDisplayed()
        }
        composeRule.onNodeWithTag(SignKoreanTestTags.LEARN_REPORT_SUBMIT).assertIsNotEnabled()
        composeRule.onNodeWithTag(SignKoreanTestTags.learnReportReason(CardReportReason.OTHER.name)).performClick()
        composeRule.onNodeWithTag(SignKoreanTestTags.LEARN_REPORT_SUBMIT).assertIsEnabled()
    }

    @Test
    fun details_field_shows_privacy_guidance_and_cancel_is_exposed() {
        val details = mutableListOf<String>()
        val cancellations = mutableListOf<Unit>()
        composeRule.setContent {
            SignKoreanTheme {
                ReportSheet(
                    state = ReportUiState(selectedReason = CardReportReason.PHOTO_PROBLEM, details = details.lastOrNull() ?: ""),
                    onReasonSelected = {},
                    onDetailsChanged = { details += it },
                    onSubmit = {},
                    onCancel = { cancellations += Unit },
                )
            }
        }

        composeRule.onNodeWithText("Please do not include personal information.").assertIsDisplayed()
        composeRule.onNodeWithTag(SignKoreanTestTags.LEARN_REPORT_DETAILS).performTextInput("The photo is blurry.")
        composeRule.onNodeWithTag(SignKoreanTestTags.LEARN_REPORT_CANCEL).performScrollTo().assertIsDisplayed().performClick()

        composeRule.runOnIdle {
            assertEquals("The photo is blurry.", details.single())
            assertEquals(1, cancellations.size)
        }
    }
}
