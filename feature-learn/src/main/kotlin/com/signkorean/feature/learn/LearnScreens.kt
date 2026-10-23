@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.feature.learn

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.signkorean.designsystem.components.GameButton
import com.signkorean.designsystem.components.GameButtonStyle
import com.signkorean.designsystem.components.GameCard
import com.signkorean.designsystem.components.GameIconButton
import com.signkorean.designsystem.testing.SignKoreanTestTags
import com.signkorean.designsystem.theme.SignKoreanSpacing
import com.signkorean.domain.gesture.RatingDecision
import com.signkorean.domain.model.AudioPlaybackState
import com.signkorean.domain.model.FocusRect
import com.signkorean.domain.model.LearnSessionError
import com.signkorean.domain.model.RatingInputSource
import java.time.LocalDate
import java.time.format.TextStyle
import com.signkorean.domain.model.VoiceId

data class LearnHomeUiState(
    val streakDays: Int = 0,
    val session: HomeSessionUiModel = HomeSessionUiModel(),
    val categories: List<HomeCategoryUiModel> = emptyList(),
    val week: List<WeekActivityUiModel> = emptyList(),
    val isStarting: Boolean = false,
    val hasStartError: Boolean = false,
    val pendingCategoryReplacement: String? = null,
)

data class HomeSessionUiModel(
    val length: Int = 20,
    val estimatedMinutes: Int = 8,
    val reviewCount: Int = 0,
    val newCount: Int = 0,
    val completedCount: Int? = null,
    val sessionId: String = "",
    val isPaused: Boolean = false,
) {
    val isActive: Boolean
        get() = completedCount != null

    companion object {
        fun active(
            completedCount: Int,
            totalCount: Int,
        ): HomeSessionUiModel = HomeSessionUiModel(length = totalCount, completedCount = completedCount)
    }
}

data class HomeCategoryUiModel(
    val key: String,
    val label: String,
    val cardCount: Int,
)

data class WeekActivityUiModel(
    val label: String,
    val count: Int,
    val date: LocalDate? = null,
)

object HomeTestTags {
    const val SESSION_ACTION = "home-session-action"
    const val SEE_ALL = "home-see-all"
    const val WEEK_CHART = "home-week-chart"
}

sealed interface LearnSessionUiState {
    data object Loading : LearnSessionUiState

    data class Active(
        val sessionId: String = "",
        val cardId: String,
        val sign: String,
        val position: Int,
        val totalCount: Int,
        val photo: LearnPhotoUiModel = LearnPhotoUiModel.placeholder(),
        val answer: LearnAnswerUiModel = LearnAnswerUiModel.placeholder(),
        val audioAssetUris: Map<VoiceId, String> = emptyMap(),
        val upcomingCards: List<LearnCardPreviewUiModel> = emptyList(),
        val stage: LearnCardStage = LearnCardStage.Question,
        val isRating: Boolean = false,
        val ratingDecision: RatingDecision? = null,
        val ratingError: Boolean = false,
    ) : LearnSessionUiState {
        init {
            require(position in 1..totalCount) { "Position must be within the active session" }
        }

        val progressFraction: Float
            get() = position.toFloat() / totalCount
    }

    data object Completed : LearnSessionUiState

    data class Error(
        val error: LearnSessionError,
    ) : LearnSessionUiState
}

data class LearnPhotoUiModel(
    val imageAssetUri: String,
    val focus: FocusRect,
    val questionContentDescription: String,
    val answerContentDescription: String,
) {
    companion object {
        fun placeholder(): LearnPhotoUiModel =
            LearnPhotoUiModel(
                imageAssetUri = "",
                focus = FocusRect(0f, 0f, 100f, 100f),
                questionContentDescription = "",
                answerContentDescription = "",
            )
    }
}

data class LearnCardPreviewUiModel(
    val cardId: String,
    val imageAssetUri: String,
)

data class LearnWordPartUiModel(
    val korean: String,
    val meaning: String,
)

data class LearnAnswerUiModel(
    val displayMeaning: String,
    val pronunciationGuide: String,
    val wordParts: List<LearnWordPartUiModel>,
    val streetNote: String,
    val showRomanization: Boolean,
) {
    companion object {
        fun placeholder(): LearnAnswerUiModel =
            LearnAnswerUiModel(
                displayMeaning = "",
                pronunciationGuide = "",
                wordParts = emptyList(),
                streetNote = "",
                showRomanization = false,
            )
    }
}

