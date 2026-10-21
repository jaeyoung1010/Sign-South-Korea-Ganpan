package com.signkorean.domain.usecase

import com.signkorean.domain.model.ProgressDay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Shared calendar aggregation used by Home and Progress so their charts cannot drift. */
object ProgressStatistics {
    fun lastSevenDays(
        timestamps: Collection<Instant>,
        now: Instant,
        zoneId: ZoneId,
    ): List<ProgressDay> {
        val today = now.atZone(zoneId).toLocalDate()
        return (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            ProgressDay(date, timestamps.count { it.atZone(zoneId).toLocalDate() == date })
        }
    }

    fun streak(
        timestamps: Collection<Instant>,
        now: Instant,
        zoneId: ZoneId,
    ): Int {
        val activityDays = timestamps.map { it.atZone(zoneId).toLocalDate() }.toSet()
        var date: LocalDate = now.atZone(zoneId).toLocalDate()
        var streak = 0
        while (date in activityDays) {
            streak += 1
            date = date.minusDays(1)
        }
        return streak
    }
}
