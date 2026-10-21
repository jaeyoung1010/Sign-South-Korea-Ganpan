package com.signkorean.app.legal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signkorean.domain.model.PolicyDocument
import com.signkorean.domain.model.PolicyDocumentId
import com.signkorean.domain.port.input.LoadPolicyDocument
import com.signkorean.domain.port.input.OpenPolicyDocument
import com.signkorean.feature.settings.LegalUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class LegalViewModel
    @Inject
    constructor(
        private val loadPolicyDocument: LoadPolicyDocument,
        private val openPolicyDocument: OpenPolicyDocument,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow(LegalUiState(isLoading = true))
        private var hasLoaded = false

        init {
            load()
        }

        val uiState = mutableUiState.asStateFlow()

        fun load(force: Boolean = false) {
            if (hasLoaded && !force) return
            hasLoaded = true
            viewModelScope.launch {
                mutableUiState.update { it.copy(isLoading = true, loadError = null) }
                val documents = buildList {
                    PolicyDocumentId.entries.forEach { id ->
                        try {
                            add(loadPolicyDocument(id))
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (_: Exception) {
                            // Keep the other bundled policy available when one asset is damaged.
                        }
                    }
                }
                mutableUiState.update {
                    it.copy(
                        documents = documents,
                        isLoading = false,
                        usingBundledFallback = true,
                        loadError = if (documents.size == PolicyDocumentId.entries.size) null else "One or more policy copies could not be loaded.",
                    )
                }
            }
        }

        fun selectDocument(id: PolicyDocumentId) {
            mutableUiState.update {
                it.copy(
                    selectedDocumentId = id,
                    usingBundledFallback = true,
                    loadError = null,
                )
            }
        }

        fun openOnline() {
            val document = uiState.value.selectedDocument ?: return
            val opened = openPolicyDocument(document)
            mutableUiState.update {
                it.copy(
                    usingBundledFallback = !opened,
                    loadError = if (opened) null else "The online policy is unavailable. Showing the bundled copy.",
                )
            }
        }
    }
