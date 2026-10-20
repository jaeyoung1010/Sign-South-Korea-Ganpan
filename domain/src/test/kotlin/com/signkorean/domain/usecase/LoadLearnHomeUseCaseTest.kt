package com.signkorean.domain.usecase

import com.signkorean.domain.model.ActiveSession
import com.signkorean.domain.model.BundleValidationReport
import com.signkorean.domain.model.FocusRect
import com.signkorean.domain.model.HomeActivitySnapshot
import com.signkorean.domain.model.SignCard
import com.signkorean.domain.model.UserSettings
import com.signkorean.domain.port.output.CardCatalog
import com.signkorean.domain.port.output.ClockPort
import com.signkorean.domain.port.output.HomeActivityRepository
import com.signkorean.domain.port.output.SessionRepository
import com.signkorean.domain.port.output.UserSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class LoadLearnHomeUseCaseTest {
    private val now = Instant.parse("2026-10-20T03:00:00Z")

    @Test
    fun `creates today's twenty sign session from settings and repository fixtures`() =
        runTest {
            val home = load(settings = UserSettings(sessionLength = 20))()

            assertEquals(20, home.session.length)
            assertEquals(8, home.session.estimatedMinutes)
            assertEquals(0, home.session.reviewCount)
            assertEquals(5, home.session.newCount)
            assertEquals(
                listOf("Landmarks" to 2, "Neighborhoods" to 1, "Markets" to 1, "Transport" to 1),
                home.categories.map { it.label to it.cardCount },
            )
        }

    @Test
    fun `honors five and ten sign settings and calculates review new mix from stored progress`() =
        runTest {
            val snapshot =
                HomeActivitySnapshot(
                    progressedCardIds = setOf("landmark-1", "transport-1"),
                    dueCardIds = setOf("landmark-1", "transport-1"),
                )

            val five = load(settings = UserSettings(sessionLength = 5), activity = snapshot)()
            val ten = load(settings = UserSettings(sessionLength = 10), activity = snapshot)()

            assertEquals(2, five.session.estimatedMinutes)
            assertEquals(2, five.session.reviewCount)
            assertEquals(3, five.session.newCount)
            assertEquals(4, ten.session.estimatedMinutes)
            assertEquals(2, ten.session.reviewCount)
            assertEquals(3, ten.session.newCount)
        }

    @Test
    fun `shows active session progress and derives streak and seven day activity from persisted timestamps`() =
        runTest {
            val activity =
                HomeActivitySnapshot(
                    progressedCardIds = setOf("landmark-1", "landmark-2", "market-1"),
                    reviewedAt =
                        listOf(
                            now,
                            now.minusSeconds(86_400),
                            now.minusSeconds(86_400),
                            now.minusSeconds(3 * 86_400),
                        ),
                )
            val home = load(activity = activity, activeSession = ActiveSession(completedCount = 4, totalCount = 10))()

            assertEquals(2, home.streakDays)
            assertEquals(listOf(0, 0, 0, 1, 0, 2, 1), home.weeklyActivity.map { it.count })
            assertEquals(ActiveSession(completedCount = 4, totalCount = 10), home.session.activeSession)
        }

    private fun load(
        settings: UserSettings = UserSettings(),
        activity: HomeActivitySnapshot = HomeActivitySnapshot(),
        activeSession: ActiveSession? = null,
    ): LoadLearnHomeUseCase =
        LoadLearnHomeUseCase(
            userSettingsRepository = SettingsRepository(settings),
            cardCatalog = FixtureCatalog(),
            homeActivityRepository = HomeActivityRepository { _ -> activity },
            sessionRepository =
                object : SessionRepository {
                    override suspend fun hasActiveSession(): Boolean = activeSession != null

                    override suspend fun getActiveSession(): ActiveSession? = activeSession
                },
            clock = FixedClock(now),
        )

    private class SettingsRepository(
        private val storedSettings: UserSettings,
    ) : UserSettingsRepository {
        override val settings: Flow<UserSettings> = flowOf(storedSettings)

        override suspend fun updateSettings(transform: (UserSettings) -> UserSettings): UserSettings = transform(storedSettings)
    }

    private class FixtureCatalog : CardCatalog {
        override fun getCards(): List<SignCard> =
            listOf(
                card("landmark-1", "LANDMARK"),
                card("landmark-2", "LANDMARK"),
                card("neighborhood-1", "NEIGHBORHOOD"),
                card("market-1", "MARKET"),
                card("transport-1", "TRANSPORT"),
            )

        override fun getCard(cardId: String): SignCard? = getCards().firstOrNull { it.id == cardId }

        override fun getLocalizedCopy(languageTag: String) = emptyList<com.signkorean.domain.model.LocalizedCardCopy>()

        override fun validateBundle() = BundleValidationReport(5, 5, 5, 5, setOf("en"))

        private fun card(
            id: String,
            category: String,
        ) = SignCard(
            id = id,
            order = getCardsCount.incrementAndGet(),
            korean = id,
            category = category,
            imageAssetUri = "asset://$id",
            focus = FocusRect(0f, 0f, 1f, 1f),
            audioAssetUris = emptyMap(),
            contentVersion = 1,
        )

        companion object {
            private var getCardsCount =
                java.util.concurrent.atomic
                    .AtomicInteger()
        }
    }

    private class FixedClock(
        private val instant: Instant,
    ) : ClockPort {
        override fun now(): Instant = instant

        override fun zoneId(): ZoneId = ZoneId.of("Asia/Seoul")
    }
}
