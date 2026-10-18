package com.signkorean.designsystem.theme

import org.junit.Assert.assertTrue
import org.junit.Test

class ColorContrastTest {
    @Test
    fun `body ink on light background meets WCAG AA contrast`() {
        val contrast = ColorContrast.ratio(SignKoreanPalette.INK, SignKoreanPalette.BACKGROUND)

        assertTrue("Expected at least 4.5 to 1 but was $contrast", contrast >= 4.5)
    }
}
