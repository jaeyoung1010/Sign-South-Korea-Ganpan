package com.signkorean.app

import org.junit.Assert.assertEquals
import org.junit.Test

class BuildConfigurationTest {
    @Test
    fun `minimum supported API remains 26`() {
        assertEquals(26, BuildConfig.MIN_SUPPORTED_API)
    }
}
