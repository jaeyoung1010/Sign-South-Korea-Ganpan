package com.signkorean.domain.port.output

import java.time.Instant
import java.time.ZoneId

interface ClockPort {
    fun now(): Instant

    fun zoneId(): ZoneId
}
