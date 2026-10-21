package com.signkorean.domain.reminder

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderPolicyTest {
    private val zone = ZoneId.of("Asia/Seoul")
    private val now = Instant.parse("2026-10-21T08:00:00Z")

    @Test
    fun `off reminder never has a trigger`() {
        assertNull(ReminderPolicy.nextTrigger(false, 3, LocalTime.of(19, 0), now, zone))
    }

    @Test
    fun `zero due cards never has a trigger`() {
        assertNull(ReminderPolicy.nextTrigger(true, 0, LocalTime.of(19, 0), now, zone))
    }

    @Test
    fun `due cards trigger at local seven pm`() {
        val expected = ZonedDateTime.of(2026, 10, 21, 19, 0, 0, 0, zone).toInstant()
        assertEquals(expected, ReminderPolicy.nextTrigger(true, 1, LocalTime.of(19, 0), now, zone))
    }

    @Test
    fun `after seven pm trigger moves to the next local day`() {
        val afterSeven = ZonedDateTime.of(2026, 10, 21, 20, 0, 0, 0, zone).toInstant()
        val expected = ZonedDateTime.of(2026, 10, 22, 19, 0, 0, 0, zone).toInstant()
        assertEquals(expected, ReminderPolicy.nextTrigger(true, 1, LocalTime.of(19, 0), afterSeven, zone))
    }
}
