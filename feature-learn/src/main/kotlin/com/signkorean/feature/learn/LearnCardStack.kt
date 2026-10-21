@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.feature.learn

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.signkorean.designsystem.components.GameCard
import com.signkorean.designsystem.components.GameIconButton
import com.signkorean.designsystem.testing.SignKoreanTestTags
import com.signkorean.designsystem.theme.SignKoreanSpacing
import com.signkorean.domain.gesture.AnswerGestureInput
import com.signkorean.domain.gesture.AnswerRelease
import com.signkorean.domain.gesture.DragAxis
import com.signkorean.domain.gesture.DragOverlay
import com.signkorean.domain.gesture.GesturePolicy
import com.signkorean.domain.gesture.QuestionGestureInput
import com.signkorean.domain.gesture.QuestionRelease
import com.signkorean.domain.gesture.RatingDecision
import com.signkorean.domain.model.AudioPlaybackState
import com.signkorean.domain.model.RatingInputSource
import kotlinx.coroutines.delay

@Composable
internal fun LearnCardStack(
    state: LearnSessionUiState.Active,
    onAction: (LearnUiAction) -> Unit,
    audioState: AudioPlaybackState = AudioPlaybackState.Idle,
    reduceMotion: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val policy = remember { GesturePolicy() }
    val motionPolicy = remember(reduceMotion) { MotionPolicy(reduceMotion) }
    var dragOffsetDp by remember(state.cardId, state.stage) { mutableFloatStateOf(0f) }
    var dragAxis by remember(state.cardId, state.stage) { mutableStateOf(DragAxis.Undecided) }
    var dragOverlay by remember(state.cardId, state.stage) { mutableStateOf<DragOverlay?>(null) }
    val hapticFeedback = LocalHapticFeedback.current
    LaunchedEffect(state.isRating) {
        if (state.isRating) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(motionPolicy.ratingAnimationDurationMillis)
            onAction(LearnUiAction.RatingAnimationFinished)
        }
    }
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
                    visuals = cardPreviewVisuals(layer.role, dragOffsetDp, reduceMotion),
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(top = layer.previewOffset(), bottom = PREVIEW_OFFSET),
                )
            }
        ActiveCard(
            state = state,
            dragOffsetDp = dragOffsetDp,
            dragOverlay = dragOverlay,
            ratingDecision = state.ratingDecision,
            isRating = state.isRating,
            onAction = onAction,
            audioState = audioState,
            motionPolicy = motionPolicy,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(bottom = PREVIEW_OFFSET * previewCount.toFloat())
                    .cardPointerInput(
                        density = androidx.compose.ui.platform.LocalDensity.current,
                        onMove = { offsetXDp, offsetYDp ->
                            if (state.isRating) {
                                false
                            } else if (state.stage == LearnCardStage.Question) {
                                val feedback =
                                    policy.drag(
                                        QuestionGestureInput(
                                            dxDp = offsetXDp,
                                            dyDp = offsetYDp,
                                            previousAxis = dragAxis,
                                        ),
                                    )
                                dragAxis = feedback.axis
                                dragOffsetDp = feedback.offsetDp
                                dragOverlay = null
                                feedback.axis == DragAxis.Horizontal
                            } else {
                                val feedback =
                                    policy.drag(
                                        AnswerGestureInput(
                                            dxDp = offsetXDp,
                                            dyDp = offsetYDp,
                                            previousAxis = dragAxis,
                                        ),
                                    )
                                dragAxis = feedback.axis
                                dragOffsetDp = feedback.offsetDp
                                dragOverlay = feedback.overlay
                                feedback.axis == DragAxis.Horizontal
                            }
                        },
                        onRelease = { cancelled, outOfBounds ->
                            if (!cancelled || outOfBounds) {
                                if (state.stage == LearnCardStage.Question) {
                                    when (
                                        policy.release(
                                            QuestionGestureInput(
                                                dxDp = dragOffsetDp,
                                                dyDp = 0f,
                                                previousAxis = dragAxis,
                                            ),
                                        )
                                    ) {
                                        QuestionRelease.CheckAnswerFirst -> onAction(LearnUiAction.CheckAnswerFirst)

                                        QuestionRelease.Ignored,
                                        QuestionRelease.SnapBack,
                                        -> Unit
                                    }
                                } else {
                                    val release =
                                        policy.release(
                                            AnswerGestureInput(
                                                dxDp = dragOffsetDp,
                                                dyDp = 0f,
                                                previousAxis = dragAxis,
                                            ),
                                        )
                                    when (release) {
                                        is AnswerRelease.Commit -> {
                                            onAction(
                                                LearnUiAction.RateCard(release.decision, RatingInputSource.Gesture),
                                            )
                                        }

                                        AnswerRelease.Ignored,
                                        AnswerRelease.SnapBack,
                                        -> {
                                            Unit
                                        }
                                    }
                                }
                            }
                            dragOffsetDp = 0f
                            dragAxis = DragAxis.Undecided
                            dragOverlay = null
                        },
                    ),
        )
    }
}

