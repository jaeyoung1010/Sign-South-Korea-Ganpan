package com.signkorean.feature.learn

data class PointerPoint(
    val xPx: Float,
    val yPx: Float,
) {
    init {
        require(xPx.isFinite() && yPx.isFinite())
    }
}

data class PointerBounds(
    val leftPx: Float,
    val topPx: Float,
    val rightPx: Float,
    val bottomPx: Float,
) {
    init {
        require(leftPx.isFinite() && topPx.isFinite() && rightPx.isFinite() && bottomPx.isFinite())
        require(rightPx >= leftPx && bottomPx >= topPx)
    }

    fun clamp(point: PointerPoint): PointerPoint =
        PointerPoint(
            xPx = point.xPx.coerceIn(leftPx, rightPx),
            yPx = point.yPx.coerceIn(topPx, bottomPx),
        )
}

sealed interface PointerBoundaryEvent {
    data object Ignored : PointerBoundaryEvent

    data class Moved(
        val point: PointerPoint,
        val offsetXpx: Float,
        val offsetYpx: Float,
    ) : PointerBoundaryEvent

    data class Ended(
        val point: PointerPoint,
        val offsetXpx: Float,
        val offsetYpx: Float,
        val cancelled: Boolean,
        val outOfBounds: Boolean = false,
    ) : PointerBoundaryEvent
}

/** Keeps pointer coordinates inside the stage and makes termination explicit. */
class PointerBoundaryAdapter(
    private val bounds: PointerBounds,
) {
    private var active = false
    private var start = PointerPoint(bounds.leftPx, bounds.topPx)
    private var last = start

    fun begin(point: PointerPoint) {
        start = bounds.clamp(point)
        last = start
        active = true
    }

    fun move(point: PointerPoint): PointerBoundaryEvent {
        if (!active) return PointerBoundaryEvent.Ignored
        val clamped = bounds.clamp(point)
        val outOfBounds = clamped != point
        last = clamped
        if (outOfBounds) {
            active = false
            return ended(cancelled = true, outOfBounds = true)
        }
        return moved()
    }

    fun end(cancelled: Boolean = false): PointerBoundaryEvent {
        if (!active) return PointerBoundaryEvent.Ignored
        active = false
        return ended(cancelled = cancelled, outOfBounds = false)
    }

    private fun moved(): PointerBoundaryEvent.Moved =
        PointerBoundaryEvent.Moved(
            point = last,
            offsetXpx = last.xPx - start.xPx,
            offsetYpx = last.yPx - start.yPx,
        )

    private fun ended(
        cancelled: Boolean,
        outOfBounds: Boolean,
    ): PointerBoundaryEvent.Ended =
        PointerBoundaryEvent.Ended(
            point = last,
            offsetXpx = last.xPx - start.xPx,
            offsetYpx = last.yPx - start.yPx,
            cancelled = cancelled,
            outOfBounds = outOfBounds,
        )
}
