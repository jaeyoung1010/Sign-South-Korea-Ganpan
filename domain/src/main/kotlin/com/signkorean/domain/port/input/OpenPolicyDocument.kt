package com.signkorean.domain.port.input

import com.signkorean.domain.model.PolicyDocument

fun interface OpenPolicyDocument {
    operator fun invoke(document: PolicyDocument): Boolean
}
