package com.signkorean.domain.usecase

import com.signkorean.domain.model.BundleValidationReport
import com.signkorean.domain.model.FocusRect
import com.signkorean.domain.model.HomeActivitySnapshot
import com.signkorean.domain.model.Interest
import com.signkorean.domain.model.LearningLevel
import com.signkorean.domain.model.LocalizedCardCopy
import com.signkorean.domain.model.SessionCardSource
import com.signkorean.domain.model.SessionStartedEvent
import com.signkorean.domain.model.SessionToCreate
import com.signkorean.domain.model.SignCard
import com.signkorean.domain.model.UserSettings
import com.signkorean.domain.port.input.StartSession
import com.signkorean.domain.port.input.StartSessionRequest
import com.signkorean.domain.port.input.StartSessionResult
import com.signkorean.domain.port.output.AnalyticsSink
import com.signkorean.domain.port.output.CardCatalog
import com.signkorean.domain.port.output.ClockPort
import com.signkorean.domain.port.output.HomeActivityRepository
import com.signkorean.domain.port.output.SessionCreationRepository
import com.signkorean.domain.port.output.SessionIdGenerator
import com.signkorean.domain.port.output.SessionRepository
import com.signkorean.domain.port.output.UserSettingsRepository
import com.signkorean.domain.session.SessionBuilder
import com.signkorean.domain.session.SessionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class StartSessionUseCaseTest {
    @Test
    fun `persists the planned session cards before reporting navigation-ready success`() =
        runTest {
            val store = RecordingSessionStore()
            val analytics = RecordingAnalytics()
            val start = start(store = store, analytics = analytics)

            val result = start(StartSessionRequest.daily())

            assertEquals(StartSessionResult.Started("session-1"), result)
            assertEquals(1, store.saved.size)
            assertEquals("session-1", store.saved.single().id)
            assertEquals(
                5,
                store.saved
                    .single()
                    .cards.size,
            )
            assertEquals(
                listOf(
                    SessionCardSource.REVIEW,
                    SessionCardSource.REVIEW,
                    SessionCardSource.NEW,
                    SessionCardSource.NEW,
                    SessionCardSource.NEW,
                ),
                store.saved
                    .single()
                    .cards
                    .map { it.source },
            )
            assertEquals(false, store.replaceRequests.single())
            assertEquals(SessionType.DAILY, analytics.events.single().source)
            assertEquals(2, analytics.events.single().dueCount)
            assertEquals(3, analytics.events.single().newCount)
        }

    @Test
    fun `daily request resumes an existing active session without writing another one`() =
        runTest {
            val store = RecordingSessionStore()
            val start =
                start(
                    store = store,
                    activeSession =
                        com.signkorean.domain.model
                            .ActiveSession(2, 5),
                )

            val result = start(StartSessionRequest.daily())

            assertEquals(
                StartSessionResult.Resumed(
                    com.signkorean.domain.model
                        .ActiveSession(2, 5),
                ),
                result,
            )
            assertTrue(store.saved.isEmpty())
        }

    @Test
    fun `category request requires replacement confirmation before replacing an active session`() =
        runTest {
            val store = RecordingSessionStore()
            val start =
                start(
                    store = store,
                    activeSession =
                        com.signkorean.domain.model
                            .ActiveSession(1, 5),
                )

            val confirmation = start(StartSessionRequest.category("MARKET"))
            val started = start(StartSessionRequest.category("MARKET", replaceActiveSession = true))

            assertEquals(
                StartSessionResult.ReplacementRequired(
                    com.signkorean.domain.model
                        .ActiveSession(1, 5),
                ),
                confirmation,
            )
            assertEquals(StartSessionResult.Started("session-1"), started)
            assertEquals(listOf(true), store.replaceRequests)
            assertEquals(SessionType.CATEGORY, store.saved.single().source)
            assertEquals("MARKET", store.saved.single().category)
        }

    @Test
    fun `completed sessions do not block creating a new session`() =
        runTest {
            val store = RecordingSessionStore()
            val start = start(store = store, activeSession = null)

            val result = start(StartSessionRequest.daily())

            assertEquals(StartSessionResult.Started("session-1"), result)
            assertEquals(1, store.saved.size)
        }

    @Test
    fun `analytics failure does not turn a saved session into a start failure`() =
        runTest {
            val store = RecordingSessionStore()
            val start =
                start(
                    store = store,
                    analytics =
                        object : AnalyticsSink {
                            override fun recordAppStarted(event: com.signkorean.domain.model.AppStartupEvent) = Unit

                            override fun recordSessionStarted(event: SessionStartedEvent) = error("analytics unavailable")
                        },
                )

            val result = start(StartSessionRequest.daily())

            assertEquals(StartSessionResult.Started("session-1"), result)
            assertEquals(1, store.saved.size)
        }

    private fun start(
        store: RecordingSessionStore,
        analytics: AnalyticsSink = RecordingAnalytics(),
        activeSession: com.signkorean.domain.model.ActiveSession? = null,
    ): StartSession =
        StartSessionUseCase(
            userSettingsRepository = SettingsRepository(),
            cardCatalog = FixtureCatalog(),
            homeActivityRepository =
                HomeActivityRepository {
                    HomeActivitySnapshot(
                        progressedCardIds = setOf("card-0", "card-1"),
                        dueCardIds = setOf("card-0", "card-1"),
                    )
                },
            sessionRepository =
                object : SessionRepository {
                    override suspend fun hasActiveSession(): Boolean = activeSession != null

                    override suspend fun getActiveSession() = activeSession
                },
            sessionCreationRepository = store,
            sessionIdGenerator = SessionIdGenerator { "session-1" },
            sessionBuilder = SessionBuilder(),
            analyticsSink = analytics,
            clock = FixedClock(),
        )

    private class SettingsRepository : UserSettingsRepository {
        override val settings: Flow<UserSettings> =
            flowOf(
                UserSettings(
                    sessionLength = 5,
                    interests = setOf(Interest.MARKET),
                    level = LearningLevel.BEGINNER,
                ),
            )

        override suspend fun updateSettings(transform: (UserSettings) -> UserSettings): UserSettings = transform(settingsValue)

        private val settingsValue = UserSettings(sessionLength = 5)
    }

    private class FixtureCatalog : CardCatalog {
        override fun getCards(): List<SignCard> = (0 until 6).map { index -> card(index) }

        override fun getCard(cardId: String): SignCard? = getCards().firstOrNull { it.id == cardId }

        override fun getLocalizedCopy(languageTag: String): List<LocalizedCardCopy> = emptyList()

        override fun validateBundle(): BundleValidationReport = BundleValidationReport(6, 6, 6, 6, setOf("en"))

        private fun card(index: Int) =
            SignCard(
                id = "card-$index",
                order = index,
                korean = "카드 $index",
                category = if (index < 3) "MARKET" else "TRANSPORT",
                imageAssetUri = "asset://card-$index",
                focus = FocusRect(0f, 0f, 1f, 1f),
                audioAssetUris = emptyMap(),
                contentVersion = 1,
            )
    }

    private class RecordingSessionStore : SessionCreationRepository {
        val saved = mutableListOf<SessionToCreate>()
        val replaceRequests = mutableListOf<Boolean>()

        override suspend fun save(
            session: SessionToCreate,
            replaceActiveSession: Boolean,
        ) {
            saved += session
            replaceRequests += replaceActiveSession
        }
    }

    private class RecordingAnalytics : AnalyticsSink {
        val events = mutableListOf<SessionStartedEvent>()

        override fun recordAppStarted(event: com.signkorean.domain.model.AppStartupEvent) = Unit

        override fun recordSessionStarted(event: SessionStartedEvent) {
            events += event
        }
    }

    private class FixedClock : ClockPort {
        override fun now(): Instant = Instant.parse("2026-10-20T00:00:00Z")

        override fun zoneId(): ZoneId = ZoneId.of("Asia/Seoul")
    }
}
