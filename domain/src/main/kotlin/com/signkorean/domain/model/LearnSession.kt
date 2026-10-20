package com.signkorean.domain.model

/** Raw active-session data. It deliberately permits malformed persisted data so callers can recover safely. */
data class ActiveLearnSession(
    val sessionId: String,
    val currentIndex: Int,
    val requestedLength: Int,
    val cards: List<LearnSessionCard>,
)

data class LearnSessionCard(
    val position: Int,
    val cardId: String,
    val answerRevealedAt: java.time.Instant? = null,
)

sealed interface LoadLearnSessionResult {
    data class Active(
        val sessionId: String,
        val card: SignCard,
        val position: Int,
        val totalCount: Int,
        val photo: LearnSessionPhoto = LearnSessionPhoto.from(card),
        val answer: LearnSessionAnswer = LearnSessionAnswer.placeholder(),
        val upcomingCards: List<LearnSessionPreview> = emptyList(),
        val answerRevealed: Boolean = false,
    ) : LoadLearnSessionResult {
        val progressFraction: Float
            get() = position.toFloat() / totalCount
    }

    data object Completed : LoadLearnSessionResult

    data class Invalid(
        val error: LearnSessionError,
    ) : LoadLearnSessionResult
}

data class LearnSessionAnswer(
    val displayMeaning: String,
    val pronunciationGuide: String,
    val wordParts: List<LocalizedWordPart>,
    val streetNote: String,
    val showRomanization: Boolean,
) {
    companion object {
        fun from(
            copy: LocalizedCardCopy,
            showRomanization: Boolean,
        ): LearnSessionAnswer =
            LearnSessionAnswer(
                displayMeaning = copy.displayMeaning,
                pronunciationGuide = copy.pronunciationGuide,
                wordParts = copy.wordParts,
                streetNote = copy.streetNote,
                showRomanization = showRomanization,
            )

        fun placeholder(): LearnSessionAnswer =
            LearnSessionAnswer(
                displayMeaning = "",
                pronunciationGuide = "",
                wordParts = emptyList(),
                streetNote = "",
                showRomanization = false,
            )
    }
}

data class LearnSessionPhoto(
    val imageAssetUri: String,
    val focus: FocusRect,
    val questionContentDescription: String,
    val answerContentDescription: String,
) {
    companion object {
        fun from(
            card: SignCard,
            copy: LocalizedCardCopy? = null,
        ): LearnSessionPhoto =
            LearnSessionPhoto(
                imageAssetUri = card.imageAssetUri,
                focus = card.focus,
                questionContentDescription = copy?.imageAltTextQuestion ?: "Street sign photo.",
                answerContentDescription = copy?.imageAltTextAnswer ?: "Street sign photo for ${card.korean}.",
            )
    }
}

data class LearnSessionPreview(
    val cardId: String,
    val imageAssetUri: String,
)

enum class LearnSessionError {
    NoActiveSession,
    EmptySession,
    InvalidCardCount,
    InvalidCardOrder,
    InvalidCurrentIndex,
    UnknownCard,
    Unavailable,
}
