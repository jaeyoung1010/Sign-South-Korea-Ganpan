package com.signkorean.domain.port.output

import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

enum class ReminderPermission {
    GRANTED,
    DENIED,
    NOT_REQUIRED,
}

data class ReminderSchedule(
    val triggerAt: Instant,
    val localTime: LocalTime,
    val zoneId: ZoneId,
    val appLanguageTag: String,
) {
    init {
        require(appLanguageTag.isNotBlank()) { "Language tag must not be blank" }
    }
}

interface ReminderSchedulerPort {
    fun observePermission(): Flow<ReminderPermission>

    suspend fun schedule(schedule: ReminderSchedule)

    suspend fun cancel()
}
