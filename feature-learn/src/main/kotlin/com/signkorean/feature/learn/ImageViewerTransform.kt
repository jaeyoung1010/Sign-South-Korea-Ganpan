package com.signkorean.feature.learn

/**
 * Pure interaction state for the full-screen photo viewer.
 *
 * Keeping this value object independent from Compose makes gesture bounds and reset behavior
 * deterministic to test, while the composable remains an adapter for pointer events.
 */
data class ImageViewerTransform(
    val scale: Float = MIN_SCALE,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotationDegrees: Float = 0f,
) {
    fun applyGesture(
        zoomChange: Float,
        panX: Float,
        panY: Float,
        rotationDelta: Float,
    ): ImageViewerTransform {
        val safeZoom = zoomChange.takeIf { it.isFinite() && it > 0f } ?: 1f
        val safePanX = panX.takeIf(Float::isFinite) ?: 0f
        val safePanY = panY.takeIf(Float::isFinite) ?: 0f
        val safeRotation = rotationDelta.takeIf(Float::isFinite) ?: 0f

        return copy(
            scale = (scale * safeZoom).coerceIn(MIN_SCALE, MAX_SCALE),
            offsetX = offsetX + safePanX,
            offsetY = offsetY + safePanY,
            rotationDegrees = normalizeDegrees(rotationDegrees + safeRotation),
        )
    }

    fun reset(): ImageViewerTransform = ImageViewerTransform()

    companion object {
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 4f

        private fun normalizeDegrees(degrees: Float): Float {
            val wrapped = (degrees + 180f) % 360f
            return if (wrapped < 0f) wrapped + 180f else wrapped - 180f
        }
    }
}
