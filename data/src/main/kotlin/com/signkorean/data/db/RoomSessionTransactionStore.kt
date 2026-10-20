package com.signkorean.data.db

import com.signkorean.domain.port.output.ClockPort
import java.time.Instant

data class RatingCommand(
    val sessionId: String,
    val position: Int,
    val cardId: String,
    val rating: String,
    val ratedAt: Instant,
    val updatedProgress: CardProgressEntity,
)

data class SessionCreationCommand(
    val session: SessionEntity,
    val cards: List<SessionCardEntity>,
)

class RoomSessionTransactionStore(
    private val database: SignKoreanDatabase,
    private val clock: ClockPort,
) {
    fun createSession(command: SessionCreationCommand) {
        saveSession(command, replaceActiveSession = false)
    }

    fun replaceActiveSession(command: SessionCreationCommand) {
        saveSession(command, replaceActiveSession = true)
    }

    private fun saveSession(
        command: SessionCreationCommand,
        replaceActiveSession: Boolean,
    ) {
        require(command.session.status == "ACTIVE") { "New session must be active" }
        require(command.session.currentIndex == 0) { "New session must start at position zero" }
        require(command.session.completedAt == null) { "New session cannot be completed" }
        require(command.cards.size == command.session.requestedLength) { "Card count must match requested length" }
        require(command.cards.map(SessionCardEntity::sessionId).all { it == command.session.sessionId }) {
            "All cards must belong to the new session"
        }
        require(command.cards.map(SessionCardEntity::position) == command.cards.indices.toList()) {
            "Card positions must be contiguous from zero"
        }

        database.runInTransaction {
            if (replaceActiveSession) {
                database.sessionDao().abandonActive(command.session.createdAt)
            } else {
                require(database.sessionDao().active() == null) { "An active session already exists" }
            }
            database.sessionDao().insert(command.session)
            database.sessionCardDao().insertAll(command.cards)
        }
    }

    fun rate(command: RatingCommand) {
        database.runInTransaction {
            val session = requireNotNull(database.sessionDao().get(command.sessionId)) { "Unknown session: ${command.sessionId}" }
            require(session.status == "ACTIVE") { "Session is not active: ${command.sessionId}" }
            require(session.currentIndex == command.position) { "Stale card position: ${command.position}" }
            val card =
                requireNotNull(
                    database.sessionCardDao().get(command.sessionId, command.position),
                ) { "Unknown session card position: ${command.position}" }
            require(card.cardId == command.cardId) { "Stale card id: ${command.cardId}" }
            require(card.rating == null) { "Card has already been rated: ${command.cardId}" }
            require(command.rating in RATINGS) { "Unsupported rating: ${command.rating}" }
            require(command.updatedProgress.cardId == command.cardId) { "Progress card id does not match rating command" }

            database.ratingSnapshotDao().upsert(snapshot(session, card, database.cardProgressDao().get(command.cardId)))
            database.cardProgressDao().upsert(command.updatedProgress)
            database.sessionCardDao().update(card.copy(rating = command.rating, ratedAt = command.ratedAt))
            val nextIndex = command.position + 1
            val isComplete = nextIndex == database.sessionCardDao().countForSession(command.sessionId)
            database.sessionDao().update(
                session.copy(
                    currentIndex = nextIndex,
                    status = if (isComplete) "COMPLETED" else "ACTIVE",
                    updatedAt = command.ratedAt,
                    completedAt = if (isComplete) command.ratedAt else null,
                ),
            )
        }
    }

    fun undoLastRating(sessionId: String) {
        database.runInTransaction {
            val snapshot = database.ratingSnapshotDao().forSession(sessionId) ?: return@runInTransaction
            val session = requireNotNull(database.sessionDao().get(sessionId)) { "Unknown session: $sessionId" }
            val card =
                requireNotNull(
                    database.sessionCardDao().get(sessionId, snapshot.position),
                ) { "Unknown snapshot position: ${snapshot.position}" }
            if (snapshot.hadProgress) {
                database.cardProgressDao().upsert(snapshot.toProgress())
            } else {
                database.cardProgressDao().delete(snapshot.cardId)
            }
            database.sessionCardDao().update(card.copy(rating = null, ratedAt = null))
            database.sessionDao().update(
                session.copy(
                    currentIndex = snapshot.position,
                    status = snapshot.previousStatus,
                    updatedAt = clock.now(),
                    completedAt = snapshot.previousCompletedAt,
                ),
            )
            database.ratingSnapshotDao().delete(sessionId)
        }
    }

    private fun snapshot(
        session: SessionEntity,
        card: SessionCardEntity,
        progress: CardProgressEntity?,
    ) = RatingSnapshotEntity(
        sessionId = session.sessionId,
        cardId = card.cardId,
        position = card.position,
        hadProgress = progress != null,
        stage = progress?.stage,
        mastered = progress?.mastered,
        lastRating = progress?.lastRating,
        lastReviewedAt = progress?.lastReviewedAt,
        nextReviewAt = progress?.nextReviewAt,
        reviewCount = progress?.reviewCount,
        gotItCount = progress?.gotItCount,
        version = progress?.version,
        previousStatus = session.status,
        previousCompletedAt = session.completedAt,
    )

    private fun RatingSnapshotEntity.toProgress() =
        CardProgressEntity(
            cardId = cardId,
            stage = checkNotNull(stage),
            mastered = checkNotNull(mastered),
            lastRating = lastRating,
            lastReviewedAt = lastReviewedAt,
            nextReviewAt = nextReviewAt,
            reviewCount = checkNotNull(reviewCount),
            gotItCount = checkNotNull(gotItCount),
            version = checkNotNull(version),
        )

    private companion object {
        val RATINGS = setOf("GOT_IT", "REVIEW")
    }
}
