package com.signkorean.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.signkorean.app.di.DataAdapterModule
import com.signkorean.domain.model.LaunchDestination
import com.signkorean.domain.model.UserSettings
import com.signkorean.domain.port.input.ResolveLaunchDestination
import com.signkorean.domain.port.output.SessionRepository
import com.signkorean.domain.port.output.UserSettingsRepository
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

        assertEquals(LaunchDestination.LEARN, runBlocking { resolveLaunchDestination() })
    }
}

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DataAdapterModule::class],
)
object FakeDataAdapterModule {
    @Provides
    @Singleton
    fun provideUserSettingsRepository(): UserSettingsRepository = FakeUserSettingsRepository(UserSettings(onboardingCompleted = true))

    @Provides
    @Singleton
    fun provideSessionRepository(): SessionRepository = FakeSessionRepository(activeSession = true)
}
