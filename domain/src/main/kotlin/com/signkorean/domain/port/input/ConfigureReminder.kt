package com.signkorean.domain.port.input

sealed interface ConfigureReminderResult {
    data object Scheduled : ConfigureReminderResult

    data object NoDueCards : ConfigureReminderResult

    data object Disabled : ConfigureReminderResult
}

fun interface ConfigureReminder {
    suspend operator fun invoke(
        enabled: Boolean,
        dueCount: Int,
    ): ConfigureReminderResult
}
