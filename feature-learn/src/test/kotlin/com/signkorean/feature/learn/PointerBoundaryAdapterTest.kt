package com.signkorean.feature.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PointerBoundaryAdapterTest {
    private val bounds = PointerBounds(leftPx = 0f, topPx = 0f, rightPx = 100f, bottomPx = 80f)

    @Test
    fun `clamps an out of bounds move and terminates the pointer`() {
        val adapter = PointerBoundaryAdapter(bounds)
        adapter.begin(PointerPoint(20f, 20f))

        val result = adapter.move(PointerPoint(140f, -10f))

        assertEquals(
            PointerBoundaryEvent.Ended(
                point = PointerPoint(100f, 0f),
                offsetXpx = 80f,
                offsetYpx = -20f,
                cancelled = true,
                outOfBounds = true,
            ),
            result,
        )
    }

    @Test
    fun `ignores subsequent moves after boundary termination`() {
        val adapter = PointerBoundaryAdapter(bounds)
        adapter.begin(PointerPoint(20f, 20f))
        adapter.move(PointerPoint(140f, 20f))

        assertEquals(PointerBoundaryEvent.Ignored, adapter.move(PointerPoint(30f, 30f)))
        assertEquals(PointerBoundaryEvent.Ignored, adapter.end())
    }

    @Test
    fun `cancel ends at the last in bounds point`() {
        val adapter = PointerBoundaryAdapter(bounds)
        adapter.begin(PointerPoint(20f, 20f))
        assertTrue(adapter.move(PointerPoint(50f, 40f)) is PointerBoundaryEvent.Moved)

        assertEquals(
            PointerBoundaryEvent.Ended(
                point = PointerPoint(50f, 40f),
                offsetXpx = 30f,
                offsetYpx = 20f,
                cancelled = true,
                outOfBounds = false,
            ),
            adapter.end(cancelled = true),
        )
    }

    @Test
    fun `begin clamps a point that starts outside the stage`() {
        val adapter = PointerBoundaryAdapter(bounds)

        adapter.begin(PointerPoint(-5f, 90f))

        assertEquals(
            PointerBoundaryEvent.Moved(
                point = PointerPoint(0f, 80f),
                offsetXpx = 0f,
                offsetYpx = 0f,
            ),
            adapter.move(PointerPoint(0f, 80f)),
        )
    }
}
