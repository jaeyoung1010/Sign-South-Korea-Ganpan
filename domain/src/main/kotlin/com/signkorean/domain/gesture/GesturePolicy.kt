package com.signkorean.domain.gesture

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

/** The single source of truth for gesture thresholds, all expressed in dp. */
data class GesturePolicyConfig(
    val axisLockThresholdDp: Float = 12f,
    val horizontalDominanceRatio: Float = 1.2f,
    val questionOffsetLimitDp: Float = 30f,
    val questionHintThresholdDp: Float = 28f,
    val answerCommitThresholdDp: Float = 88f,
    val overlayFullStrengthDp: Float = 90f,
) {
    init {
        require(axisLockThresholdDp > 0f)
        require(horizontalDominanceRatio > 1f)
        require(questionOffsetLimitDp > questionHintThresholdDp)
        require(questionHintThresholdDp > 0f)
        require(answerCommitThresholdDp > 0f)
        require(overlayFullStrengthDp > 0f)
    }
}

enum class DragAxis {
    Undecided,
    Horizontal,
    Vertical,
}

enum class DragOverlay {
    GotIt,
    Review,
}

enum class RatingDecision {
    GotIt,
    Review,
}

data class QuestionGestureInput(
    val dxDp: Float,
    val dyDp: Float,
    val activePointerCount: Int = 1,
    val previousAxis: DragAxis = DragAxis.Undecided,
)

data class AnswerGestureInput(
    val dxDp: Float,
    val dyDp: Float,
    val activePointerCount: Int = 1,
    val previousAxis: DragAxis = DragAxis.Undecided,
)

data class QuestionDragFeedback(
    val axis: DragAxis,
    val offsetDp: Float,
    val showCheckAnswerHint: Boolean,
) {
    companion object {
        fun ignored(): QuestionDragFeedback = QuestionDragFeedback(DragAxis.Undecided, 0f, false)
    }
}

data class AnswerDragFeedback(
    val axis: DragAxis,
    val offsetDp: Float,
    val overlay: DragOverlay?,
    val overlayStrength: Float,
) {
    companion object {
        fun ignored(): AnswerDragFeedback = AnswerDragFeedback(DragAxis.Undecided, 0f, null, 0f)
    }
}

/** A question interaction deliberately has no rating-carrying case. */
sealed interface QuestionRelease {
    data object Ignored : QuestionRelease

    data object SnapBack : QuestionRelease

    data object CheckAnswerFirst : QuestionRelease
}

sealed interface AnswerRelease {
    data object Ignored : AnswerRelease

    data object SnapBack : AnswerRelease

    data class Commit(
        val decision: RatingDecision,
    ) : AnswerRelease
}

class GesturePolicy(
    private val config: GesturePolicyConfig = GesturePolicyConfig(),
) {
    fun axisFor(
        dxDp: Float,
        dyDp: Float,
        previousAxis: DragAxis = DragAxis.Undecided,
    ): DragAxis {
        if (!dxDp.isFinite() || !dyDp.isFinite()) return DragAxis.Undecided
        if (previousAxis != DragAxis.Undecided) return previousAxis
        if (hypot(dxDp.toDouble(), dyDp.toDouble()) <= config.axisLockThresholdDp) return DragAxis.Undecided
        return if (abs(dxDp) > abs(dyDp) * config.horizontalDominanceRatio) {
            DragAxis.Horizontal
        } else {
            DragAxis.Vertical
        }
    }

    fun drag(input: QuestionGestureInput): QuestionDragFeedback {
        if (!input.isUsable()) return QuestionDragFeedback.ignored()
        val axis = axisFor(input.dxDp, input.dyDp, input.previousAxis)
        if (axis != DragAxis.Horizontal) return QuestionDragFeedback(axis, 0f, false)
        val offset = input.dxDp.coerceIn(-config.questionOffsetLimitDp, config.questionOffsetLimitDp)
        return QuestionDragFeedback(
            axis = axis,
            offsetDp = offset,
            showCheckAnswerHint = abs(input.dxDp) > config.questionHintThresholdDp,
        )
    }

    fun release(input: QuestionGestureInput): QuestionRelease {
        if (!input.isUsable()) return QuestionRelease.Ignored
        return when (axisFor(input.dxDp, input.dyDp, input.previousAxis)) {
            DragAxis.Horizontal -> {
                if (abs(input.dxDp) > config.questionHintThresholdDp) {
                    QuestionRelease.CheckAnswerFirst
                } else {
                    QuestionRelease.SnapBack
                }
            }

            DragAxis.Undecided,
            DragAxis.Vertical,
            -> {
                QuestionRelease.Ignored
            }
        }
    }

    fun drag(input: AnswerGestureInput): AnswerDragFeedback {
        if (!input.isUsable()) return AnswerDragFeedback.ignored()
        val axis = axisFor(input.dxDp, input.dyDp, input.previousAxis)
        if (axis != DragAxis.Horizontal) return AnswerDragFeedback(axis, 0f, null, 0f)
        val overlay = input.dxDp.overlayFor()
        return AnswerDragFeedback(
            axis = axis,
            offsetDp = input.dxDp,
            overlay = overlay,
            overlayStrength = min(abs(input.dxDp) / config.overlayFullStrengthDp, 1f),
        )
    }

    fun release(input: AnswerGestureInput): AnswerRelease {
        if (!input.isUsable()) return AnswerRelease.Ignored
        if (axisFor(input.dxDp, input.dyDp, input.previousAxis) != DragAxis.Horizontal) {
            return AnswerRelease.Ignored
        }
        return when {
            input.dxDp < -config.answerCommitThresholdDp -> AnswerRelease.Commit(RatingDecision.GotIt)
            input.dxDp > config.answerCommitThresholdDp -> AnswerRelease.Commit(RatingDecision.Review)
            else -> AnswerRelease.SnapBack
        }
    }

    private fun QuestionGestureInput.isUsable(): Boolean = activePointerCount == 1 && dxDp.isFinite() && dyDp.isFinite()

    private fun AnswerGestureInput.isUsable(): Boolean = activePointerCount == 1 && dxDp.isFinite() && dyDp.isFinite()

    private fun Float.overlayFor(): DragOverlay? =
        when {
            this < 0f -> DragOverlay.GotIt
            this > 0f -> DragOverlay.Review
            else -> null
        }
}
