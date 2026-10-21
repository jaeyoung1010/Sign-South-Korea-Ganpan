package com.signkorean.domain.usecase

import com.signkorean.domain.model.PolicyDocument
import com.signkorean.domain.port.input.OpenPolicyDocument
import com.signkorean.domain.port.output.PolicyBrowserPort

class OpenPolicyDocumentUseCase(
    private val browser: PolicyBrowserPort,
) : OpenPolicyDocument {
    override fun invoke(document: PolicyDocument): Boolean = browser.open(document)
}
