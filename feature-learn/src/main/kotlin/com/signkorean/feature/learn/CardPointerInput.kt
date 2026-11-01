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
    eventPass: PointerEventPass,
    onMove: (offsetXDp: Float, offsetYDp: Float) -> Boolean,
    onRelease: (offsetXDp: Float, cancelled: Boolean, outOfBounds: Boolean) -> Unit,
): Modifier =
    pointerInput(density, eventPass) {
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
            var releaseSent = false
            try {
                while (!finished) {
                    // Answer details are vertically scrollable, so receive
                    // those swipes before the scroll container consumes them.
                    // The question card has no scrollable body and uses the
                    // main pass for its normal tap-and-reveal interaction.
                    val event = awaitPointerEvent(eventPass)
                    val change = event.changes.firstOrNull { it.id == down.id }
                    if (change == null) {
                        onRelease(0f, true, false)
                        releaseSent = true
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
                            onRelease(
                                result.offsetXpx / density.density,
                                result.cancelled && !result.outOfBounds,
                                result.outOfBounds,
                            )
                            releaseSent = true
                            finished = true
                        }
                    }
                }
            } finally {
                if (!releaseSent) onRelease(0f, true, false)
            }
        }
    }

private fun PointerInputChange.changedToCancel(): Boolean = !pressed && !changedToUp()
