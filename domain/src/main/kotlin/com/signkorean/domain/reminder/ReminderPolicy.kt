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
        if (!enabled || dueCount == 0) return null

        val localNow = now.atZone(zoneId)
        var next = ZonedDateTime.of(localNow.toLocalDate(), localTime, zoneId)
        if (!next.toInstant().isAfter(now)) next = next.plusDays(1)
        return next.toInstant()
    }
}
