package com.signkorean.domain.usecase

import com.signkorean.domain.model.PolicyDocument
import com.signkorean.domain.model.PolicyDocumentId
import com.signkorean.domain.port.output.PolicyBrowserPort
import com.signkorean.domain.port.output.PolicyDocumentRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyDocumentUseCaseTest {
    private val document =
        PolicyDocument(
            id = PolicyDocumentId.TERMS,
            title = "Terms of use",
            version = "1.0",
            effectiveDate = "2026-10-21",
            body = "Terms",
            onlineUrl = "https://signkorean.app/terms",
            bundledChecksum = "b".repeat(64),
        )

    @Test
    fun `load use case delegates document id to repository`() =
        runTest {
            var requested: PolicyDocumentId? = null
            val useCase =
                LoadPolicyDocumentUseCase(
                    PolicyDocumentRepository { id ->
                        requested = id
                        document
                    },
                )

            assertEquals(document, useCase(PolicyDocumentId.TERMS))
            assertEquals(PolicyDocumentId.TERMS, requested)
        }

    @Test
    fun `open use case reports browser result without changing document`() {
        var opened: PolicyDocument? = null
        val useCase =
            OpenPolicyDocumentUseCase(
                PolicyBrowserPort {
                    opened = it
                    true
                },
            )

        assertTrue(useCase(document))
        assertEquals(document, opened)
    }
}
