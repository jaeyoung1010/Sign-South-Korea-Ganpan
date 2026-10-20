package com.signkorean.domain.usecase

import com.signkorean.domain.model.ActiveLearnSession
import com.signkorean.domain.model.BundleValidationReport
import com.signkorean.domain.model.FocusRect
import com.signkorean.domain.model.LearnSessionCard
import com.signkorean.domain.model.LearnSessionError
import com.signkorean.domain.model.LoadLearnSessionResult
import com.signkorean.domain.model.LocalizedCardCopy
import com.signkorean.domain.model.SignCard
import com.signkorean.domain.port.output.CardCatalog
import com.signkorean.domain.port.output.SessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadLearnSessionUseCaseTest {
    @Test
    fun `reports one based position and fraction for supported fixture lengths`() =
        runTest {
            listOf(
                Triple(5, 0, .2f),
                Triple(10, 4, .5f),
                Triple(20, 19, 1f),
            ).forEach { (total, currentIndex, expectedProgress) ->
                val result = load(session(total = total, currentIndex = currentIndex))()

                val active = assertActive(result)
                assertEquals(currentIndex + 1, active.position)
                assertEquals(total, active.totalCount)
                assertEquals(expectedProgress, active.progressFraction)
                assertEquals("card-$currentIndex", active.card.id)
            }
        }

    @Test
    fun `does not request a card when the active index is complete`() =
        runTest {
            val result = load(session(total = 5, currentIndex = 5))()

            assertEquals(LoadLearnSessionResult.Completed, result)
        }

    @Test
    fun `returns a safe error for an empty or malformed session snapshot`() =
        runTest {
            val empty = load(session(total = 0, currentIndex = 0, cards = emptyList()))()
            val malformed =
                load(
                    session(
                        total = 5,
                        currentIndex = 0,
                        cards = (0 until 5).map { LearnSessionCard(position = it + 1, cardId = "card-$it") },
                    ),
                )()

            assertEquals(LoadLearnSessionResult.Invalid(LearnSessionError.EmptySession), empty)
            assertEquals(LoadLearnSessionResult.Invalid(LearnSessionError.InvalidCardOrder), malformed)
        }

    @Test
    fun `returns a safe error for an invalid active card id`() =
        runTest {
            val result = load(session(total = 5, currentIndex = 2, invalidCardAt = 2))()

            assertEquals(LoadLearnSessionResult.Invalid(LearnSessionError.UnknownCard), result)
        }

    @Test
    fun `a later corrupt card does not prevent the current valid card from loading`() =
        runTest {
            val result = load(session(total = 5, currentIndex = 0, invalidCardAt = 3))()

            val active = assertActive(result)
            assertEquals("card-0", active.card.id)
            assertEquals(1, active.position)
            assertEquals(.2f, active.progressFraction)
        }

    private fun load(snapshot: ActiveLearnSession): LoadLearnSessionUseCase =
        LoadLearnSessionUseCase(
            sessionRepository =
                object : SessionRepository {
                    override suspend fun hasActiveSession(): Boolean = true

                    override suspend fun getActiveLearnSession(): ActiveLearnSession = snapshot
                },
            cardCatalog = FixtureCatalog,
        )

    private fun session(
        total: Int,
        currentIndex: Int,
        invalidCardAt: Int? = null,
        cards: List<LearnSessionCard> =
            (0 until total).map { index ->
                LearnSessionCard(
                    position = index,
                    cardId = if (index == invalidCardAt) "missing-$index" else "card-$index",
                )
            },
    ) = ActiveLearnSession(
        sessionId = "session-$total-$currentIndex",
        currentIndex = currentIndex,
        requestedLength = total,
        cards = cards,
    )

    private fun assertActive(result: LoadLearnSessionResult): LoadLearnSessionResult.Active {
        assertTrue(result is LoadLearnSessionResult.Active)
        return result as LoadLearnSessionResult.Active
    }

    private object FixtureCatalog : CardCatalog {
        override fun getCards(): List<SignCard> = (0 until 20).map(::card)

        override fun getCard(cardId: String): SignCard? = getCards().firstOrNull { it.id == cardId }

        override fun getLocalizedCopy(languageTag: String): List<LocalizedCardCopy> = emptyList()

        override fun validateBundle(): BundleValidationReport = BundleValidationReport(20, 20, 0, 0, emptySet())

        private fun card(index: Int) =
            SignCard(
                id = "card-$index",
                order = index,
                korean = "표지판 $index",
                category = "MARKET",
                imageAssetUri = "asset://card-$index",
                focus = FocusRect(0f, 0f, 1f, 1f),
                audioAssetUris = emptyMap(),
                contentVersion = 1,
            )
    }
}
