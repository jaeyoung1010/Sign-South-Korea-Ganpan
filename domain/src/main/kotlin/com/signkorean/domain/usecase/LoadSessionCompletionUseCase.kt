package com.signkorean.domain.usecase

import com.signkorean.domain.model.SessionCompletion
import com.signkorean.domain.port.input.LoadSessionCompletion
import com.signkorean.domain.port.input.LoadSessionCompletionRequest
import com.signkorean.domain.port.output.SessionRepository

class LoadSessionCompletionUseCase(
    private val sessionRepository: SessionRepository,
) : LoadSessionCompletion {
    override suspend fun invoke(request: LoadSessionCompletionRequest): SessionCompletion? =
        sessionRepository.getSessionCompletion(request.sessionId)
}
