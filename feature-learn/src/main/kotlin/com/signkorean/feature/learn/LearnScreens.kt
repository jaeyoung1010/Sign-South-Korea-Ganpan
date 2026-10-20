@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.feature.learn

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.signkorean.designsystem.components.GameButton
import com.signkorean.designsystem.components.GameButtonStyle
import com.signkorean.designsystem.components.GameCard
import com.signkorean.designsystem.testing.SignKoreanTestTags
import com.signkorean.designsystem.theme.SignKoreanSpacing
import com.signkorean.domain.model.FocusRect
import com.signkorean.domain.model.LearnSessionError

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
)

object HomeTestTags {
    const val SESSION_ACTION = "home-session-action"
    const val SEE_ALL = "home-see-all"
    const val WEEK_CHART = "home-week-chart"
}

sealed interface LearnSessionUiState {
    data object Loading : LearnSessionUiState

    data class Active(
        val cardId: String,
        val sign: String,
        val position: Int,
        val totalCount: Int,
        val photo: LearnPhotoUiModel = LearnPhotoUiModel.placeholder(),
        val upcomingCards: List<LearnCardPreviewUiModel> = emptyList(),
        val stage: LearnCardStage = LearnCardStage.Question,
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
                questionContentDescription = "Street sign photo.",
                answerContentDescription = "Street sign photo.",
            )
    }
}

data class LearnCardPreviewUiModel(
    val cardId: String,
    val imageAssetUri: String,
)

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
    val title: String = "Great memory!",
    val summary: String = "You finished this sign deck.",
)

sealed interface LearnUiAction {
    data object StartSession : LearnUiAction

    data class StartCategorySession(
        val category: String,
    ) : LearnUiAction

    data object ConfirmCategoryReplacement : LearnUiAction

    data object CancelCategoryReplacement : LearnUiAction

    data object RevealAnswer : LearnUiAction

    data object ShowImageViewer : LearnUiAction

    data object ShowDeckSheet : LearnUiAction

    data object CloseSession : LearnUiAction

    data object ShowSessionMenu : LearnUiAction

    data object FinishSession : LearnUiAction

    data object OpenProgress : LearnUiAction

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
        Text(text = "Ready to read Korea?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(text = "A short street-reading session is waiting.", style = MaterialTheme.typography.bodyLarge)
        HomeSessionCard(state.session, state.isStarting, state.hasStartError, onAction)
        state.pendingCategoryReplacement?.let { category -> CategoryReplacementCard(category, state.isStarting, onAction) }
        CategorySection(state.categories, state.isStarting, onAction)
        WeekSection(state.week)
    }
}

