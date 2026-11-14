package com.signkorean.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.signkorean.app.di.DataAdapterModule
import com.signkorean.app.di.SessionAdapterModule
import com.signkorean.domain.model.AppStartupEvent
import com.signkorean.domain.model.BundleValidationReport
import com.signkorean.domain.model.ContentMigrationResult
import com.signkorean.domain.model.FocusRect
import com.signkorean.domain.model.LaunchDestination
import com.signkorean.domain.model.SessionToCreate
import com.signkorean.domain.model.SignCard
import com.signkorean.domain.model.UserSettings
import com.signkorean.domain.port.input.ResolveLaunchDestination
import com.signkorean.domain.port.output.AnalyticsSink
import com.signkorean.domain.port.output.CardCatalog
import com.signkorean.domain.port.output.ClockPort
import com.signkorean.domain.port.output.ContentMigrationRepository
import com.signkorean.domain.port.output.HomeActivityRepository
import com.signkorean.domain.port.output.SessionCreationRepository
import com.signkorean.domain.port.output.SessionIdGenerator
import com.signkorean.domain.port.output.SessionRepository
import com.signkorean.domain.port.output.UserSettingsRepository
import com.signkorean.testsupport.FakeClock
import com.signkorean.testsupport.FakeSessionRepository
import com.signkorean.testsupport.FakeUserSettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import javax.inject.Singleton

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HiltBindingReplacementTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var resolveLaunchDestination: ResolveLaunchDestination

    @Test
    fun testBindingReplacesProductionAdapters() {
        hiltRule.inject()

        assertEquals(LaunchDestination.HOME, runBlocking { resolveLaunchDestination() })
    }
}

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DataAdapterModule::class, SessionAdapterModule::class],
)
object FakeDataAdapterModule {
    @Provides
    @Singleton
    fun provideUserSettingsRepository(): UserSettingsRepository = FakeUserSettingsRepository(UserSettings(onboardingCompleted = true))

    @Provides
    @Singleton
    fun provideSessionRepository(): SessionRepository = FakeSessionRepository(activeSession = true)

    @Provides
    @Singleton
    fun provideSessionCreationRepository(): SessionCreationRepository =
        object : SessionCreationRepository {
            override suspend fun save(
                session: SessionToCreate,
                replaceActiveSession: Boolean,
            ) = Unit
        }

    @Provides
    @Singleton
    fun provideSessionIdGenerator(): SessionIdGenerator = SessionIdGenerator { "test-session" }

    @Provides
    @Singleton
    fun provideHomeActivityRepository(): HomeActivityRepository =
        HomeActivityRepository {
            com.signkorean.domain.model
                .HomeActivitySnapshot()
        }

    @Provides
    @Singleton
    fun provideCardCatalog(): CardCatalog =
        object : CardCatalog {
            override fun getCards(): List<SignCard> =
                listOf(
                    SignCard(
                        id = "test-card",
                        order = 1,
                        korean = "표지",
                        category = "PLACE",
                        imageAssetUri = "asset://image",
                        focus = FocusRect(0f, 0f, 1f, 1f),
                        audioAssetUris = emptyMap(),
                        contentVersion = 1,
                    ),
                )

            override fun getCard(cardId: String): SignCard? = getCards().singleOrNull { it.id == cardId }

            override fun getLocalizedCopy(languageTag: String) = emptyList<com.signkorean.domain.model.LocalizedCardCopy>()

            override fun validateBundle() = BundleValidationReport(1, 1, 1, setOf("en"))
        }

    @Provides
    @Singleton
    fun provideContentMigrationRepository(): ContentMigrationRepository =
        ContentMigrationRepository { ContentMigrationResult(migrated = false, activeSessionInvalidated = false) }

    @Provides
    @Singleton
    fun provideAnalyticsSink(): AnalyticsSink = AnalyticsSink { _: AppStartupEvent -> Unit }

    @Provides
    @Singleton
    fun provideClockPort(): ClockPort = FakeClock()
}
