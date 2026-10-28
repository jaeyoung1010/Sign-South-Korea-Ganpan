@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.feature.learn

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.signkorean.designsystem.components.GameCard
import com.signkorean.designsystem.components.GameIconButton
import com.signkorean.designsystem.testing.SignKoreanTestTags
import com.signkorean.designsystem.theme.SignKoreanPalette
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
    modifier: Modifier = Modifier,
    audioState: AudioPlaybackState = AudioPlaybackState.Idle,
    reduceMotion: Boolean = false,
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
    val layers =
        remember(state.cardId, state.upcomingCards) {
            cardStackLayers(state.cardId, state.upcomingCards.map(LearnCardPreviewUiModel::cardId))
        }
    val previewsById = remember(state.cardId, state.upcomingCards) { state.upcomingCards.associateBy(LearnCardPreviewUiModel::cardId) }
    val dragOffsetState = rememberUpdatedState(dragOffsetDp)
    val previewCount = layers.count { it.role != LearnCardLayerRole.Active }
    Box(modifier = modifier.testTag(SignKoreanTestTags.LEARN_CARD_STACK)) {
        layers
            .filter { it.role != LearnCardLayerRole.Active }
            .forEach { layer ->
                val preview = checkNotNull(previewsById[layer.cardId])
                PreviewCard(
                    preview = preview,
                    role = layer.role,
                    dragOffset = dragOffsetState,
                    reduceEffects = reduceMotion,
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
    role: LearnCardLayerRole,
    dragOffset: androidx.compose.runtime.State<Float>,
    reduceEffects: Boolean,
    modifier: Modifier,
) {
    GameCard(
        modifier =
            modifier
                .graphicsLayer {
                    val progress =
                        if (reduceEffects) {
                            0f
                        } else {
                            (kotlin.math.abs(dragOffset.value) / 90f).coerceIn(0f, 1f)
                        }
                    val scale =
                        when (role) {
                            LearnCardLayerRole.Next -> 0.96f + 0.04f * progress
                            LearnCardLayerRole.NextNext -> 0.92f + 0.03f * progress
                            LearnCardLayerRole.Active -> 1f
                        }
                    val alpha =
                        when (role) {
                            LearnCardLayerRole.Next -> 0.9f + 0.1f * progress
                            LearnCardLayerRole.NextNext -> 0.72f + 0.13f * progress
                            LearnCardLayerRole.Active -> 1f
                        }
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }.clearAndSetSemantics {},
    ) {
        PhotoSurface(
            imageAssetUri = preview.imageAssetUri,
            contentDescription = stringResource(R.string.learn_upcoming_card),
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
    val ratingProgress by
        animateFloatAsState(
            targetValue = if (isRating) 1f else 0f,
            animationSpec = tween(durationMillis = motionPolicy.ratingAnimationDurationMillis.toInt()),
            label = "rating-card-exit",
        )
    val focusRequester = remember { FocusRequester() }
    var widthPx by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(state.cardId, state.stage) {
        focusRequester.requestFocus()
    }
    val hapticFeedback = LocalHapticFeedback.current
    val markAsGotItLabel = stringResource(R.string.learn_mark_as_got_it)
    val markForReviewLabel = stringResource(R.string.learn_mark_for_review)
    val stageDescription =
        if (state.stage == LearnCardStage.Question) {
            stringResource(R.string.learn_question)
        } else {
            stringResource(R.string.learn_answer)
        }
    Box(
        modifier =
            modifier
                .onSizeChanged { widthPx = it.width.toFloat() }
                .testTag(SignKoreanTestTags.LEARN_KEYBOARD_STAGE)
                .semantics { stateDescription = stageDescription }
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
                },
    ) {
        GameCard(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                    val exitDirection =
                        when (ratingDecision) {
                            RatingDecision.GotIt -> -1f
                            RatingDecision.Review -> 1f
                            null -> 0f
                        }
                    translationX =
                        if (isRating) {
                            exitDirection * widthPx * motionPolicy.cardExitTranslationFactor * ratingProgress
                        } else {
                            dragOffsetDp * density
                        }
                    rotationZ =
                        if (isRating) {
                            exitDirection * motionPolicy.cardExitRotationDegrees * ratingProgress
                        } else if (motionPolicy.reduceMotion) {
                            0f
                        } else {
                            (dragOffsetDp / 12f).coerceIn(-8f, 8f)
                        }
                    alpha = if (isRating && motionPolicy.reduceMotion) 1f - ratingProgress else 1f
                    }
                    .testTag(SignKoreanTestTags.LEARN_ACTIVE_CARD)
                    .semantics {
                        customActions =
                            listOf(
                                CustomAccessibilityAction(markAsGotItLabel) {
                                    dispatchRatingAction(
                                        state = state,
                                        decision = RatingDecision.GotIt,
                                        onAction = onAction,
                                        hapticFeedback = hapticFeedback,
                                    )
                                },
                                CustomAccessibilityAction(markForReviewLabel) {
                                    dispatchRatingAction(
                                        state = state,
                                        decision = RatingDecision.Review,
                                        onAction = onAction,
                                        hapticFeedback = hapticFeedback,
                                    )
                                },
                            )
                    },
        ) {
            ActiveCardContent(state, onAction, audioState)
        }
        (dragOverlay ?: ratingDecision?.toOverlay()).let { overlay ->
            if (overlay == null) return@let
            SwipeActionOverlay(overlay = overlay)
        }
    }
}

@Composable
private fun BoxScope.SwipeActionOverlay(overlay: DragOverlay) {
    val isGotIt = overlay == DragOverlay.GotIt
    val accent = Color(if (isGotIt) SignKoreanPalette.PRIMARY else SignKoreanPalette.SURFACE_STRONG)
    val darkAccent = Color(if (isGotIt) SignKoreanPalette.PRIMARY_DARK else SignKoreanPalette.OUTLINE)
    val label = stringResource(if (isGotIt) R.string.learn_got_it else R.string.learn_review)
    val actionIcon = if (isGotIt) "✓" else "↻"
    var isVisible by remember(overlay) { mutableStateOf(false) }
    LaunchedEffect(overlay) { isVisible = true }
    val scale by
        animateFloatAsState(
            targetValue = if (isVisible) 1f else 0.78f,
            animationSpec = spring(dampingRatio = 0.58f, stiffness = 460f),
            label = "swipe-action-scale",
        )
    val alpha by
        animateFloatAsState(
            targetValue = if (isVisible) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
            label = "swipe-action-alpha",
        )

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(accent.copy(alpha = 0.16f))
                .clearAndSetSemantics {},
    )
    Surface(
        modifier =
            Modifier
                .align(if (isGotIt) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(SignKoreanSpacing.large)
                .graphicsLayer {
                    this.alpha = alpha
                    scaleX = scale
                    scaleY = scale
                }.testTag(SignKoreanTestTags.LEARN_DRAG_OVERLAY),
        color = accent,
        contentColor = Color(SignKoreanPalette.INK),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, darkAccent),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = SignKoreanSpacing.medium, vertical = SignKoreanSpacing.small),
            horizontalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = actionIcon, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Column {
                Text(text = label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(
                    text = if (isGotIt) "←" else "→",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

private fun dispatchRatingAction(
    state: LearnSessionUiState.Active,
    decision: RatingDecision,
    onAction: (LearnUiAction) -> Unit,
    hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback,
): Boolean {
    if (state.stage != LearnCardStage.Answer || state.isRating) return false
    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    onAction(LearnUiAction.RateCard(decision, RatingInputSource.Accessibility))
    return true
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
                .heightIn(min = MIN_PHOTO_HEIGHT)
                .weight(PHOTO_PORTION)
                .padding(bottom = SignKoreanSpacing.small),
            onClick = { onAction(LearnUiAction.ShowImageViewer) },
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
                    hasAudio = state.audioAssetUris.isNotEmpty(),
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
            text = stringResource(R.string.learn_question),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = stringResource(R.string.learn_question_hint),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
internal fun CardAnswer(
    cardId: String = "",
    sign: String,
    answer: LearnAnswerUiModel,
    hasAudio: Boolean,
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
                text = stringResource(R.string.learn_answer),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
            )
            if (hasAudio) {
                GameIconButton(
                    symbol = if (isPlaying) "■" else "♪",
                    contentDescription = if (isPlaying) stringResource(R.string.learn_playing_pronunciation) else stringResource(R.string.learn_play_pronunciation),
                    onClick = onPlayAudio,
                    modifier = Modifier.testTag(SignKoreanTestTags.LEARN_PLAY_AUDIO),
                )
            }
        }
        Text(text = sign, style = MaterialTheme.typography.displayMedium)
        Text(text = answer.displayMeaning, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        if (answer.showRomanization && answer.pronunciationGuide.isNotBlank()) {
            Text(
                text = answer.pronunciationGuide,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Text(text = stringResource(R.string.learn_words), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        answer.wordParts.forEach { wordPart ->
            Text(
                text = "${wordPart.korean} · ${wordPart.meaning}",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Text(text = stringResource(R.string.learn_street_note), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Text(text = answer.streetNote, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(R.string.learn_gesture_hint),
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
private val MIN_PHOTO_HEIGHT = 200.dp
private const val PHOTO_PORTION = .62f
private const val INFO_PORTION = .38f
private fun RatingDecision.toOverlay(): DragOverlay =
    when (this) {
        RatingDecision.GotIt -> DragOverlay.GotIt
        RatingDecision.Review -> DragOverlay.Review
    }
