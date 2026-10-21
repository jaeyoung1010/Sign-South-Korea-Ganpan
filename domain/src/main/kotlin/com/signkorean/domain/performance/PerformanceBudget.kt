package com.signkorean.domain.performance

data class PerformanceMeasurement(
    val coldStartMillis: Long? = null,
    val inputToFrameMillis: Long? = null,
    val audioStartMillis: Long? = null,
    val frameDurationsMillis: List<Double> = emptyList(),
    val peakDecodedImageCount: Int? = null,
)

enum class PerformanceBudgetViolation(
    val userMessage: String? = null,
) {
    ColdStart,
    InputResponse,
    AudioStart,
    FramePacing,
    ImageMemory,
}

/** Release thresholds shared by benchmark adapters and CI reports. */
data class PerformanceBudget(
    val coldStartLimitMillis: Long = 2_000,
    val inputToFrameLimitMillis: Long = 100,
    val audioStartLimitMillis: Long = 300,
    val maxDecodedImageCount: Int = 2,
    val slowFrameDurationMillis: Double = 1_000.0 / 45.0,
    val minimumConsecutiveSlowFrames: Int = 3,
) {
    init {
        require(coldStartLimitMillis > 0)
        require(inputToFrameLimitMillis > 0)
        require(audioStartLimitMillis > 0)
        require(maxDecodedImageCount > 0)
        require(slowFrameDurationMillis.isFinite() && slowFrameDurationMillis > 0)
        require(minimumConsecutiveSlowFrames > 0)
    }

    fun evaluate(measurement: PerformanceMeasurement): Set<PerformanceBudgetViolation> =
        buildSet {
            if (measurement.coldStartMillis != null && measurement.coldStartMillis > coldStartLimitMillis) {
                add(PerformanceBudgetViolation.ColdStart)
            }
            if (measurement.inputToFrameMillis != null && measurement.inputToFrameMillis > inputToFrameLimitMillis) {
                add(PerformanceBudgetViolation.InputResponse)
            }
            if (measurement.audioStartMillis != null && measurement.audioStartMillis > audioStartLimitMillis) {
                add(PerformanceBudgetViolation.AudioStart)
            }
            if (measurement.frameDurationsMillis.hasConsecutiveSlowFrames()) {
                add(PerformanceBudgetViolation.FramePacing)
            }
            if (measurement.peakDecodedImageCount != null && measurement.peakDecodedImageCount > maxDecodedImageCount) {
                add(PerformanceBudgetViolation.ImageMemory)
            }
        }

    private fun List<Double>.hasConsecutiveSlowFrames(): Boolean {
        if (size < minimumConsecutiveSlowFrames) return false
        return windowed(minimumConsecutiveSlowFrames).any { window ->
            window.all { duration -> duration.isFinite() && duration > slowFrameDurationMillis }
        }
    }
}
