package com.signkorean.domain.port.input

import com.signkorean.domain.model.PolicyDocument
import com.signkorean.domain.model.PolicyDocumentId

fun interface LoadPolicyDocument {
    suspend operator fun invoke(id: PolicyDocumentId): PolicyDocument
}
