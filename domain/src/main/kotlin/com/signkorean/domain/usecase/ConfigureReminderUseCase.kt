package com.signkorean.domain.usecase

import com.signkorean.domain.port.input.ConfigureReminder
import com.signkorean.domain.port.input.ConfigureReminderResult
import com.signkorean.domain.port.output.ClockPort
import com.signkorean.domain.port.output.ReminderSchedule
import com.signkorean.domain.port.output.ReminderSchedulerPort
import com.signkorean.domain.port.output.UserSettingsRepository
import com.signkorean.domain.reminder.ReminderPolicy
import kotlinx.coroutines.flow.first

class ConfigureReminderUseCase(
    private val userSettingsRepository: UserSettingsRepository,
    private val reminderScheduler: ReminderSchedulerPort,
    private val clock: ClockPort,
) : ConfigureReminder {
    override suspend fun invoke(
        enabled: Boolean,
        dueCount: Int,
    ): ConfigureReminderResult {
        require(dueCount >= 0) { "Due count must not be negative" }
        val settings = userSettingsRepository.settings.first()
        if (!enabled) {
            userSettingsRepository.updateSettings { it.copy(reminderEnabled = false) }
            reminderScheduler.cancel()
            return ConfigureReminderResult.Disabled
        }

        val triggerAt =
            ReminderPolicy.nextTrigger(
                enabled = true,
                dueCount = dueCount,
                localTime = settings.reminderLocalTime,
                now = clock.now(),
                zoneId = clock.zoneId(),
            )
        userSettingsRepository.updateSettings { it.copy(reminderEnabled = triggerAt != null) }
        if (triggerAt == null) {
            reminderScheduler.cancel()
            return ConfigureReminderResult.NoDueCards
        }

        reminderScheduler.schedule(
            ReminderSchedule(
                triggerAt = triggerAt,
                localTime = settings.reminderLocalTime,
                zoneId = clock.zoneId(),
                appLanguageTag = settings.appLanguageTag,
            ),
        )
        return ConfigureReminderResult.Scheduled
    }
}
