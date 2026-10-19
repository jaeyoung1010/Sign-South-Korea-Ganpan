package com.signkorean.data.time

import com.signkorean.domain.port.output.ClockPort
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class SystemClockAdapter(
    private val clock: Clock = Clock.systemDefaultZone(),
) : ClockPort {
    override fun now(): Instant = clock.instant()

    override fun zoneId(): ZoneId = clock.zone
}
