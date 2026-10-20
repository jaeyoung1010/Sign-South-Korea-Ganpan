package com.signkorean.domain.port.input

fun interface RevealAnswer {
    suspend operator fun invoke(): RevealAnswerResult
}

sealed interface RevealAnswerResult {
    data object Revealed : RevealAnswerResult

    data object AlreadyRevealed : RevealAnswerResult

    data object Unavailable : RevealAnswerResult
}
