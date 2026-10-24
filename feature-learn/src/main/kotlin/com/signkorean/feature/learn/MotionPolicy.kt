package com.signkorean.feature.learn

/**
 * UI-only motion decisions. Keeping this policy pure makes accessibility behavior testable
 * without a Compose runtime and keeps animation timing out of the domain layer.
 */
data class MotionPolicy(
    val reduceMotion: Boolean,
) {
    val ratingAnimationDurationMillis: Long
        get() = if (reduceMotion) REDUCED_RATING_DURATION_MILLIS else DEFAULT_RATING_DURATION_MILLIS

    val cardExitTranslationFactor: Float
        get() = if (reduceMotion) 0f else 1.25f

    val cardExitRotationDegrees: Float
        get() = if (reduceMotion) 0f else 8f

    companion object {
        const val DEFAULT_RATING_DURATION_MILLIS = 420L
        const val REDUCED_RATING_DURATION_MILLIS = 20L
    }
}
