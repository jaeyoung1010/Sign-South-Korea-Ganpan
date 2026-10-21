package com.signkorean.data.db

internal fun SignKoreanDatabase.isConsistent(session: SessionEntity): Boolean {
    if (!isStructurallyConsistent(session)) return false

    val cards = sessionCardDao().forSession(session.sessionId)

    val ratings = setOf("GOT_IT", "REVIEW")
    val ratedCards = cards.take(session.currentIndex)
    val pendingCards = cards.drop(session.currentIndex)
    return ratedCards.all { it.rating in ratings && it.ratedAt != null } &&
        pendingCards.all { it.rating == null && it.ratedAt == null }
}

internal fun SignKoreanDatabase.isStructurallyConsistent(session: SessionEntity): Boolean {
    if (session.requestedLength <= 0 || session.currentIndex !in 0 until session.requestedLength) return false

    val cards = sessionCardDao().forSession(session.sessionId)
    if (cards.size != session.requestedLength) return false
    return cards.map(SessionCardEntity::position) == cards.indices.toList()
}
