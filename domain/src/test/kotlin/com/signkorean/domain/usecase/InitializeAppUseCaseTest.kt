package com.signkorean.domain.usecase

import com.signkorean.domain.model.AppContent
import com.signkorean.domain.model.AppInitializationResult
import com.signkorean.domain.model.AppStartupEvent
import com.signkorean.domain.model.ContentMigrationResult
import com.signkorean.domain.model.LaunchDestination
import com.signkorean.domain.model.UserSettings
import com.signkorean.domain.port.output.AnalyticsSink
import com.signkorean.domain.port.output.CardCatalog
import com.signkorean.domain.port.output.ContentMigrationRepository
import com.signkorean.domain.port.output.SessionRepository
import com.signkorean.domain.port.output.UserSettingsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InitializeAppUseCaseTest {
    @Test
    fun `new installation opens onboarding and records a non identifying event`() =
        runTest {
            val analytics = RecordingAnalyticsSink()
            val result =
                initialize(
                    settings = SettingsRepository(UserSettings()),
                    analytics = analytics,
                )()

            assertEquals(
                AppInitializationResult.Ready(
                    destination = LaunchDestination.ONBOARDING,
                    hasResumableSession = false,
                    usedDefaultSettings = false,
                ),
                result,
            )
            assertEquals(
                listOf(
                    AppStartupEvent(
                        onboardingCompleted = false,
                        hasResumableSession = false,
                        usedDefaultSettings = false,
                        contentMigrated = false,
                    ),
                ),
                analytics.events,
            )
        }

    @Test
    fun `completed onboarding with active session opens home for a resume card`() =
        runTest {
            val result =
                initialize(
                    settings = SettingsRepository(UserSettings(onboardingCompleted = true)),
                    sessions = SessionRepository(activeSession = true),
                )()

            assertEquals(
                AppInitializationResult.Ready(
                    destination = LaunchDestination.HOME,
                    hasResumableSession = true,
                    usedDefaultSettings = false,
                ),
                result,
            )
        }

    @Test
    fun `settings read failure falls back to safe defaults`() =
        runTest {
            val result =
                initialize(
                    settings = SettingsRepository(error = IllegalStateException("settings unavailable")),
                )()

            assertEquals(
                AppInitializationResult.Ready(
                    destination = LaunchDestination.ONBOARDING,
                    hasResumableSession = false,
                    usedDefaultSettings = true,
                ),
                result,
            )
        }

    @Test
    fun `room failure is recoverable and does not emit app started`() =
        runTest {
            val analytics = RecordingAnalyticsSink()
            val result =
                initialize(
                    sessions = SessionRepository(error = IllegalStateException("database unavailable")),
                    analytics = analytics,
                )()

            assertEquals(AppInitializationResult.RecoverableFailure.Database, result)
            assertTrue(analytics.events.isEmpty())
        }

    @Test
    fun `content version change migrates by card id before reading active session`() =
        runTest {
            val migration = MigrationRepository(migrated = true)
            val analytics = RecordingAnalyticsSink()
            val result =
                initialize(
                    settings = SettingsRepository(UserSettings(onboardingCompleted = true)),
                    migration = migration,
                    analytics = analytics,
                )()

            assertTrue(result is AppInitializationResult.Ready)
            assertEquals(AppContent(version = 2, cardIds = setOf("known-card")), migration.receivedContent)
            assertTrue(analytics.events.single().contentMigrated)
        }

    @Test
    fun `settings and room work begin in parallel without an intermediate destination`() =
        runTest {
            val settings = SettingsRepository(UserSettings(onboardingCompleted = true), suspendRead = true)
            val sessions = SessionRepository(activeSession = false, suspendRead = true)
            val useCase = initialize(settings = settings, sessions = sessions)
            val result = async { useCase() }

            awaitAll(async { settings.awaitStarted() }, async { sessions.awaitStarted() })
            assertFalse(result.isCompleted)
            settings.release()
            sessions.release()

            assertEquals(
                AppInitializationResult.Ready(
                    destination = LaunchDestination.HOME,
                    hasResumableSession = false,
                    usedDefaultSettings = false,
                ),
                result.await(),
            )
        }

    private fun initialize(
        settings: SettingsRepository = SettingsRepository(UserSettings()),
        sessions: SessionRepository = SessionRepository(),
        migration: MigrationRepository = MigrationRepository(),
        analytics: RecordingAnalyticsSink = RecordingAnalyticsSink(),
    ): InitializeAppUseCase =
        InitializeAppUseCase(
            userSettingsRepository = settings,
            sessionRepository = sessions,
            cardCatalog = Catalog(),
            contentMigrationRepository = migration,
            analyticsSink = analytics,
        )

    private class SettingsRepository(
        private val storedSettings: UserSettings? = null,
        private val error: Throwable? = null,
        private val suspendRead: Boolean = false,
    ) : UserSettingsRepository {
        private val started = kotlinx.coroutines.CompletableDeferred<Unit>()
        private val release = kotlinx.coroutines.CompletableDeferred<Unit>()

        override val settings: Flow<UserSettings> =
            flow {
                started.complete(Unit)
                if (suspendRead) release.await()
                error?.let { throw it }
                emit(checkNotNull(storedSettings))
            }

        override suspend fun updateSettings(transform: (UserSettings) -> UserSettings): UserSettings =
            transform(checkNotNull(storedSettings))

        suspend fun awaitStarted() {
            started.await()
        }

        fun release() {
            release.complete(Unit)
        }
    }

    private class SessionRepository(
        private val activeSession: Boolean = false,
        private val error: Throwable? = null,
        private val suspendRead: Boolean = false,
    ) : com.signkorean.domain.port.output.SessionRepository {
        private val started = kotlinx.coroutines.CompletableDeferred<Unit>()
        private val release = kotlinx.coroutines.CompletableDeferred<Unit>()

        override suspend fun hasActiveSession(): Boolean {
            started.complete(Unit)
            if (suspendRead) release.await()
            error?.let { throw it }
            return activeSession
        }

        suspend fun awaitStarted() {
            started.await()
        }

        fun release() {
            release.complete(Unit)
        }
    }

    private class Catalog : CardCatalog {
        override fun getCards() =
            listOf(
                com.signkorean.domain.model.SignCard(
                    id = "known-card",
                    order = 1,
                    korean = "표지",
                    category = "PLACE",
                    imageAssetUri = "asset://image",
                    focus =
                        com.signkorean.domain.model
                            .FocusRect(0f, 0f, 1f, 1f),
                    audioAssetUris = emptyMap(),
                    contentVersion = 2,
                ),
            )

        override fun getCard(cardId: String) = getCards().singleOrNull { it.id == cardId }

        override fun getLocalizedCopy(languageTag: String) = emptyList<com.signkorean.domain.model.LocalizedCardCopy>()

        override fun validateBundle() =
            com.signkorean.domain.model
                .BundleValidationReport(1, 1, 1, 1, setOf("en"))
    }

    private class MigrationRepository(
        private val migrated: Boolean = false,
    ) : ContentMigrationRepository {
        var receivedContent: AppContent? = null

        override suspend fun migrate(content: AppContent): ContentMigrationResult {
            receivedContent = content
            return ContentMigrationResult(migrated = migrated, activeSessionInvalidated = false)
        }
    }

    private class RecordingAnalyticsSink : AnalyticsSink {
        val events = mutableListOf<AppStartupEvent>()

        override fun recordAppStarted(event: AppStartupEvent) {
            events += event
        }
    }
}
