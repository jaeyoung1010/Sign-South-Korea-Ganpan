package com.signkorean.feature.learn

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density

internal fun Modifier.cardPointerInput(
    density: Density,
    onMove: (offsetXDp: Float, offsetYDp: Float) -> Boolean,
    onRelease: (cancelled: Boolean, outOfBounds: Boolean) -> Unit,
): Modifier =
    pointerInput(density) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val boundary =
                PointerBoundaryAdapter(
                    PointerBounds(
                        leftPx = 0f,
                        topPx = 0f,
                        rightPx = size.width.toFloat(),
                        bottomPx = size.height.toFloat(),
                    ),
                )
            boundary.begin(PointerPoint(down.position.x, down.position.y))
            var finished = false
            while (!finished) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null) {
                    onRelease(true, false)
                    finished = true
                    continue
                }
                val result =
                    when {
                        change.changedToCancel() -> boundary.end(cancelled = true)
                        change.changedToUp() -> boundary.end()
                        else -> boundary.move(PointerPoint(change.position.x, change.position.y))
                    }
                when (result) {
                    PointerBoundaryEvent.Ignored -> {
                        finished = true
                    }

                    is PointerBoundaryEvent.Moved -> {
                        if (onMove(result.offsetXpx / density.density, result.offsetYpx / density.density)) {
                            change.consume()
                        }
                    }

                    is PointerBoundaryEvent.Ended -> {
                        if (onMove(result.offsetXpx / density.density, result.offsetYpx / density.density)) {
                            change.consume()
                        }
                        onRelease(result.cancelled && !result.outOfBounds, result.outOfBounds)
                        finished = true
                    }
                }
            }
        }
    }

private fun PointerInputChange.changedToCancel(): Boolean = !pressed && !changedToUp()
