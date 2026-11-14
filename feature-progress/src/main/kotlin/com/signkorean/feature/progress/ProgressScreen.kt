@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.signkorean.feature.progress

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.signkorean.designsystem.components.GameButton
import com.signkorean.designsystem.components.GameButtonRow
import com.signkorean.designsystem.components.GameButtonStyle
import com.signkorean.designsystem.components.GameCard
import com.signkorean.designsystem.components.GameSheet
import com.signkorean.designsystem.theme.SignKoreanPalette
import com.signkorean.designsystem.theme.SignKoreanSpacing
import java.time.DayOfWeek
import java.util.Locale

data class ProgressUiState(
    val title: String = "Your progress",
    val summary: String = "See how your sign reading is growing.",
    val isLoading: Boolean = true,
    val learnedCount: Int = 0,
    val masteredCount: Int = 0,
    val dueCount: Int = 0,
    val week: List<ProgressDayUiModel> = emptyList(),
    val categories: List<ProgressCategoryUiModel> = emptyList(),
    val recentReviews: List<ProgressCardUiModel> = emptyList(),
    val allReviews: List<ProgressCardUiModel> = emptyList(),
    val isReviewListVisible: Boolean = false,
    val selectedReview: ProgressCardUiModel? = null,
    val isPracticing: Boolean = false,
    val errorMessage: String? = null,
)

data class ProgressDayUiModel(
    val label: String,
    val count: Int,
    val dayOfWeek: DayOfWeek? = null,
)

data class ProgressCategoryUiModel(
    val key: String,
    val label: String,
    val learnedCount: Int,
    val totalCount: Int,
    val ratio: Float,
)

data class ProgressCardUiModel(
    val cardId: String,
    val korean: String,
    val meaning: String,
    val pronunciation: String,
    val streetNote: String,
    val imageAssetUri: String,
    val reviewedAtLabel: String = "",
)

sealed interface ProgressUiAction {
    data object ReturnToLearn : ProgressUiAction

    data object OpenReviewList : ProgressUiAction

    data object CloseReviewList : ProgressUiAction

    data class SelectReview(
        val cardId: String,
    ) : ProgressUiAction

    data object CloseDetail : ProgressUiAction

    data class PracticeReview(
        val cardId: String,
    ) : ProgressUiAction

    data object Retry : ProgressUiAction
}

sealed interface ProgressUiEffect {
    data object NavigateToLearn : ProgressUiEffect
}

object ProgressTestTags {
    const val SCREEN = "screen-progress"
    const val STAT_LEARNED = "progress.stat.learned"
    const val STAT_MASTERED = "progress.stat.mastered"
    const val STAT_DUE = "progress.stat.due"
    const val WEEK_CHART = "progress.week-chart"
    const val CATEGORY_PROGRESS = "progress.category-progress"
    const val RECENT_REVIEWS = "progress.recent-reviews"
    const val SEE_ALL = "progress.see-all"
    const val REVIEW_LIST = "progress.review-list"
    const val REVIEW_DETAIL = "progress.review-detail"
    const val PRACTICE = "progress.practice"
    const val CLOSE = "progress.close"
    const val DETAIL_CLOSE = "progress.detail.close"
    const val EMPTY = "progress.empty"

    fun reviewCard(cardId: String): String = "progress.review.$cardId"
}

