package com.signkorean.testsupport

import com.signkorean.domain.port.output.ClockPort
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class FakeClock(
    private var currentTime: Instant = Instant.EPOCH,
    private var currentZoneId: ZoneId = ZoneId.of("UTC"),
) : ClockPort {
    override fun now(): Instant = currentTime

    override fun zoneId(): ZoneId = currentZoneId

    fun advanceBy(duration: Duration) {
        currentTime = currentTime.plus(duration)
    }

    fun set(
        now: Instant,
        zoneId: ZoneId = currentZoneId,
    ) {
        currentTime = now
        currentZoneId = zoneId
    }
}
