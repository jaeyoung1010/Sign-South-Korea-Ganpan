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
import androidx.compose.ui.res.stringResource
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
            text = stringResource(R.string.learn_report_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
        )
        CardReportReason.entries.forEach { reason ->
            GameButton(
                text = stringResource(reason.labelResource),
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
            label = { Text(stringResource(R.string.learn_report_details_label)) },
            placeholder = { Text(stringResource(R.string.learn_report_details_hint)) },
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
            text = stringResource(R.string.learn_report_privacy_notice),
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
            text = if (state.status == ReportStatus.Saving) stringResource(R.string.learn_report_sending) else stringResource(R.string.learn_report_send),
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().testTag(SignKoreanTestTags.LEARN_REPORT_SUBMIT),
            enabled = state.canSubmit,
        )
        GameButtonRow {
            GameButton(
                text = stringResource(R.string.learn_report_cancel),
                onClick = onCancel,
                modifier = Modifier.weight(1f).testTag(SignKoreanTestTags.LEARN_REPORT_CANCEL),
                style = GameButtonStyle.Secondary,
            )
        }
    }
}

private val CardReportReason.labelResource: Int
    get() =
        when (this) {
            CardReportReason.WRONG_KOREAN -> R.string.learn_report_wrong_korean
            CardReportReason.WRONG_MEANING -> R.string.learn_report_wrong_meaning
            CardReportReason.PHOTO_PROBLEM -> R.string.learn_report_photo_problem
            CardReportReason.AUDIO_PROBLEM -> R.string.learn_report_audio_problem
            CardReportReason.OTHER -> R.string.learn_report_other
        }
