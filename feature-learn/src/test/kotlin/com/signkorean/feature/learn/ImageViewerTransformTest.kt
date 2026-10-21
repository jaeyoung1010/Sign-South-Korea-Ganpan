package com.signkorean.feature.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageViewerTransformTest {
    @Test
    fun zoom_is_clamped_to_the_viewer_bounds() {
        val transform = ImageViewerTransform()

        assertEquals(ImageViewerTransform.MAX_SCALE, transform.applyGesture(10f, 0f, 0f, 0f).scale, 0.001f)
        assertEquals(ImageViewerTransform.MIN_SCALE, transform.applyGesture(0.01f, 0f, 0f, 0f).scale, 0.001f)
    }

    @Test
    fun pan_and_rotation_are_accumulated_without_changing_the_card_state() {
        val transform =
            ImageViewerTransform().applyGesture(
                zoomChange = 2f,
                panX = 24f,
                panY = -8f,
                rotationDelta = 15f,
            )

        assertEquals(2f, transform.scale, 0.001f)
        assertEquals(24f, transform.offsetX, 0.001f)
        assertEquals(-8f, transform.offsetY, 0.001f)
        assertEquals(15f, transform.rotationDegrees, 0.001f)
    }

    @Test
    fun rotation_wraps_to_a_stable_range() {
        val transform = ImageViewerTransform(rotationDegrees = 170f).applyGesture(1f, 0f, 0f, 30f)

        assertTrue(transform.rotationDegrees in -180f..180f)
        assertEquals(-160f, transform.rotationDegrees, 0.001f)
    }

    @Test
    fun double_tap_reset_restores_the_original_transform() {
        val transform =
            ImageViewerTransform(scale = 3f, offsetX = 30f, offsetY = -12f, rotationDegrees = 48f)

        assertEquals(ImageViewerTransform(), transform.reset())
    }
}
