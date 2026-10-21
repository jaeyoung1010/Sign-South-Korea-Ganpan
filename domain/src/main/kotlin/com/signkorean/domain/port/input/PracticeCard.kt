package com.signkorean.domain.port.input

fun interface PracticeCard {
    suspend operator fun invoke(request: PracticeCardRequest): StartSessionResult
}

data class PracticeCardRequest(
    val cardId: String,
) {
    init {
        require(cardId.isNotBlank()) { "Practice card ID must not be blank" }
    }
}
