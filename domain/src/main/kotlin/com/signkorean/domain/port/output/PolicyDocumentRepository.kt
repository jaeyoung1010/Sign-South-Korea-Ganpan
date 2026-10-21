package com.signkorean.domain.port.output

import com.signkorean.domain.model.PolicyDocument
import com.signkorean.domain.model.PolicyDocumentId

fun interface PolicyDocumentRepository {
    suspend fun load(id: PolicyDocumentId): PolicyDocument
}
