package com.signkorean.domain.performance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PerformanceBudgetTest {
    private val budget = PerformanceBudget()

    @Test
    fun `accepts measurements at the release thresholds`() {
        val violations =
            budget.evaluate(
                PerformanceMeasurement(
                    coldStartMillis = 2_000,
                    inputToFrameMillis = 100,
                    audioStartMillis = 300,
                    frameDurationsMillis = listOf(22.22, 16.67, 16.67),
                    peakDecodedImageCount = 2,
                ),
            )

        assertEquals(emptySet<PerformanceBudgetViolation>(), violations)
    }

    @Test
    fun `reports each exceeded release budget without exposing raw measurements`() {
        val violations =
            budget.evaluate(
                PerformanceMeasurement(
                    coldStartMillis = 2_001,
                    inputToFrameMillis = 101,
                    audioStartMillis = 301,
                    frameDurationsMillis = listOf(22.23, 23.0, 30.0),
                    peakDecodedImageCount = 3,
                ),
            )

        assertEquals(
            setOf(
                PerformanceBudgetViolation.ColdStart,
                PerformanceBudgetViolation.InputResponse,
                PerformanceBudgetViolation.AudioStart,
                PerformanceBudgetViolation.FramePacing,
                PerformanceBudgetViolation.ImageMemory,
            ),
            violations,
        )
        assertTrue(violations.all { it.userMessage == null })
    }
}
