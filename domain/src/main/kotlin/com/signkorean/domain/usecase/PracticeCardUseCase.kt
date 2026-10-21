package com.signkorean.domain.usecase

import com.signkorean.domain.port.input.PracticeCard
import com.signkorean.domain.port.input.PracticeCardRequest
import com.signkorean.domain.port.input.StartSession
import com.signkorean.domain.port.input.StartSessionRequest
import com.signkorean.domain.port.input.StartSessionResult

class PracticeCardUseCase(
    private val startSession: StartSession,
) : PracticeCard {
    override suspend fun invoke(request: PracticeCardRequest): StartSessionResult =
        startSession(StartSessionRequest.practice(request.cardId))
}