@Composable
private fun HomeBrandBar(streakDays: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = "Sign Korean", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(text = "$streakDays days", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
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
        Text(text = "TODAY'S SESSION", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
        Text(
            text = if (session.isActive) "Pick up where you left off" else "${session.length} street signs",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
        )
        Text(
            text =
                session.completedCount?.let { "$it of ${session.length} signs complete" }
                    ?: "About ${session.estimatedMinutes} minutes",
            style = MaterialTheme.typography.bodyLarge,
        )
        if (!session.isActive) {
            Row(horizontalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small)) {
                Text(text = "${session.reviewCount} review", style = MaterialTheme.typography.labelLarge)
                Text(text = "${session.newCount} new", style = MaterialTheme.typography.labelLarge)
            }
        }
        GameButton(
            text =
                if (isStarting) {
                    "Starting session…"
                } else if (session.isActive) {
                    "Resume session"
                } else {
                    "Start session"
                },
            onClick = { onAction(LearnUiAction.StartSession) },
            modifier = Modifier.fillMaxWidth().testTag(HomeTestTags.SESSION_ACTION),
            enabled = !isStarting,
        )
        if (hasStartError) Text("Could not start the session. Please try again.", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun CategoryReplacementCard(
    category: String,
    isStarting: Boolean,
    onAction: (LearnUiAction) -> Unit,
) {
    GameCard(modifier = Modifier.fillMaxWidth()) {
        Text("Replace your current session?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text("Starting $category will replace the session you have in progress.", style = MaterialTheme.typography.bodyLarge)
        GameButton(
            text = if (isStarting) "Starting session…" else "Replace session",
            onClick = { onAction(LearnUiAction.ConfirmCategoryReplacement) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isStarting,
        )
        GameButton(
            text = "Keep current session",
            onClick = { onAction(LearnUiAction.CancelCategoryReplacement) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isStarting,
            style = GameButtonStyle.Secondary,
        )
    }
}

@Composable
private fun CategorySection(
    categories: List<HomeCategoryUiModel>,
    isStarting: Boolean,
    onAction: (LearnUiAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Explore by category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            GameButton(
                text = "See all",
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
                    text = "${category.label} · ${category.cardCount} signs",
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
private fun WeekSection(week: List<WeekActivityUiModel>) {
    Column(verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small)) {
        Text(text = "Your week", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        GameCard(modifier = Modifier.fillMaxWidth().testTag(HomeTestTags.WEEK_CHART)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.forEach { day ->
                    Column {
                        Text(text = day.count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Text(text = day.label, style = MaterialTheme.typography.labelSmall)
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
) {
    Column(
        modifier = modifier.fillMaxSize().testTag("screen-learn"),
        verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.medium),
    ) {
        LearnTopBar(onAction)
        when (state) {
            LearnSessionUiState.Loading -> LearnLoadingState()
            is LearnSessionUiState.Active -> ActiveLearnSession(state, onAction)
            LearnSessionUiState.Completed -> LearnCompletedState(onAction)
            is LearnSessionUiState.Error -> LearnErrorState(state.error, onAction)
        }
    }
}

@Composable
private fun LearnTopBar(onAction: (LearnUiAction) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        GameButton(
            text = "Close",
            onClick = { onAction(LearnUiAction.CloseSession) },
            modifier = Modifier.testTag(SignKoreanTestTags.LEARN_CLOSE_SESSION),
            style = GameButtonStyle.Secondary,
        )
        GameButton(
            text = "More",
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
) {
    Text(
        text = "${state.position} of ${state.totalCount}",
        modifier = Modifier.testTag(LearnSessionTestTags.POSITION),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Black,
    )
    LinearProgressIndicator(
        progress = { state.progressFraction },
        modifier = Modifier.fillMaxWidth().testTag(LearnSessionTestTags.PROGRESS),
    )
    LearnCardStack(
        state = state,
        modifier = Modifier.fillMaxWidth().weight(1f),
    )
    GameButton(
        text = if (state.stage == LearnCardStage.Question) "Show answer" else "Answer shown",
        onClick = { onAction(LearnUiAction.RevealAnswer) },
        modifier = Modifier.fillMaxWidth().testTag(SignKoreanTestTags.LEARN_REVEAL_ANSWER),
        enabled = state.stage == LearnCardStage.Question,
    )
    GameButton(
        text = "View sign photo",
        onClick = { onAction(LearnUiAction.ShowImageViewer) },
        modifier = Modifier.fillMaxWidth().testTag(SignKoreanTestTags.LEARN_ZOOM_IMAGE),
        style = GameButtonStyle.Secondary,
    )
}

@Composable
private fun LearnLoadingState() {
    GameCard(modifier = Modifier.fillMaxWidth()) {
        Text("Loading session…", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun LearnCompletedState(onAction: (LearnUiAction) -> Unit) {
    GameCard(modifier = Modifier.fillMaxWidth()) {
        Text("Session complete", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text("This session has no cards left to review.", style = MaterialTheme.typography.bodyLarge)
        GameButton("Back to learn", { onAction(LearnUiAction.CloseSession) }, Modifier.fillMaxWidth())
    }
}

@Composable
private fun LearnErrorState(
    error: LearnSessionError,
    onAction: (LearnUiAction) -> Unit,
) {
    GameCard(modifier = Modifier.fillMaxWidth()) {
        Text("This session card is unavailable", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(error.toDisplayMessage(), style = MaterialTheme.typography.bodyLarge)
        GameButton("Back to learn", { onAction(LearnUiAction.CloseSession) }, Modifier.fillMaxWidth())
    }
}

private fun LearnSessionError.toDisplayMessage(): String =
    when (this) {
        LearnSessionError.NoActiveSession -> "There is no active learning session."

        LearnSessionError.EmptySession -> "This saved session has no cards."

        LearnSessionError.InvalidCardCount,
        LearnSessionError.InvalidCardOrder,
        LearnSessionError.InvalidCurrentIndex,
        -> "This saved session needs to be started again."

        LearnSessionError.UnknownCard -> "This sign is no longer available."

        LearnSessionError.Unavailable -> "Your saved session could not be loaded."
    }

@Composable
fun CompletionScreen(
    state: CompletionUiState,
    onAction: (LearnUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    GameCard(modifier = modifier.testTag("screen-done")) {
        Text(text = state.title, style = MaterialTheme.typography.headlineMedium)
        Text(text = state.summary, style = MaterialTheme.typography.bodyLarge)
        GameButton("See progress", { onAction(LearnUiAction.OpenProgress) }, Modifier.fillMaxWidth())
    }
}
