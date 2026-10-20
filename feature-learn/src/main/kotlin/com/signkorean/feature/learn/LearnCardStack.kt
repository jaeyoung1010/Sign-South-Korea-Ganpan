@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.feature.learn

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.signkorean.designsystem.components.GameCard
import com.signkorean.designsystem.testing.SignKoreanTestTags
import com.signkorean.designsystem.theme.SignKoreanSpacing

@Composable
internal fun LearnCardStack(
    state: LearnSessionUiState.Active,
    modifier: Modifier = Modifier,
) {
    val layers = cardStackLayers(state.cardId, state.upcomingCards.map(LearnCardPreviewUiModel::cardId))
    val previewsById = state.upcomingCards.associateBy(LearnCardPreviewUiModel::cardId)
    val previewCount = layers.count { it.role != LearnCardLayerRole.Active }
    Box(modifier = modifier.testTag(SignKoreanTestTags.LEARN_CARD_STACK)) {
        layers
            .filter { it.role != LearnCardLayerRole.Active }
            .forEach { layer ->
                val preview = checkNotNull(previewsById[layer.cardId])
                PreviewCard(
                    preview = preview,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(top = layer.previewOffset(), bottom = PREVIEW_OFFSET),
                )
            }
        ActiveCard(
            state = state,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        bottom =
                            PREVIEW_OFFSET *
                                previewCount.toFloat(),
                    ),
        )
    }
}

@Composable
private fun PreviewCard(
    preview: LearnCardPreviewUiModel,
    modifier: Modifier,
) {
    GameCard(
        modifier = modifier.clearAndSetSemantics {},
    ) {
        PhotoSurface(
            imageAssetUri = preview.imageAssetUri,
            contentDescription = "Upcoming card",
            focus = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ActiveCard(
    state: LearnSessionUiState.Active,
    modifier: Modifier,
) {
    GameCard(modifier = modifier.testTag(SignKoreanTestTags.LEARN_ACTIVE_CARD)) {
        ActiveCardContent(state)
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.ActiveCardContent(state: LearnSessionUiState.Active) {
    PhotoSurface(
        imageAssetUri = state.photo.imageAssetUri,
        contentDescription =
            if (state.stage == LearnCardStage.Question) {
                state.photo.questionContentDescription
            } else {
                state.photo.answerContentDescription
            },
        focus = state.photo.focus,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp)
                .weight(PHOTO_PORTION)
                .padding(bottom = SignKoreanSpacing.small),
    )
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .weight(INFO_PORTION),
    ) {
        Text(
            text = if (state.stage == LearnCardStage.Question) "What does this sign mean?" else "Answer",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = state.sign,
            style = MaterialTheme.typography.displayMedium,
        )
        if (state.stage == LearnCardStage.Answer) {
            Text("Answer details will appear here.", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun LearnCardLayer.previewOffset() =
    when (role) {
        LearnCardLayerRole.NextNext -> PREVIEW_OFFSET * 2f
        LearnCardLayerRole.Next -> PREVIEW_OFFSET
        LearnCardLayerRole.Active -> 0.dp
    }

private val PREVIEW_OFFSET = 14.dp
private const val PHOTO_PORTION = .62f
private const val INFO_PORTION = .38f
