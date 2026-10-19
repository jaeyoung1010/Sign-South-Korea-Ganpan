package com.signkorean.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "card_progress")
data class CardProgressEntity(
    @PrimaryKey
    @ColumnInfo(name = "card_id")
    val cardId: String,
    val stage: Int,
    val mastered: Boolean,
    @ColumnInfo(name = "last_rating")
    val lastRating: String?,
    @ColumnInfo(name = "last_reviewed_at")
    val lastReviewedAt: Instant?,
    @ColumnInfo(name = "next_review_at")
    val nextReviewAt: Instant?,
    @ColumnInfo(name = "review_count")
    val reviewCount: Int,
    @ColumnInfo(name = "got_it_count")
    val gotItCount: Int,
    val version: Int,
) {
    companion object {
        fun initial(cardId: String) =
            CardProgressEntity(
                cardId = cardId,
                stage = 0,
                mastered = false,
                lastRating = null,
                lastReviewedAt = null,
                nextReviewAt = null,
                reviewCount = 0,
                gotItCount = 0,
                version = 0,
            )
    }
}

@Entity(tableName = "sessions", indices = [Index(value = ["status"])])
data class SessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    val status: String,
    @ColumnInfo(name = "current_index")
    val currentIndex: Int,
    @ColumnInfo(name = "requested_length")
    val requestedLength: Int,
    val source: String,
    val category: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
    @ColumnInfo(name = "completed_at")
    val completedAt: Instant?,
) {
    companion object {
        fun active(
            id: String,
            requestedLength: Int,
            now: Instant = Instant.EPOCH,
        ) = SessionEntity(
            sessionId = id,
            status = "ACTIVE",
            currentIndex = 0,
            requestedLength = requestedLength,
            source = "DAILY",
            category = null,
            createdAt = now,
            updatedAt = now,
            completedAt = null,
        )
    }
}

@Entity(
    tableName = "session_cards",
    primaryKeys = ["session_id", "position"],
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["session_id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["session_id"]), Index(value = ["card_id"])],
)
data class SessionCardEntity(
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    val position: Int,
    @ColumnInfo(name = "card_id")
    val cardId: String,
    val source: String,
    @ColumnInfo(name = "answer_revealed_at")
    val answerRevealedAt: Instant? = null,
    val rating: String? = null,
    @ColumnInfo(name = "rated_at")
    val ratedAt: Instant? = null,
)

@Entity(
    tableName = "rating_snapshot",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["session_id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class RatingSnapshotEntity(
    @PrimaryKey
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "card_id")
    val cardId: String,
    val position: Int,
    @ColumnInfo(name = "had_progress")
    val hadProgress: Boolean,
    val stage: Int?,
    val mastered: Boolean?,
    @ColumnInfo(name = "last_rating")
    val lastRating: String?,
    @ColumnInfo(name = "last_reviewed_at")
    val lastReviewedAt: Instant?,
    @ColumnInfo(name = "next_review_at")
    val nextReviewAt: Instant?,
    @ColumnInfo(name = "review_count")
    val reviewCount: Int?,
    @ColumnInfo(name = "got_it_count")
    val gotItCount: Int?,
    val version: Int?,
    @ColumnInfo(name = "previous_status")
    val previousStatus: String,
    @ColumnInfo(name = "previous_completed_at")
    val previousCompletedAt: Instant?,
) {
    companion object {
        fun noProgress(
            sessionId: String,
            cardId: String,
            position: Int,
        ) = RatingSnapshotEntity(
            sessionId = sessionId,
            cardId = cardId,
            position = position,
            hadProgress = false,
            stage = null,
            mastered = null,
            lastRating = null,
            lastReviewedAt = null,
            nextReviewAt = null,
            reviewCount = null,
            gotItCount = null,
            version = null,
            previousStatus = "ACTIVE",
            previousCompletedAt = null,
        )
    }
}

@Entity(tableName = "card_reports", indices = [Index(value = ["sync_state"])])
data class CardReportEntity(
    @PrimaryKey
    @ColumnInfo(name = "report_id")
    val reportId: String,
    @ColumnInfo(name = "card_id")
    val cardId: String,
    val reason: String,
    val details: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "sync_state")
    val syncState: String,
)
