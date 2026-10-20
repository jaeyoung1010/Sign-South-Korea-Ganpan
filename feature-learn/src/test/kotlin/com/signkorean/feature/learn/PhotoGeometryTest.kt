package com.signkorean.feature.learn

import com.signkorean.domain.model.FocusRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoGeometryTest {
    @Test
    fun `fit foreground retains the complete image width at supported stage widths`() {
        listOf(360f, 390f, 480f).forEach { stageWidth ->
            val bounds = fitForegroundBounds(PhotoSize(stageWidth, 320f), PhotoSize(1600f, 900f))

            assertEquals(0f, bounds.left, .001f)
            assertEquals(stageWidth, bounds.width, .001f)
            assertTrue(bounds.top >= 0f)
            assertTrue(bounds.bottom <= 320f)
        }
    }

    @Test
    fun `transforms a percentage focus rectangle inside the fit foreground`() {
        val frame =
            focusFrameInFitForeground(
                focus = FocusRect(x = 40f, y = 18f, width = 28f, height = 12f),
                container = PhotoSize(width = 390f, height = 320f),
                image = PhotoSize(width = 1600f, height = 900f),
            )

        assertEquals(156f, frame.left, .001f)
        assertEquals(89.8f, frame.top, .001f)
        assertEquals(109.2f, frame.width, .001f)
        assertEquals(26.325f, frame.height, .001f)
    }

    @Test
    fun `focus frame stays inside the rendered image`() {
        val bounds = fitForegroundBounds(PhotoSize(360f, 260f), PhotoSize(900f, 1600f))
        val frame =
            focusFrameInFitForeground(
                focus = FocusRect(0f, 0f, 100f, 100f),
                container = PhotoSize(360f, 260f),
                image = PhotoSize(900f, 1600f),
            )

        assertEquals(bounds.left, frame.left, .001f)
        assertEquals(bounds.top, frame.top, .001f)
        assertEquals(bounds.width, frame.width, .001f)
        assertEquals(bounds.height, frame.height, .001f)
    }
}
