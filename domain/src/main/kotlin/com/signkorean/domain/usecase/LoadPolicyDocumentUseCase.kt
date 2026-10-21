package com.signkorean.domain.usecase

import com.signkorean.domain.model.PolicyDocument
import com.signkorean.domain.model.PolicyDocumentId
import com.signkorean.domain.port.input.LoadPolicyDocument
import com.signkorean.domain.port.output.PolicyDocumentRepository

class LoadPolicyDocumentUseCase(
    private val repository: PolicyDocumentRepository,
) : LoadPolicyDocument {
    override suspend fun invoke(id: PolicyDocumentId): PolicyDocument = repository.load(id)
}
