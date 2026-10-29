package com.signkorean.domain.reminder

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Pure scheduling rules shared by the Android adapter and JVM tests. */
object ReminderPolicy {
    fun nextTrigger(
        enabled: Boolean,
        dueCount: Int,
        localTime: LocalTime,
        now: Instant,
        zoneId: ZoneId,
    ): Instant? {
        require(dueCount >= 0) { "Due count must not be negative" }
        // Keep the daily work scheduled even when there is nothing due today. The worker
        // checks the current due count before posting and reschedules itself, so enabling a
        // reminder remains a stable user preference instead of silently turning itself off.
        if (!enabled) return null

        val localNow = now.atZone(zoneId)
        var next = ZonedDateTime.of(localNow.toLocalDate(), localTime, zoneId)
        if (!next.toInstant().isAfter(now)) next = next.plusDays(1)
        return next.toInstant()
    }
}
