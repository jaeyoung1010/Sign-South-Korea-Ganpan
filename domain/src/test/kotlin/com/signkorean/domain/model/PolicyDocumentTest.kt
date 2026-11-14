package com.signkorean.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyDocumentTest {
    private val validDocument =
        PolicyDocument(
            id = PolicyDocumentId.PRIVACY,
            title = "Privacy policy",
            version = "1.0",
            effectiveDate = "2026-10-21",
            body = "This is a bundled copy.",
            onlineUrl = "https://signkorean.app/privacy",
            bundledChecksum = "a".repeat(64),
        )

    @Test
    fun `document keeps release metadata and checksum`() {
        assertEquals(PolicyDocumentId.PRIVACY, validDocument.id)
        assertEquals("1.0", validDocument.version)
        assertEquals("2026-10-21", validDocument.effectiveDate)
        assertEquals(64, validDocument.bundledChecksum.length)
    }

    @Test
    fun `document rejects insecure URL and malformed checksum`() {
        assertThrows(IllegalArgumentException::class.java) {
            validDocument.copy(onlineUrl = "http://signkorean.app/privacy")
        }
        assertThrows(IllegalArgumentException::class.java) {
            validDocument.copy(bundledChecksum = "not-a-checksum")
        }
    }

    @Test
    fun `guest account is device-only and never carries email or sync`() {
        val account = GuestAccount.local()

        assertEquals(GuestAccount.StorageScope.THIS_DEVICE, account.storageScope)
        assertNull(account.email)
        assertFalse(account.cloudSyncAvailable)
        assertTrue(account.dataLostWhenAppDeleted)
    }
}