@Composable
private fun PreviewCard(
    preview: LearnCardPreviewUiModel,
    visuals: CardPreviewVisuals,
    modifier: Modifier,
) {
    GameCard(
        modifier =
            modifier
                .graphicsLayer {
                    scaleX = visuals.scale
                    scaleY = visuals.scale
                    alpha = visuals.alpha
                }.clearAndSetSemantics {},
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
    dragOffsetDp: Float,
    dragOverlay: DragOverlay?,
    ratingDecision: RatingDecision?,
    isRating: Boolean,
    onAction: (LearnUiAction) -> Unit,
    audioState: AudioPlaybackState,
    motionPolicy: MotionPolicy,
    modifier: Modifier,
) {
    val density = LocalDensity.current.density
    val focusRequester = remember { FocusRequester() }
    var widthPx by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(state.cardId) {
        focusRequester.requestFocus()
    }
    Box(
        modifier =
            modifier
                .onSizeChanged { widthPx = it.width.toFloat() }
                .testTag(SignKoreanTestTags.LEARN_KEYBOARD_STAGE)
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (isRating || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val key =
                        when (event.key) {
                            Key.DirectionLeft -> CardKeyboardKey.ArrowLeft
                            Key.DirectionRight -> CardKeyboardKey.ArrowRight
                            Key.Escape -> CardKeyboardKey.Escape
                            else -> CardKeyboardKey.Other
                        }
                    when (val action = cardKeyboardAction(key, state.stage)) {
                        is CardKeyboardAction.Rate -> onAction(LearnUiAction.RateCard(action.decision, RatingInputSource.Keyboard))
                        CardKeyboardAction.CheckAnswerFirst -> onAction(LearnUiAction.CheckAnswerFirst)
                        CardKeyboardAction.Close -> onAction(LearnUiAction.CloseSession)
                        CardKeyboardAction.Ignored -> return@onPreviewKeyEvent false
                    }
                    true
                }.graphicsLayer {
                    val exitDirection =
                        when (ratingDecision) {
                            RatingDecision.GotIt -> -1f
                            RatingDecision.Review -> 1f
                            null -> 0f
                        }
                    translationX =
                        if (isRating) {
                            exitDirection * widthPx * motionPolicy.cardExitTranslationFactor
                        } else {
                            dragOffsetDp * density
                        }
                    rotationZ =
                        if (isRating) {
                            exitDirection * motionPolicy.cardExitRotationDegrees
                        } else if (motionPolicy.reduceMotion) {
                            0f
                        } else {
                            (dragOffsetDp / 12f).coerceIn(-8f, 8f)
                        }
                    alpha = if (isRating && motionPolicy.reduceMotion) 0f else 1f
                },
    ) {
        GameCard(modifier = Modifier.fillMaxSize().testTag(SignKoreanTestTags.LEARN_ACTIVE_CARD)) {
            ActiveCardContent(state, onAction, audioState)
        }
        (dragOverlay ?: ratingDecision?.toOverlay()).let { overlay ->
            if (overlay == null) return@let
            Text(
                text = if (overlay == DragOverlay.GotIt) "Got it" else "Review",
                modifier =
                    Modifier
                        .align(if (overlay == DragOverlay.GotIt) Alignment.CenterStart else Alignment.CenterEnd)
                        .testTag(SignKoreanTestTags.LEARN_DRAG_OVERLAY),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.ActiveCardContent(
    state: LearnSessionUiState.Active,
    onAction: (LearnUiAction) -> Unit,
    audioState: AudioPlaybackState,
) {
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
    val informationScroll = rememberScrollState()
    LaunchedEffect(state.cardId, state.stage) {
        informationScroll.scrollTo(0)
    }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .weight(INFO_PORTION)
                .verticalScroll(informationScroll)
                .testTag(SignKoreanTestTags.LEARN_INFO_PANEL),
    ) {
        when (state.stage) {
            LearnCardStage.Question -> {
                CardQuestion()
            }

            LearnCardStage.Answer -> {
                CardAnswer(
                    cardId = state.cardId,
                    sign = state.sign,
                    answer = state.answer,
                    audioState = audioState,
                    onPlayAudio = { onAction(LearnUiAction.PlayAudio) },
                )
            }
        }
    }
}

@Composable
internal fun CardQuestion() {
    Column(modifier = Modifier.testTag(SignKoreanTestTags.LEARN_QUESTION)) {
        Text(
            text = "What does this sign mean?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = "Take a moment to read the sign before you reveal it.",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
internal fun CardAnswer(
    cardId: String = "",
    sign: String,
    answer: LearnAnswerUiModel,
    onPlayAudio: () -> Unit,
    audioState: AudioPlaybackState = AudioPlaybackState.Idle,
) {
    val isPlaying = audioState is AudioPlaybackState.Playing && audioState.cardId == cardId
    Column(modifier = Modifier.testTag(SignKoreanTestTags.LEARN_ANSWER)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Answer",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
            )
            GameIconButton(
                symbol = if (isPlaying) "■" else "♪",
                contentDescription = if (isPlaying) "Playing pronunciation" else "Play pronunciation",
                onClick = onPlayAudio,
                modifier = Modifier.testTag(SignKoreanTestTags.LEARN_PLAY_AUDIO),
            )
        }
        Text(text = sign, style = MaterialTheme.typography.displayMedium)
        Text(text = answer.displayMeaning, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        if (answer.showRomanization && answer.pronunciationGuide.isNotBlank()) {
            Text(
                text = answer.pronunciationGuide,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Text(text = "Words", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        answer.wordParts.forEach { wordPart ->
            Text(
                text = "${wordPart.korean} · ${wordPart.meaning}",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Text(text = "Street note", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Text(text = answer.streetNote, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "← Got it · Review →",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
        )
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
private fun RatingDecision.toOverlay(): DragOverlay =
    when (this) {
        RatingDecision.GotIt -> DragOverlay.GotIt
        RatingDecision.Review -> DragOverlay.Review
    }
