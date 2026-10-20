package com.signkorean.domain.usecase

import com.signkorean.domain.model.AppContent
import com.signkorean.domain.model.AppInitializationResult
import com.signkorean.domain.model.AppStartupEvent
import com.signkorean.domain.model.ContentMigrationResult
import com.signkorean.domain.model.LaunchDestination
import com.signkorean.domain.model.UserSettings
import com.signkorean.domain.port.input.InitializeApp
import com.signkorean.domain.port.output.AnalyticsSink
import com.signkorean.domain.port.output.CardCatalog
import com.signkorean.domain.port.output.ContentMigrationRepository
import com.signkorean.domain.port.output.SessionRepository
import com.signkorean.domain.port.output.UserSettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

class InitializeAppUseCase(
    private val userSettingsRepository: UserSettingsRepository,
    private val sessionRepository: SessionRepository,
    private val cardCatalog: CardCatalog,
    private val contentMigrationRepository: ContentMigrationRepository,
    private val analyticsSink: AnalyticsSink,
) : InitializeApp {
    override suspend fun invoke(): AppInitializationResult =
        coroutineScope {
            val settings =
                async {
                    recoverResult { userSettingsRepository.settings.first() }
                }
            val database =
                async {
                    val content =
                        recoverResult { cardCatalog.getCards().toAppContent() }
                            .getOrElse { return@async StartupRead.ContentFailure }
                    recoverResult {
                        val migration = contentMigrationRepository.migrate(content)
                        val hasActiveSession = sessionRepository.hasActiveSession()
                        StartupRead.DatabaseSuccess(migration, hasActiveSession)
                    }.getOrElse { StartupRead.DatabaseFailure }
                }

            val settingsResult = settings.await()
            when (val databaseResult = database.await()) {
                StartupRead.ContentFailure -> {
                    AppInitializationResult.RecoverableFailure.Content
                }

                StartupRead.DatabaseFailure -> {
                    AppInitializationResult.RecoverableFailure.Database
                }

                is StartupRead.DatabaseSuccess -> {
                    val usedDefaultSettings = settingsResult.isFailure
                    val userSettings = settingsResult.getOrElse { UserSettings() }
                    val result =
                        AppInitializationResult.Ready(
                            destination =
                                if (userSettings.onboardingCompleted) LaunchDestination.HOME else LaunchDestination.ONBOARDING,
                            hasResumableSession = databaseResult.hasActiveSession,
                            usedDefaultSettings = usedDefaultSettings,
                        )
                    runCatching {
                        analyticsSink.recordAppStarted(
                            AppStartupEvent(
                                onboardingCompleted = userSettings.onboardingCompleted,
                                hasResumableSession = databaseResult.hasActiveSession,
                                usedDefaultSettings = usedDefaultSettings,
                                contentMigrated = databaseResult.migration.migrated,
                            ),
                        )
                    }
                    result
                }
            }
        }

    private fun List<com.signkorean.domain.model.SignCard>.toAppContent(): AppContent =
        AppContent(
            version = map(com.signkorean.domain.model.SignCard::contentVersion).distinct().single(),
            cardIds = map(com.signkorean.domain.model.SignCard::id).toSet(),
        )

    private suspend fun <T> recoverResult(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            Result.failure(failure)
        }

    private sealed interface StartupRead {
        data class DatabaseSuccess(
            val migration: ContentMigrationResult,
            val hasActiveSession: Boolean,
        ) : StartupRead

        data object ContentFailure : StartupRead

        data object DatabaseFailure : StartupRead
    }
}