@Composable
fun ProgressScreen(
    state: ProgressUiState,
    onAction: (ProgressUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dashboardScroll = rememberScrollState()
    val reviewListState = rememberLazyListState()
    BackHandler(enabled = state.selectedReview != null) {
        onAction(ProgressUiAction.CloseDetail)
    }
    BackHandler(enabled = state.isReviewListVisible && state.selectedReview == null) {
        onAction(ProgressUiAction.CloseReviewList)
    }
    Box(modifier = modifier.fillMaxSize().testTag(ProgressTestTags.SCREEN)) {
        if (state.isReviewListVisible) {
            ReviewList(
                state = state,
                listState = reviewListState,
                onAction = onAction,
            )
        } else {
            ProgressDashboard(
                state = state,
                scrollState = dashboardScroll,
                onAction = onAction,
            )
        }
        state.selectedReview?.let { review ->
            ReviewDetailSheet(review = review, state = state, onAction = onAction)
        }
    }
}

@Composable
private fun ProgressDashboard(
    state: ProgressUiState,
    scrollState: androidx.compose.foundation.ScrollState,
    onAction: (ProgressUiAction) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.medium),
    ) {
        Text(text = stringResource(R.string.progress_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(text = stringResource(R.string.progress_summary), style = MaterialTheme.typography.bodyLarge)
        if (state.isLoading) {
            GameCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.progress_loading), style = MaterialTheme.typography.bodyLarge)
            }
        } else if (state.errorMessage != null) {
            GameCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = state.errorMessage, style = MaterialTheme.typography.bodyLarge)
                GameButton(
                    text = stringResource(R.string.progress_retry),
                    onClick = { onAction(ProgressUiAction.Retry) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else if (state.learnedCount == 0 && state.recentReviews.isEmpty()) {
            GameCard(modifier = Modifier.fillMaxWidth().testTag(ProgressTestTags.EMPTY)) {
                Text(
                    text = stringResource(R.string.progress_empty_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
                Text(text = stringResource(R.string.progress_empty_body), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            ProgressStats(state)
            WeekChart(state.week)
            CategoryProgress(state.categories)
            RecentReviews(state.recentReviews, onAction)
        }
    }
}

@Composable
private fun ProgressStats(state: ProgressUiState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small)) {
        ProgressStatCard(
            label = stringResource(R.string.progress_stat_learned),
            value = state.learnedCount,
            modifier = Modifier.weight(1f).testTag(ProgressTestTags.STAT_LEARNED),
        )
        ProgressStatCard(
            label = stringResource(R.string.progress_stat_mastered),
            value = state.masteredCount,
            modifier = Modifier.weight(1f).testTag(ProgressTestTags.STAT_MASTERED),
        )
        ProgressStatCard(
            label = stringResource(R.string.progress_stat_due),
            value = state.dueCount,
            modifier = Modifier.weight(1f).testTag(ProgressTestTags.STAT_DUE),
        )
    }
}

@Composable
private fun ProgressStatCard(
    label: String,
    value: Int,
    modifier: Modifier,
) {
    GameCard(modifier = modifier, contentPadding = SignKoreanSpacing.medium) {
        Text(text = value.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
        )
    }
}

@Composable
private fun WeekChart(week: List<ProgressDayUiModel>) {
    val maxCount = remember(week) { week.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1 }
    Column(verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small)) {
        Text(
            text = stringResource(R.string.progress_week_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
        )
        GameCard(modifier = Modifier.fillMaxWidth().testTag(ProgressTestTags.WEEK_CHART)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(112.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                week.forEach { day ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(text = day.count.toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                        Box(
                            modifier = Modifier.width(24.dp).height(72.dp),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            if (day.count > 0) {
                                Box(
                                    modifier =
                                        Modifier
                                            .width(24.dp)
                                            .height((72f * day.count / maxCount).coerceAtLeast(4f).dp)
                                            .clip(MaterialTheme.shapes.small)
                                            .background(Color(SignKoreanPalette.GOT_IT)),
                                )
                            }
                        }
                        Text(text = day.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryProgress(categories: List<ProgressCategoryUiModel>) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag(ProgressTestTags.CATEGORY_PROGRESS),
        verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small),
    ) {
        Text(
            text = stringResource(R.string.progress_category_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
        )
        categories.forEach { category ->
            GameCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = category.localizedLabel(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Black)
                    Text(text = "${category.learnedCount}/${category.totalCount}", style = MaterialTheme.typography.labelLarge)
                }
                LinearProgressIndicator(
                    progress = { category.ratio },
                    color = Color(SignKoreanPalette.GOT_IT),
                    trackColor = Color(SignKoreanPalette.SURFACE_SOFT),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ProgressCategoryUiModel.localizedLabel(): String =
    when (key) {
        "LANDMARK" -> stringResource(R.string.progress_category_landmarks)
        "NEIGHBORHOOD" -> stringResource(R.string.progress_category_neighborhoods)
        "MARKET" -> stringResource(R.string.progress_category_markets)
        "TRANSPORT" -> stringResource(R.string.progress_category_transport)
        else -> label
    }

@Composable
private fun RecentReviews(
    reviews: List<ProgressCardUiModel>,
    onAction: (ProgressUiAction) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag(ProgressTestTags.RECENT_REVIEWS),
        verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.progress_recent_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            GameButton(
                text = stringResource(R.string.progress_see_all),
                onClick = { onAction(ProgressUiAction.OpenReviewList) },
                modifier = Modifier.testTag(ProgressTestTags.SEE_ALL),
                style = GameButtonStyle.Secondary,
            )
        }
        reviews.forEach { review -> ReviewCard(review, onAction) }
    }
}

@Composable
private fun ReviewList(
    state: ProgressUiState,
    listState: LazyListState,
    onAction: (ProgressUiAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().testTag(ProgressTestTags.REVIEW_LIST)) {
        GameButtonRow {
            GameButton(
                text = stringResource(R.string.progress_close),
                onClick = { onAction(ProgressUiAction.CloseReviewList) },
                modifier = Modifier.testTag(ProgressTestTags.CLOSE),
                style = GameButtonStyle.Secondary,
            )
            Text(
                text = stringResource(R.string.progress_recent_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
            )
        }
        if (state.allReviews.isEmpty()) {
            GameCard(modifier = Modifier.fillMaxWidth().padding(top = SignKoreanSpacing.medium)) {
                Text(text = stringResource(R.string.progress_empty_reviews), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(top = SignKoreanSpacing.medium),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.small),
            ) {
                items(state.allReviews, key = { it.cardId }) { review -> ReviewCard(review, onAction) }
            }
        }
    }
}

@Composable
private fun ReviewCard(
    review: ProgressCardUiModel,
    onAction: (ProgressUiAction) -> Unit,
) {
    GameCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onAction(ProgressUiAction.SelectReview(review.cardId)) }
                .testTag(ProgressTestTags.reviewCard(review.cardId)),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(SignKoreanSpacing.medium), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = review.imageAssetUri,
                contentDescription = review.korean,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(72.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = review.korean, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(text = review.meaning, style = MaterialTheme.typography.bodyLarge)
                if (review.reviewedAtLabel.isNotBlank()) Text(text = review.reviewedAtLabel, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ReviewDetailSheet(
    review: ProgressCardUiModel,
    state: ProgressUiState,
    onAction: (ProgressUiAction) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(alpha = 0.32f),
                    ).clickable { onAction(ProgressUiAction.CloseDetail) },
        )
        GameSheet(title = review.korean, modifier = Modifier.testTag(ProgressTestTags.REVIEW_DETAIL)) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(SignKoreanSpacing.medium),
            ) {
                AsyncImage(
                    model = review.imageAssetUri,
                    contentDescription = review.korean,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 280.dp),
                )
                Text(text = review.meaning, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                if (review.pronunciation.isNotBlank()) Text(text = review.pronunciation, style = MaterialTheme.typography.bodyLarge)
                Text(text = review.streetNote, style = MaterialTheme.typography.bodyLarge)
                GameButton(
                    text = stringResource(R.string.progress_practice),
                    onClick = { onAction(ProgressUiAction.PracticeReview(review.cardId)) },
                    modifier = Modifier.fillMaxWidth().testTag(ProgressTestTags.PRACTICE),
                    enabled = !state.isPracticing,
                )
                GameButton(
                    text = stringResource(R.string.progress_close),
                    onClick = { onAction(ProgressUiAction.CloseDetail) },
                    modifier = Modifier.fillMaxWidth().testTag(ProgressTestTags.DETAIL_CLOSE),
                    style = GameButtonStyle.Secondary,
                )
                state.errorMessage?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
            }
        }
    }
}
