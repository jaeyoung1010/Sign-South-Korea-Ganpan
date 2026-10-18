package com.signkorean.designsystem.testing

import org.junit.Assert.assertEquals
import org.junit.Test

class SignKoreanTestTagsTest {
    @Test
    fun `card tags use stable IDs rather than translated UI text`() {
        assertEquals("learn.card.active", SignKoreanTestTags.LEARN_ACTIVE_CARD)
        assertEquals("learn.card.sign-01", SignKoreanTestTags.learnCard("sign-01"))
    }
}
