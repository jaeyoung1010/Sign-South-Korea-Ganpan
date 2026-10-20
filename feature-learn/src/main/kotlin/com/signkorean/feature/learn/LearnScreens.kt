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

data class LearnHomeUiState(
    val streakDays: Int = 0,
    val session: HomeSessionUiModel = HomeSessionUiModel(),
    val categories: List<HomeCategoryUiModel> = emptyList(),
    val week: List<WeekActivityUiModel> = emptyList(),
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

data class LearnSessionUiState(
    val sign: String = "주차 금지",
    val translation: String = "No parking",
    val isAnswerVisible: Boolean = false,
)

data class CompletionUiState(
    val title: String = "Great memory!",
    val summary: String = "You finished this sign deck.",
)

sealed interface LearnUiAction {
    data object StartSession : LearnUiAction

    data object RevealAnswer : LearnUiAction

    data object ShowImageViewer : LearnUiAction

    data object ShowDeckSheet : LearnUiAction

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
        HomeSessionCard(state.session, onAction)
        CategorySection(state.categories, onAction)
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
            text = if (session.isActive) "Resume session" else "Start session",
            onClick = { onAction(LearnUiAction.StartSession) },
            modifier = Modifier.fillMaxWidth().testTag(HomeTestTags.SESSION_ACTION),
        )
    }
}

@Composable
private fun CategorySection(
    categories: List<HomeCategoryUiModel>,
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
                    onClick = { onAction(LearnUiAction.ShowDeckSheet) },
                    modifier = Modifier.width(176.dp),
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
    GameCard(modifier = modifier.testTag("screen-learn")) {
        Text(text = "Sign deck", style = MaterialTheme.typography.titleMedium)
        GameCard(modifier = Modifier.testTag(SignKoreanTestTags.LEARN_CARD_STACK)) {
            Text(text = state.sign, style = MaterialTheme.typography.displayMedium)
            if (state.isAnswerVisible) {
                Text(text = state.translation, style = MaterialTheme.typography.titleLarge)
            }
        }
        GameButton(
            text = if (state.isAnswerVisible) "Show photo" else "Show answer",
            onClick = { onAction(LearnUiAction.RevealAnswer) },
            modifier = Modifier.fillMaxWidth().testTag(SignKoreanTestTags.LEARN_REVEAL_ANSWER),
        )
        GameButton(
            text = "View sign photo",
            onClick = { onAction(LearnUiAction.ShowImageViewer) },
            modifier = Modifier.fillMaxWidth().testTag(SignKoreanTestTags.LEARN_ZOOM_IMAGE),
            style = GameButtonStyle.Secondary,
        )
        GameButton(
            text = "Finish practice",
            onClick = { onAction(LearnUiAction.FinishSession) },
            modifier = Modifier.fillMaxWidth().testTag(SignKoreanTestTags.LEARN_CLOSE_SESSION),
            style = GameButtonStyle.GotIt,
        )
    }
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
