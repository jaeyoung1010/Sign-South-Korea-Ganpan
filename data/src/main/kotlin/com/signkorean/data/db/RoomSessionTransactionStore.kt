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

sealed interface RatingTransactionResult {
    data class Applied(
        val nextPosition: Int,
        val completed: Boolean,
    ) : RatingTransactionResult

    data object AlreadyRated : RatingTransactionResult

    data object Unavailable : RatingTransactionResult
}

sealed interface UndoTransactionResult {
    data class Restored(
        val cardId: String,
        val position: Int,
        val rating: String,
    ) : UndoTransactionResult

    data object NothingToUndo : UndoTransactionResult

    data object Unavailable : UndoTransactionResult
}

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
                val existingSession = database.sessionDao().resumable()
                if (existingSession != null) {
                    if (database.isConsistent(existingSession)) {
                        error("An active session already exists")
                    }
                    database.sessionDao().update(
                        existingSession.copy(
                            status = "ABANDONED",
                            updatedAt = command.session.createdAt,
                        ),
                    )
                }
            }
            database.sessionDao().insert(command.session)
            database.sessionCardDao().insertAll(command.cards)
        }
    }

    fun rate(command: RatingCommand) {
        require(tryRate(command) is RatingTransactionResult.Applied) { "Rating transaction was not applied" }
    }

    fun tryRate(command: RatingCommand): RatingTransactionResult =
        database.runInTransaction<RatingTransactionResult> {
            val session = database.sessionDao().get(command.sessionId) ?: return@runInTransaction RatingTransactionResult.Unavailable
            val card =
                database.sessionCardDao().get(command.sessionId, command.position)
                    ?: return@runInTransaction RatingTransactionResult.Unavailable
            if (card.cardId != command.cardId) return@runInTransaction RatingTransactionResult.Unavailable
            if (card.rating != null) return@runInTransaction RatingTransactionResult.AlreadyRated
            if (session.status != "ACTIVE" || session.currentIndex != command.position) {
                return@runInTransaction RatingTransactionResult.Unavailable
            }
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
            RatingTransactionResult.Applied(nextPosition = nextIndex, completed = isComplete)
        }

    fun undoLastRating(sessionId: String) {
        tryUndoLastRating(sessionId)
    }

    fun tryUndoLastRating(sessionId: String): UndoTransactionResult =
        database.runInTransaction<UndoTransactionResult> {
            val snapshot = database.ratingSnapshotDao().forSession(sessionId) ?: return@runInTransaction UndoTransactionResult.NothingToUndo
            val session = database.sessionDao().get(sessionId) ?: return@runInTransaction UndoTransactionResult.Unavailable
            val card =
                database.sessionCardDao().get(sessionId, snapshot.position)
                    ?: return@runInTransaction UndoTransactionResult.Unavailable
            val rating = card.rating ?: return@runInTransaction UndoTransactionResult.Unavailable
            if (rating !in RATINGS || card.cardId != snapshot.cardId || session.currentIndex != snapshot.position + 1) {
                return@runInTransaction UndoTransactionResult.Unavailable
            }
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
            UndoTransactionResult.Restored(card.cardId, card.position, rating)
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
