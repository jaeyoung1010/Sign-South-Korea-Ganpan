package com.signkorean.feature.learn

import com.signkorean.domain.model.FocusRect
import kotlin.math.min

data class PhotoSize(
    val width: Float,
    val height: Float,
) {
    init {
        require(width > 0f && height > 0f) { "Photo dimensions must be positive" }
    }
}

data class PhotoBounds(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
) {
    val right: Float
        get() = left + width

    val bottom: Float
        get() = top + height
}

/** Finds the full-image bounds for a [androidx.compose.ui.layout.ContentScale.Fit] foreground. */
fun fitForegroundBounds(
    container: PhotoSize,
    image: PhotoSize,
): PhotoBounds {
    val scale = min(container.width / image.width, container.height / image.height)
    val width = image.width * scale
    val height = image.height * scale
    return PhotoBounds(
        left = (container.width - width) / 2f,
        top = (container.height - height) / 2f,
        width = width,
        height = height,
    )
}

/** Converts bundle percentage coordinates to the fitted foreground's on-screen pixels. */
fun focusFrameInFitForeground(
    focus: FocusRect,
    container: PhotoSize,
    image: PhotoSize,
): PhotoBounds {
    val imageBounds = fitForegroundBounds(container, image)
    return PhotoBounds(
        left = imageBounds.left + (focus.x / PERCENT_BASE) * imageBounds.width,
        top = imageBounds.top + (focus.y / PERCENT_BASE) * imageBounds.height,
        width = (focus.width / PERCENT_BASE) * imageBounds.width,
        height = (focus.height / PERCENT_BASE) * imageBounds.height,
    )
}

private const val PERCENT_BASE = 100f
