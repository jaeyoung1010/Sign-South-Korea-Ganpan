@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.feature.learn

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.signkorean.designsystem.components.GameButton
import com.signkorean.designsystem.components.GameButtonRow
import com.signkorean.designsystem.components.GameButtonStyle
import com.signkorean.designsystem.testing.SignKoreanTestTags
import com.signkorean.designsystem.theme.SignKoreanSpacing
import com.signkorean.domain.model.CardReportReason
import com.signkorean.domain.model.ReportDetails

enum class ReportStatus {
    Editing,
    Saving,
    Error,
}

data class ReportUiState(
    val selectedReason: CardReportReason? = null,
    val details: String = "",
    val status: ReportStatus = ReportStatus.Editing,
    val errorMessage: String? = null,
) {
    val canSubmit: Boolean
        get() = selectedReason != null && status != ReportStatus.Saving

    fun withDetails(value: String): ReportUiState = copy(details = value.takeCodePoints(MAX_DETAILS_CODE_POINTS))

    private companion object {
        const val MAX_DETAILS_CODE_POINTS = 500

        fun String.takeCodePoints(maxCodePoints: Int): String {
            if (codePointCount(0, length) <= maxCodePoints) return this
            return substring(0, offsetByCodePoints(0, maxCodePoints))
        }
    }
}

@Composable
fun ReportSheet(
    state: ReportUiState,
    onReasonSelected: (CardReportReason) -> Unit,
    onDetailsChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight(0.85f).verticalScroll(rememberScrollState()),
        verticalArrangement =
            androidx.compose.foundation.layout.Arrangement
                .spacedBy(SignKoreanSpacing.small),
    ) {
        Text(
            text = "What should we check?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
        )
        CardReportReason.entries.forEach { reason ->
            GameButton(
                text = reason.label,
                onClick = { onReasonSelected(reason) },
                modifier = Modifier.fillMaxWidth().testTag(SignKoreanTestTags.learnReportReason(reason.name)),
                style = if (state.selectedReason == reason) GameButtonStyle.Primary else GameButtonStyle.Secondary,
                enabled = state.status != ReportStatus.Saving,
            )
        }
        OutlinedTextField(
            value = state.details,
            onValueChange = onDetailsChanged,
            modifier = Modifier.fillMaxWidth().testTag(SignKoreanTestTags.LEARN_REPORT_DETAILS),
            label = { Text("Optional details") },
            placeholder = { Text("Do not include personal information") },
            supportingText = {
                Text(
                    "${state.details.codePointCount(0, state.details.length)} / " +
                        ReportDetails.MAX_CODE_POINTS,
                )
            },
            minLines = 3,
            maxLines = 5,
            enabled = state.status != ReportStatus.Saving,
        )
        Text(
            text = "Please do not include personal information.",
            style = MaterialTheme.typography.bodySmall,
        )
        state.errorMessage?.let { message ->
            Text(
                text = message,
                modifier = Modifier.testTag(SignKoreanTestTags.LEARN_REPORT_ERROR),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        GameButton(
            text = if (state.status == ReportStatus.Saving) "Sending…" else "Send report",
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().testTag(SignKoreanTestTags.LEARN_REPORT_SUBMIT),
            enabled = state.canSubmit,
        )
        GameButtonRow {
            GameButton(
                text = "Cancel",
                onClick = onCancel,
                modifier = Modifier.weight(1f).testTag(SignKoreanTestTags.LEARN_REPORT_CANCEL),
                style = GameButtonStyle.Secondary,
            )
        }
    }
}

private val CardReportReason.label: String
    get() =
        when (this) {
            CardReportReason.WRONG_KOREAN -> "Korean text looks wrong"
            CardReportReason.WRONG_MEANING -> "Meaning looks wrong"
            CardReportReason.PHOTO_PROBLEM -> "Photo problem"
            CardReportReason.AUDIO_PROBLEM -> "Audio problem"
            CardReportReason.OTHER -> "Something else"
        }
