package com.signkorean.domain.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class GesturePolicyTest {
    private val policy = GesturePolicy()

    @Test
    fun `axis remains undecided through twelve dp and locks horizontal only above one point two ratio`() {
        assertEquals(DragAxis.Undecided, policy.axisFor(dxDp = 12f, dyDp = 0f))
        assertEquals(DragAxis.Vertical, policy.axisFor(dxDp = 9f, dyDp = 9f))
        assertEquals(DragAxis.Vertical, policy.axisFor(dxDp = 13f, dyDp = 11f))
        assertEquals(DragAxis.Horizontal, policy.axisFor(dxDp = 13.31f, dyDp = 11f))
        assertEquals(DragAxis.Horizontal, policy.axisFor(dxDp = -13f, dyDp = 1f, previousAxis = DragAxis.Horizontal))
    }

    @Test
    fun `question drag is clamped and can only request a check answer hint`() {
        val feedback = policy.drag(QuestionGestureInput(dxDp = -100f, dyDp = 0f))

        assertEquals(DragAxis.Horizontal, feedback.axis)
        assertEquals(-30f, feedback.offsetDp)
        assertTrue(feedback.showCheckAnswerHint)
        assertEquals(QuestionRelease.CheckAnswerFirst, policy.release(QuestionGestureInput(dxDp = 28.01f, dyDp = 0f)))
        assertEquals(QuestionRelease.SnapBack, policy.release(QuestionGestureInput(dxDp = 28f, dyDp = 0f)))
    }

    @Test
    fun `question drag never produces a rating regardless of its direction`() {
        listOf(-Float.MAX_VALUE, -88.01f, 0f, 88.01f, Float.MAX_VALUE).forEach { dxDp ->
            val result = policy.release(QuestionGestureInput(dxDp = dxDp, dyDp = 0f))

            assertTrue(
                result == QuestionRelease.Ignored ||
                    result == QuestionRelease.SnapBack ||
                    result == QuestionRelease.CheckAnswerFirst,
            )
        }
    }

    @Test
    fun `answer threshold is exclusive and retains fixed physical direction mapping`() {
        assertEquals(AnswerRelease.SnapBack, policy.release(AnswerGestureInput(dxDp = -88f, dyDp = 0f)))
        assertEquals(AnswerRelease.SnapBack, policy.release(AnswerGestureInput(dxDp = 88f, dyDp = 0f)))
        assertEquals(
            AnswerRelease.Commit(RatingDecision.GotIt),
            policy.release(AnswerGestureInput(dxDp = -88.01f, dyDp = 0f)),
        )
        assertEquals(
            AnswerRelease.Commit(RatingDecision.Review),
            policy.release(AnswerGestureInput(dxDp = 88.01f, dyDp = 0f)),
        )
    }

    @Test
    fun `answer overlay is opposite the exit direction and caps at ninety dp`() {
        val gotIt = policy.drag(AnswerGestureInput(dxDp = -45f, dyDp = 0f))
        val review = policy.drag(AnswerGestureInput(dxDp = 180f, dyDp = 0f))

        assertEquals(DragOverlay.GotIt, gotIt.overlay)
        assertEquals(.5f, gotIt.overlayStrength)
        assertEquals(DragOverlay.Review, review.overlay)
        assertEquals(1f, review.overlayStrength)
    }

    @Test
    fun `vertical and multi pointer input cannot start a card rating`() {
        val vertical = AnswerGestureInput(dxDp = 10f, dyDp = 20f)
        val multiPointer = AnswerGestureInput(dxDp = -120f, dyDp = 0f, activePointerCount = 2)

        assertEquals(DragAxis.Vertical, policy.drag(vertical).axis)
        assertEquals(null, policy.drag(vertical).overlay)
        assertEquals(AnswerRelease.Ignored, policy.release(vertical))
        assertEquals(DragAxis.Undecided, policy.drag(multiPointer).axis)
        assertEquals(AnswerRelease.Ignored, policy.release(multiPointer))
    }

    @Test
    fun `non finite and random inputs are deterministic and never invert a rating direction`() {
        val random = Random(19_2026L)
        val specialValues = floatArrayOf(Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY)
        specialValues.forEach { value ->
            assertEquals(AnswerDragFeedback.ignored(), policy.drag(AnswerGestureInput(value, 0f)))
            assertEquals(AnswerRelease.Ignored, policy.release(AnswerGestureInput(0f, value)))
        }

        repeat(10_000) {
            val dxDp = random.nextFloat() * 400f - 200f
            val dyDp = random.nextFloat() * 400f - 200f
            val input =
                AnswerGestureInput(
                    dxDp = dxDp,
                    dyDp = dyDp,
                    activePointerCount = random.nextInt(4),
                    previousAxis = DragAxis.entries[random.nextInt(DragAxis.entries.size)],
                )

            val first = policy.release(input)
            assertEquals(first, policy.release(input))
            when (first) {
                is AnswerRelease.Commit -> {
                    if (input.dxDp < 0f) assertEquals(RatingDecision.GotIt, first.decision)
                    if (input.dxDp > 0f) assertEquals(RatingDecision.Review, first.decision)
                }

                AnswerRelease.Ignored,
                AnswerRelease.SnapBack,
                -> {
                    Unit
                }
            }
        }
    }
}
