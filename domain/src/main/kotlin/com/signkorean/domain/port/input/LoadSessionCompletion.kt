package com.signkorean.domain.port.input

import com.signkorean.domain.model.SessionCompletion

fun interface LoadSessionCompletion {
    suspend operator fun invoke(request: LoadSessionCompletionRequest): SessionCompletion?
}

data class LoadSessionCompletionRequest(
    val sessionId: String,
) {
    init {
        require(sessionId.isNotBlank())
    }
}
