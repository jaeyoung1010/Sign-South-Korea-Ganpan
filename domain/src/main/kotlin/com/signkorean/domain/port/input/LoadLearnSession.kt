package com.signkorean.domain.port.input

import com.signkorean.domain.model.LoadLearnSessionResult

fun interface LoadLearnSession {
    suspend operator fun invoke(): LoadLearnSessionResult
}
