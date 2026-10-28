package com.signkorean.app.progress

import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.signkorean.domain.model.FocusRect
import com.signkorean.domain.model.LocalizedCardCopy
import com.signkorean.domain.model.LocalizedWordPart
import com.signkorean.domain.model.ProgressCategory
import com.signkorean.domain.model.ProgressDay
import com.signkorean.domain.model.ProgressReview
import com.signkorean.domain.model.ProgressSnapshot
import com.signkorean.domain.model.ReviewCardDetail
import com.signkorean.domain.model.SignCard
import com.signkorean.domain.model.VoiceId
import com.signkorean.domain.port.input.LoadReviewCardDetail
import com.signkorean.domain.port.input.ObserveProgress
import com.signkorean.domain.port.input.ObserveReviewCards
import com.signkorean.domain.port.input.PracticeCard
import com.signkorean.domain.port.input.PracticeCardRequest
import com.signkorean.domain.port.input.ReviewCardDetailRequest
import com.signkorean.domain.port.input.ReviewPageRequest
import com.signkorean.domain.port.input.StartSessionResult
import com.signkorean.domain.port.output.AppLocalePort
import com.signkorean.feature.progress.ProgressUiAction
import com.signkorean.feature.progress.ProgressUiEffect
import com.signkorean.testsupport.MainDispatcherRule
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `progress state maps stored snapshot and recent review detail`() =
        runTest {
            val viewModel = viewModel()

            advanceUntilIdle()

            assertEquals(2, viewModel.uiState.value.learnedCount)
            assertEquals(1, viewModel.uiState.value.masteredCount)
            assertEquals(1, viewModel.uiState.value.dueCount)
            assertEquals(listOf("홍대입구"), viewModel.uiState.value.recentReviews.map { it.korean })
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `uses the selected app language for weekday labels`() =
        runTest {
            val viewModel = viewModel(languageTag = "ja")

            advanceUntilIdle()

            assertEquals(listOf("火"), viewModel.uiState.value.week.map { it.label })
        }

    @Test
    fun `see all and detail preserve review list while practice navigates to learn`() =
        runTest {
            val viewModel = viewModel()

            viewModel.onAction(ProgressUiAction.OpenReviewList)
            advanceUntilIdle()
            viewModel.onAction(ProgressUiAction.SelectReview("card-1"))

            assertTrue(viewModel.uiState.value.isReviewListVisible)
            assertEquals("card-1", viewModel.uiState.value.selectedReview?.cardId)

            viewModel.onAction(ProgressUiAction.PracticeReview("card-1"))
            advanceUntilIdle()

            assertEquals(ProgressUiEffect.NavigateToLearn, viewModel.effects.first())
            assertFalse(viewModel.uiState.value.isPracticing)
        }

    private fun viewModel(languageTag: String = "en"): ProgressViewModel =
        ProgressViewModel(
            observeProgress = ObserveProgress { flowOf(snapshot) },
            observeReviewCards = ObserveReviewCards { _: ReviewPageRequest -> flowOf(listOf(review)) },
            loadReviewCardDetail =
                LoadReviewCardDetail { request: ReviewCardDetailRequest ->
                    detail.takeIf { it.card.id == request.cardId }
                },
            practiceCard =
                PracticeCard { request: PracticeCardRequest ->
                    assertEquals("card-1", request.cardId)
                    StartSessionResult.Started("practice-session")
                },
            appLocalePort = FakeAppLocalePort(languageTag),
        )

    private class FakeAppLocalePort(
        private val currentLanguageTag: String = "en",
    ) : AppLocalePort {
        override fun getSupported(): Set<String> = setOf("en", "ja")

        override fun getCurrent(): String = currentLanguageTag

        override fun setCurrent(languageTag: String) = Unit
    }

    private val review = ProgressReview("card-1", Instant.parse("2026-10-20T03:00:00Z"))
    private val snapshot =
        ProgressSnapshot(
            learnedCount = 2,
            masteredCount = 1,
            dueCount = 1,
            weeklyActivity = listOf(ProgressDay(LocalDate.parse("2026-10-20"), 1)),
            categoryProgress = listOf(ProgressCategory("NEIGHBORHOOD", 1, 2, 0.5f)),
            recentReviews = listOf(review),
        )
    private val detail =
        ReviewCardDetail(
            card =
                SignCard(
                    id = "card-1",
                    order = 1,
                    korean = "홍대입구",
                    category = "NEIGHBORHOOD",
                    imageAssetUri = "file:///android_asset/card-1.jpg",
                    focus = FocusRect(0f, 0f, 50f, 50f),
                    audioAssetUris = mapOf(VoiceId.TYPECAST_EUNKYUNG to "typecast.mp3"),
                    contentVersion = 1,
                ),
            copy =
                LocalizedCardCopy(
                    cardId = "card-1",
                    languageTag = "en",
                    displayMeaning = "Hongdae Entrance",
                    pronunciationGuide = "hongdae ipgu",
                    wordParts = listOf(LocalizedWordPart("홍대", "Hongdae")),
                    streetNote = "A lively neighborhood landmark.",
                    imageAltTextQuestion = "A street sign.",
                    imageAltTextAnswer = "A sign reading 홍대입구.",
                    searchTerms = listOf("Hongdae"),
                    contentVersion = 1,
                ),
        )
}
