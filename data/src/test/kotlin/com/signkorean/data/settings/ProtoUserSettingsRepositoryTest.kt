package com.signkorean.data.settings

import androidx.datastore.core.DataStoreFactory
import com.signkorean.data.settings.proto.UserSettingsProto
import com.signkorean.domain.model.AudioVoice
import com.signkorean.domain.model.Interest
import com.signkorean.domain.model.LearningLevel
import com.signkorean.domain.model.UserSettings
import com.signkorean.domain.usecase.CompleteOnboardingUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalTime

class ProtoUserSettingsRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() =
        runTest {
            scopes.forEach { scope ->
                scope.coroutineContext[Job]?.cancelAndJoin()
            }
        }

    @Test
    fun `defaults use system motion with the documented settings`() =
        runTest {
            val fixture = createFixture(systemReduceMotion = true)

            assertEquals(UserSettings(reduceMotion = true), fixture.repository.settings.first())
        }

    @Test
    fun `write returns persisted settings and emits the new value`() =
        runTest {
            val fixture = createFixture()
            fixture.repository.settings.first()
            val expected =
                UserSettings(
                    onboardingCompleted = true,
                    level = LearningLevel.ELEMENTARY,
                    interests = setOf(Interest.MARKET, Interest.TRANSPORT),
                    sessionLength = 10,
                    appLanguageTag = "ja",
                    learningExplanationLanguageTag = "ja",
                    showRomanization = true,
                    autoPlayAudio = true,
                    audioVoice = AudioVoice.GEMINI_KORE,
                    reduceMotion = true,
                    followSystemReduceMotion = false,
                    reminderEnabled = true,
                    reminderLocalTime = LocalTime.of(19, 0),
                )
            val emission =
                async {
                    fixture.repository.settings
                        .drop(1)
                        .first()
                }

            assertEquals(expected, fixture.repository.updateSettings { expected })
            assertEquals(expected, withTimeout(5_000) { emission.await() })
        }

    @Test
    fun `onboarding choices persist through the actual DataStore adapter`() =
        runTest {
            val fixture = createFixture()

            CompleteOnboardingUseCase(fixture.repository, NoopAnalyticsSink)(
                level = LearningLevel.NOT_SURE,
                interests = setOf(Interest.FOOD, Interest.SHOPPING),
            )

            assertEquals(
                UserSettings(
                    onboardingCompleted = true,
                    level = LearningLevel.NOT_SURE,
                    interests = setOf(Interest.FOOD, Interest.SHOPPING),
                ),
                fixture.repository.settings.first(),
            )
        }

    @Test
    fun `settings survive process recreation`() =
        runTest {
            val file = File(temporaryFolder.root, "persisted-settings.pb")
            val expected = UserSettings(onboardingCompleted = true, sessionLength = 5)
            val first = createFixture(file = file)
            first.repository.updateSettings { expected }
            close(first.scope)

            val reopened = createFixture(file = file)

            assertEquals(expected, reopened.repository.settings.first())
        }

    @Test
    fun `unrelated writes preserve the system motion preference`() =
        runTest {
            val file = File(temporaryFolder.root, "system-motion-settings.pb")
            val first = createFixture(file = file, systemReduceMotion = true)
            val updated =
                first.repository.settings
                    .first()
                    .copy(autoPlayAudio = true)
            first.repository.updateSettings { updated }
            close(first.scope)

            val reopened = createFixture(file = file, systemReduceMotion = false)
            val settings = reopened.repository.settings.first()

            assertEquals(false, settings.reduceMotion)
            assertTrue(settings.followSystemReduceMotion)
            assertTrue(settings.autoPlayAudio)
        }

    @Test
    fun `system reduce motion remains enabled when app preference is explicitly off`() =
        runTest {
            val fixture = createFixture(systemReduceMotion = true)

            fixture.repository.updateSettings {
                it.copy(reduceMotion = false, followSystemReduceMotion = false)
            }

            val whileSystemEnabled = fixture.repository.settings.first()
            assertTrue(whileSystemEnabled.reduceMotion)
            assertFalse(whileSystemEnabled.followSystemReduceMotion)
        }

    @Test
    fun `corrupt data is replaced with safe defaults and reports recovery`() =
        runTest {
            val file = File(temporaryFolder.root, "corrupted-settings.pb")
            file.writeBytes(byteArrayOf(0x80.toByte(), 0x80.toByte()))
            val diagnostics = RecordingSettingsDiagnostics()
            val fixture = createFixture(file = file, diagnostics = diagnostics)

            assertEquals(UserSettings(), fixture.repository.settings.first())
            assertEquals(1, diagnostics.recoveryCount)
        }

    @Test
    fun `unknown proto enums fall back without losing the rest of the settings`() =
        runTest {
            val fixture = createFixture()
            fixture.dataStore.updateData {
                it
                    .toBuilder()
                    .setOnboardingCompleted(true)
                    .setLevelValue(99)
                    .setVoiceValue(99)
                    .addInterestsValue(99)
                    .setSessionLength(99)
                    .setAppLanguageTag("ko")
                    .setLearningExplanationLanguageTag("ko")
                    .build()
            }

            val settings = fixture.repository.settings.first()

            assertTrue(settings.onboardingCompleted)
            assertEquals(LearningLevel.BEGINNER, settings.level)
            assertEquals(emptySet<Interest>(), settings.interests)
            assertEquals(AudioVoice.TYPECAST_EUNKYUNG, settings.audioVoice)
            assertEquals(20, settings.sessionLength)
            assertEquals("en", settings.appLanguageTag)
            assertEquals("en", settings.learningExplanationLanguageTag)
        }

    @Test
    fun `v0 data migrates to v1 while retaining recognized values`() =
        runTest {
            val file = File(temporaryFolder.root, "v0-settings.pb")
            val legacyScope = newScope()
            val legacyStore =
                DataStoreFactory.create(
                    serializer = UserSettingsProtoSerializer,
                    scope = legacyScope,
                    produceFile = { file },
                )
            legacyStore.updateData {
                UserSettingsProto
                    .newBuilder()
                    .setOnboardingCompleted(true)
                    .setSessionLength(10)
                    .setAppLanguageTag("ja")
                    .build()
            }
            close(legacyScope)

            val migrated = createFixture(file = file)

            assertEquals(
                true,
                migrated.repository.settings
                    .first()
                    .onboardingCompleted,
            )
            assertEquals(
                10,
                migrated.repository.settings
                    .first()
                    .sessionLength,
            )
            assertEquals(
                "ja",
                migrated.repository.settings
                    .first()
                    .appLanguageTag,
            )
            assertEquals(
                1,
                migrated.dataStore.data
                    .first()
                    .schemaVersion,
            )
        }

    private fun createFixture(
        file: File = File(temporaryFolder.root, "${System.nanoTime()}.pb"),
        systemReduceMotion: Boolean = false,
        diagnostics: RecordingSettingsDiagnostics = RecordingSettingsDiagnostics(),
    ): SettingsFixture {
        val scope = newScope()
        val dataStore = UserSettingsDataStoreFactory.create(file, diagnostics, scope)
        return SettingsFixture(
            repository = ProtoUserSettingsRepository(dataStore, SystemReduceMotionSource { systemReduceMotion }),
            dataStore = dataStore,
            scope = scope,
        )
    }

    private fun newScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also(scopes::add)

    private suspend fun close(scope: CoroutineScope) {
        scope.coroutineContext[Job]?.cancelAndJoin()
        scopes.remove(scope)
    }

    private data class SettingsFixture(
        val repository: ProtoUserSettingsRepository,
        val dataStore: androidx.datastore.core.DataStore<UserSettingsProto>,
        val scope: CoroutineScope,
    )

    private class RecordingSettingsDiagnostics : SettingsDiagnostics {
        var recoveryCount = 0

        override fun onCorruptionRecovered() {
            recoveryCount += 1
        }
    }

    private object NoopAnalyticsSink : com.signkorean.domain.port.output.AnalyticsSink {
        override fun recordAppStarted(event: com.signkorean.domain.model.AppStartupEvent) = Unit
    }
}
