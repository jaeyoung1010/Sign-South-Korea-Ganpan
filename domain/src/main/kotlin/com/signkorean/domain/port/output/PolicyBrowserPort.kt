package com.signkorean.domain.port.output

import com.signkorean.domain.model.PolicyDocument

fun interface PolicyBrowserPort {
    fun open(document: PolicyDocument): Boolean
}
