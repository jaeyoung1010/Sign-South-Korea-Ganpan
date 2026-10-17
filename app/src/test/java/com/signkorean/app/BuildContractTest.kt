package com.signkorean.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildContractTest {
    @Test
    fun `API 26 is supported`() {
        assertTrue(BuildContract.supportsApi(26))
    }

    @Test
    fun `API below 26 is rejected`() {
        assertFalse(BuildContract.supportsApi(25))
    }
}