data class LearnFeedbackUiState(
    val message: LearnFeedbackMessage,
    val canUndo: Boolean,
    val canRetry: Boolean = false,
)

enum class LearnFeedbackMessage {
    MarkedGotIt,
    MarkedReview,
    LastChoiceUndone,
    NothingToUndo,
    UndoFailed,
    SaveFailed,
}

enum class LearnCardStage {
    Question,
    Answer,
}

object LearnSessionTestTags {
    const val POSITION = "learn.session.position"
    const val PROGRESS = "learn.session.progress"
    const val MORE = "learn.session.more"
}

data class CompletionUiState(
    val title: String = "",
    val summary: String = "",
    val totalCount: Int = 0,
    val gotItCount: Int = 0,
    val reviewCount: Int = 0,
    val nextReviewLabel: String = "",
    val nextReviewDate: String? = null,
    val reviewCardIds: List<String> = emptyList(),
)

sealed interface LearnUiAction {
    data object StartSession : LearnUiAction

    data class StartCategorySession(
        val category: String,
    ) : LearnUiAction

    data object ConfirmCategoryReplacement : LearnUiAction

    data object CancelCategoryReplacement : LearnUiAction

    data object RevealAnswer : LearnUiAction

    data object PlayAudio : LearnUiAction

    data object CheckAnswerFirst : LearnUiAction

    data class RateCard(
        val decision: RatingDecision,
        val source: RatingInputSource = RatingInputSource.Gesture,
    ) : LearnUiAction

    data object UndoRating : LearnUiAction

    data object RetryRating : LearnUiAction

    data object RatingAnimationFinished : LearnUiAction

    data object ShowImageViewer : LearnUiAction

    data object ShowDeckSheet : LearnUiAction

    data object CloseSession : LearnUiAction

    data object ShowSessionMenu : LearnUiAction

    data object FinishSession : LearnUiAction

    data object OpenProgress : LearnUiAction

    data object OpenReviewCards : LearnUiAction

    data object BackHome : LearnUiAction

    data object OpenSettings : LearnUiAction
}

sealed interface LearnUiEffect {
    data object NavigateToSession : LearnUiEffect

    data object NavigateToCompletion : LearnUiEffect

    data object OpenFullScreenViewer : LearnUiEffect

    data object OpenDeckSheet : LearnUiEffect
}

