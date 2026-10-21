package com.signkorean.domain.usecase

import com.signkorean.domain.model.UserSettings
import com.signkorean.domain.port.input.ConfigureReminderResult
import com.signkorean.domain.port.output.ClockPort
import com.signkorean.domain.port.output.ReminderPermission
import com.signkorean.domain.port.output.ReminderSchedule
import com.signkorean.domain.port.output.ReminderSchedulerPort
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigureReminderUseCaseTest {
    private val clock =
        object : ClockPort {
            override fun now() = Instant.parse("2026-10-21T08:00:00Z")

            override fun zoneId() = ZoneId.of("Asia/Seoul")
        }

    @Test
    fun `one due card schedules and persists on`() =
        runTest {
            val repository = FakeUserSettingsRepository()
            val scheduler = RecordingReminderScheduler()
            val result = ConfigureReminderUseCase(repository, scheduler, clock)(true, 1)

            assertEquals(ConfigureReminderResult.Scheduled, result)
            assertTrue(repository.settings.first().reminderEnabled)
            assertEquals(1, scheduler.schedules.size)
        }

    @Test
    fun `zero due cards cancels and leaves reminder off`() =
        runTest {
            val repository = FakeUserSettingsRepository(UserSettings(reminderEnabled = false))
            val scheduler = RecordingReminderScheduler()
            val result = ConfigureReminderUseCase(repository, scheduler, clock)(true, 0)

            assertEquals(ConfigureReminderResult.NoDueCards, result)
            assertTrue(!repository.settings.first().reminderEnabled)
            assertEquals(1, scheduler.cancelCount)
        }

    @Test
    fun `turning reminder off cancels the existing work`() =
        runTest {
            val repository = FakeUserSettingsRepository(UserSettings(reminderEnabled = true))
            val scheduler = RecordingReminderScheduler()
            val result = ConfigureReminderUseCase(repository, scheduler, clock)(false, 4)

            assertEquals(ConfigureReminderResult.Disabled, result)
            assertTrue(!repository.settings.first().reminderEnabled)
            assertEquals(1, scheduler.cancelCount)
        }

    @Test
    fun `failed scheduling rolls the setting back off`() =
        runTest {
            val repository = FakeUserSettingsRepository()
            val scheduler = RecordingReminderScheduler(failOnSchedule = true)

            runCatching { ConfigureReminderUseCase(repository, scheduler, clock)(true, 1) }

            assertTrue(!repository.settings.first().reminderEnabled)
            assertEquals(1, scheduler.cancelCount)
        }

    private class RecordingReminderScheduler(
        private val failOnSchedule: Boolean = false,
    ) : ReminderSchedulerPort {
        val schedules = mutableListOf<ReminderSchedule>()
        var cancelCount = 0
        private val permission = MutableStateFlow(ReminderPermission.GRANTED)

        override fun observePermission(): Flow<ReminderPermission> = permission

        override suspend fun schedule(schedule: ReminderSchedule) {
            if (failOnSchedule) error("schedule failed")
            schedules += schedule
        }

        override suspend fun cancel() {
            cancelCount += 1
        }
    }

    private class FakeUserSettingsRepository(initial: UserSettings = UserSettings()) :
        com.signkorean.domain.port.output.UserSettingsRepository {
        private val state = MutableStateFlow(initial)
        private val mutex = Mutex()
        override val settings: Flow<UserSettings> = state.asStateFlow()

        override suspend fun updateSettings(transform: (UserSettings) -> UserSettings): UserSettings =
            mutex.withLock { transform(state.value).also(state::tryEmit) }
    }
}
