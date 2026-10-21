package com.signkorean.app.legal

import com.signkorean.domain.model.PolicyDocument
import com.signkorean.domain.model.PolicyDocumentId
import com.signkorean.domain.port.input.LoadPolicyDocument
import com.signkorean.domain.port.input.OpenPolicyDocument
import com.signkorean.testsupport.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LegalViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val documents =
        listOf(
            document(PolicyDocumentId.PRIVACY),
            document(PolicyDocumentId.TERMS),
        )

    @Test
    fun `loads both bundled documents and allows selecting terms`() =
        runTest {
            val viewModel = LegalViewModel(loading(documents), OpenPolicyDocument { true })
            advanceUntilIdle()

            assertEquals(documents, viewModel.uiState.value.documents)
            assertEquals(PolicyDocumentId.PRIVACY, viewModel.uiState.value.selectedDocumentId)
            viewModel.selectDocument(PolicyDocumentId.TERMS)
            assertEquals(PolicyDocumentId.TERMS, viewModel.uiState.value.selectedDocumentId)
            assertNotNull(viewModel.uiState.value.selectedDocument)
        }

    @Test
    fun `failed online open keeps bundled fallback and exposes a message`() =
        runTest {
            val viewModel = LegalViewModel(loading(documents), OpenPolicyDocument { false })
            advanceUntilIdle()

            viewModel.openOnline()

            assertTrue(viewModel.uiState.value.usingBundledFallback)
            assertEquals("The online policy is unavailable. Showing the bundled copy.", viewModel.uiState.value.loadError)
        }

    @Test
    fun `successful online open clears fallback message`() =
        runTest {
            val viewModel = LegalViewModel(loading(documents), OpenPolicyDocument { true })
            advanceUntilIdle()

            viewModel.openOnline()

            assertFalse(viewModel.uiState.value.usingBundledFallback)
            assertEquals(null, viewModel.uiState.value.loadError)
        }

    private fun loading(values: List<PolicyDocument>) =
        LoadPolicyDocument { id -> values.first { it.id == id } }

    private fun document(id: PolicyDocumentId) =
        PolicyDocument(
            id = id,
            title = id.name,
            version = "1.0",
            effectiveDate = "2026-10-21",
            body = "Bundled ${id.name}",
            onlineUrl = "https://signkorean.app/${id.name.lowercase()}",
            bundledChecksum = "a".repeat(64),
        )
}