@Composable
fun LearnHomeScreen(
    state: LearnHomeUiState,
    onAction: (LearnUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 88.dp)
                .testTag("screen-home"),
        verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.medium),
    ) {
        HomeBrandBar(state.streakDays)
        Text(text = stringResource(R.string.learn_ready_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(text = stringResource(R.string.learn_ready_description), style = MaterialTheme.typography.bodyLarge)
        HomeSessionCard(state.session, state.isStarting, state.hasStartError, onAction)
        state.pendingCategoryReplacement?.let { category -> CategoryReplacementCard(category, state.isStarting, onAction) }
        CategorySection(state.categories, state.isStarting, onAction)
        WeekSection(state.week)
    }
}

@Composable
private fun HomeBrandBar(streakDays: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = stringResource(R.string.learn_brand_name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(text = pluralStringResource(R.plurals.learn_streak_days, streakDays, streakDays), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun HomeSessionCard(
    session: HomeSessionUiModel,
    isStarting: Boolean,
    hasStartError: Boolean,
    onAction: (LearnUiAction) -> Unit,
) {
    GameCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(R.string.learn_today_session), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
        Text(
            text = if (session.isActive) stringResource(R.string.learn_pick_up) else pluralStringResource(R.plurals.learn_street_signs, session.length, session.length),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
        )
        Text(
            text =
                session.completedCount?.let { pluralStringResource(R.plurals.learn_signs_complete, session.length, it, session.length) }
                    ?: pluralStringResource(R.plurals.learn_about_minutes, session.estimatedMinutes, session.estimatedMinutes),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (!session.isActive) {
            Row(horizontalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small)) {
                Text(text = pluralStringResource(R.plurals.learn_review_count, session.reviewCount, session.reviewCount), style = MaterialTheme.typography.labelLarge)
                Text(text = pluralStringResource(R.plurals.learn_new_count, session.newCount, session.newCount), style = MaterialTheme.typography.labelLarge)
            }
        }
        GameButton(
            text =
                if (isStarting) {
                    stringResource(R.string.learn_starting_session)
                } else if (session.isActive) {
                    stringResource(R.string.learn_resume_session)
                } else {
                    stringResource(R.string.learn_start_session)
                },
            onClick = { onAction(LearnUiAction.StartSession) },
            modifier = Modifier.fillMaxWidth().testTag(HomeTestTags.SESSION_ACTION),
            enabled = !isStarting,
        )
        if (hasStartError) Text(stringResource(R.string.learn_start_error), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun CategoryReplacementCard(
    category: String,
    isStarting: Boolean,
    onAction: (LearnUiAction) -> Unit,
) {
    val categoryDisplayName = stringResource(category.labelResource())
    GameCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.learn_replace_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(stringResource(R.string.learn_replace_body, categoryDisplayName), style = MaterialTheme.typography.bodyLarge)
        GameButton(
            text = if (isStarting) stringResource(R.string.learn_starting_session) else stringResource(R.string.learn_replace_session),
            onClick = { onAction(LearnUiAction.ConfirmCategoryReplacement) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isStarting,
        )
        GameButton(
            text = stringResource(R.string.learn_keep_current_session),
            onClick = { onAction(LearnUiAction.CancelCategoryReplacement) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isStarting,
            style = GameButtonStyle.Secondary,
        )
    }
}

private fun String.labelResource(): Int =
    when (this) {
        "LANDMARK" -> R.string.learn_category_landmarks
        "NEIGHBORHOOD" -> R.string.learn_category_neighborhoods
        "MARKET" -> R.string.learn_category_markets
        "TRANSPORT" -> R.string.learn_category_transport
        else -> R.string.learn_category_landmarks
    }

@Composable
private fun CategorySection(
    categories: List<HomeCategoryUiModel>,
    isStarting: Boolean,
    onAction: (LearnUiAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(R.string.learn_explore_category), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            GameButton(
                text = stringResource(R.string.learn_see_all),
                onClick = { onAction(LearnUiAction.ShowDeckSheet) },
                modifier = Modifier.testTag(HomeTestTags.SEE_ALL),
                style = GameButtonStyle.Secondary,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            categories.forEach { category ->
                GameButton(
                    text = pluralStringResource(R.plurals.learn_category_signs, category.cardCount, category.localizedLabel(), category.cardCount),
                    onClick = { onAction(LearnUiAction.StartCategorySession(category.key)) },
                    modifier = Modifier.width(176.dp),
                    enabled = !isStarting,
                    style = GameButtonStyle.Secondary,
                )
            }
        }
    }
}

@Composable
private fun HomeCategoryUiModel.localizedLabel(): String =
    stringResource(
        when (key) {
            "LANDMARK" -> R.string.learn_category_landmarks
            "NEIGHBORHOOD" -> R.string.learn_category_neighborhoods
            "MARKET" -> R.string.learn_category_markets
            "TRANSPORT" -> R.string.learn_category_transport
            else -> R.string.learn_category_landmarks
        },
    )

@Composable
private fun WeekSection(week: List<WeekActivityUiModel>) {
    val locale = LocalConfiguration.current.locales[0]
    Column(verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small)) {
        Text(text = stringResource(R.string.learn_your_week), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        GameCard(modifier = Modifier.fillMaxWidth().testTag(HomeTestTags.WEEK_CHART)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.forEach { day ->
                    Column {
                        Text(text = day.count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Text(
                            text = day.date?.dayOfWeek?.getDisplayName(TextStyle.NARROW, locale) ?: day.label,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LearnSessionScreen(
    state: LearnSessionUiState,
    onAction: (LearnUiAction) -> Unit,
    modifier: Modifier = Modifier,
    feedback: LearnFeedbackUiState? = null,
    audioState: AudioPlaybackState = AudioPlaybackState.Idle,
    reduceMotion: Boolean = false,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val isCompact = maxHeight <= COMPACT_HEIGHT || LocalDensity.current.fontScale >= LARGE_FONT_SCALE
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(if (isCompact) Modifier.verticalScroll(rememberScrollState()) else Modifier.fillMaxSize())
                    .testTag("screen-learn"),
            verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.medium),
        ) {
            LearnTopBar(onAction)
            when (state) {
                LearnSessionUiState.Loading -> LearnLoadingState()
                is LearnSessionUiState.Active ->
                    ActiveLearnSession(
                        state,
                        onAction,
                        feedback,
                        audioState,
                        reduceMotion,
                        isCompact,
                    )

                LearnSessionUiState.Completed -> LearnCompletedState(onAction)
                is LearnSessionUiState.Error -> LearnErrorState(state.error, onAction)
            }
            if (state !is LearnSessionUiState.Active) {
                feedback?.let { LearnFeedbackHost(it, onAction) }
            }
        }
    }
}

@Composable
private fun LearnTopBar(onAction: (LearnUiAction) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        GameButton(
            text = stringResource(R.string.learn_close),
            onClick = { onAction(LearnUiAction.CloseSession) },
            modifier = Modifier.testTag(SignKoreanTestTags.LEARN_CLOSE_SESSION),
            style = GameButtonStyle.Secondary,
        )
        GameButton(
            text = stringResource(R.string.learn_more),
            onClick = { onAction(LearnUiAction.ShowSessionMenu) },
            modifier = Modifier.testTag(LearnSessionTestTags.MORE),
            style = GameButtonStyle.Secondary,
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.ActiveLearnSession(
    state: LearnSessionUiState.Active,
    onAction: (LearnUiAction) -> Unit,
    feedback: LearnFeedbackUiState?,
    audioState: AudioPlaybackState,
    reduceMotion: Boolean,
    isCompact: Boolean,
) {
    Text(
        text = stringResource(R.string.learn_position, state.position, state.totalCount),
        modifier = Modifier.testTag(LearnSessionTestTags.POSITION).semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Black,
    )
    val progressDescription = stringResource(R.string.learn_position, state.position, state.totalCount)
    LinearProgressIndicator(
        progress = { state.progressFraction },
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(LearnSessionTestTags.PROGRESS)
                .semantics { stateDescription = progressDescription },
    )
    LearnCardStack(
        state = state,
        onAction = onAction,
        audioState = audioState,
        reduceMotion = reduceMotion,
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (isCompact) Modifier.height(COMPACT_CARD_HEIGHT) else Modifier.weight(1f)),
    )
    feedback?.let { LearnFeedbackHost(it, onAction) }
    if (state.stage == LearnCardStage.Question) {
        GameButton(
            text = stringResource(R.string.learn_show_answer),
            onClick = { onAction(LearnUiAction.RevealAnswer) },
            modifier = Modifier.fillMaxWidth().testTag(SignKoreanTestTags.LEARN_REVEAL_ANSWER),
        )
    }
    GameButton(
        text = stringResource(R.string.learn_view_sign_photo),
        onClick = { onAction(LearnUiAction.ShowImageViewer) },
        modifier = Modifier.fillMaxWidth().testTag(SignKoreanTestTags.LEARN_ZOOM_IMAGE),
        style = GameButtonStyle.Secondary,
    )
}

@Composable
private fun LearnFeedbackHost(
    feedback: LearnFeedbackUiState,
    onAction: (LearnUiAction) -> Unit,
) {
    GameCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(SignKoreanTestTags.LEARN_RATING_TOAST)
                .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(feedback.message.toStringResource()),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Black,
            )
            if (feedback.canUndo) {
                GameButton(
                    text = stringResource(R.string.learn_undo),
                    onClick = { onAction(LearnUiAction.UndoRating) },
                    modifier = Modifier.testTag(SignKoreanTestTags.LEARN_UNDO_RATING),
                    style = GameButtonStyle.Secondary,
                )
            }
            if (feedback.canRetry) {
                GameButton(
                    text = stringResource(R.string.learn_retry),
                    onClick = { onAction(LearnUiAction.RetryRating) },
                    modifier = Modifier.testTag(SignKoreanTestTags.LEARN_RETRY_RATING),
                    style = GameButtonStyle.Secondary,
                )
            }
        }
    }
}

private fun LearnFeedbackMessage.toStringResource(): Int =
    when (this) {
        LearnFeedbackMessage.MarkedGotIt -> R.string.learn_feedback_marked_got_it
        LearnFeedbackMessage.MarkedReview -> R.string.learn_feedback_marked_review
        LearnFeedbackMessage.LastChoiceUndone -> R.string.learn_feedback_last_choice_undone
        LearnFeedbackMessage.NothingToUndo -> R.string.learn_feedback_nothing_to_undo
        LearnFeedbackMessage.UndoFailed -> R.string.learn_feedback_undo_failed
        LearnFeedbackMessage.SaveFailed -> R.string.learn_feedback_save_failed
    }

private val COMPACT_HEIGHT = 600.dp
private val COMPACT_CARD_HEIGHT = 360.dp
private const val LARGE_FONT_SCALE = 1.5f

@Composable
private fun LearnLoadingState() {
    GameCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.learn_loading_session), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun LearnCompletedState(onAction: (LearnUiAction) -> Unit) {
    GameCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.learn_session_complete), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(stringResource(R.string.learn_no_cards), style = MaterialTheme.typography.bodyLarge)
        GameButton(stringResource(R.string.learn_back_to_learn), { onAction(LearnUiAction.CloseSession) }, Modifier.fillMaxWidth())
    }
}

@Composable
private fun LearnErrorState(
    error: LearnSessionError,
    onAction: (LearnUiAction) -> Unit,
) {
    GameCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.learn_card_unavailable), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(stringResource(error.toStringResource()), style = MaterialTheme.typography.bodyLarge)
        GameButton(stringResource(R.string.learn_back_to_learn), { onAction(LearnUiAction.CloseSession) }, Modifier.fillMaxWidth())
    }
}

private fun LearnSessionError.toStringResource(): Int =
    when (this) {
        LearnSessionError.NoActiveSession -> R.string.learn_error_no_active

        LearnSessionError.EmptySession -> R.string.learn_error_empty

        LearnSessionError.InvalidCardCount,
        LearnSessionError.InvalidCardOrder,
        LearnSessionError.InvalidCurrentIndex,
        -> R.string.learn_error_restart

        LearnSessionError.UnknownCard -> R.string.learn_error_unknown_card

        LearnSessionError.Unavailable -> R.string.learn_error_unavailable
    }

@Composable
fun CompletionScreen(
    state: CompletionUiState,
    onAction: (LearnUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    GameCard(modifier = modifier.testTag("screen-done")) {
        Text(text = state.title.ifBlank { stringResource(R.string.learn_session_complete) }, style = MaterialTheme.typography.headlineMedium)
        Text(text = state.summary.ifBlank { stringResource(R.string.learn_completion_summary) }, style = MaterialTheme.typography.bodyLarge)
        Text(text = pluralStringResource(R.plurals.learn_signs_practiced, state.totalCount, state.totalCount), style = MaterialTheme.typography.bodyLarge)
        Text(text = stringResource(R.string.learn_got_it_count, state.gotItCount), style = MaterialTheme.typography.bodyLarge)
        Text(text = stringResource(R.string.learn_review_count_label, state.reviewCount), style = MaterialTheme.typography.bodyLarge)
        Text(
            text =
                state.nextReviewLabel.ifBlank {
                    state.nextReviewDate?.let { stringResource(R.string.learn_next_review, it) }
                        ?: stringResource(R.string.learn_no_further_review)
                },
            style = MaterialTheme.typography.bodyLarge,
        )
        if (state.reviewCount > 0) {
            GameButton(
                text = stringResource(R.string.learn_see_review_cards),
                onClick = { onAction(LearnUiAction.OpenReviewCards) },
                modifier = Modifier.fillMaxWidth().testTag(SignKoreanTestTags.DONE_REVIEW_CARDS),
            )
        }
        GameButton(
            text = stringResource(R.string.learn_back_home),
            onClick = { onAction(LearnUiAction.BackHome) },
            modifier = Modifier.fillMaxWidth().testTag(SignKoreanTestTags.DONE_BACK_HOME),
        )
        GameButton(
            text = stringResource(R.string.learn_see_progress),
            onClick = { onAction(LearnUiAction.OpenProgress) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
