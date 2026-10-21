package com.signkorean.data.db

import androidx.room.Dao
import androidx.room.ColumnInfo
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CardProgressDao {
    @Query("SELECT * FROM card_progress WHERE card_id = :cardId")
    fun get(cardId: String): CardProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(progress: CardProgressEntity)

    @Query("DELETE FROM card_progress WHERE card_id = :cardId")
    fun delete(cardId: String)

    @Query("DELETE FROM card_progress WHERE card_id NOT IN (:cardIds)")
    fun deleteNotIn(cardIds: List<String>)

    @Query("SELECT COUNT(*) FROM card_progress")
    fun count(): Int

    @Query("SELECT * FROM card_progress")
    fun all(): List<CardProgressEntity>

    @Query("SELECT * FROM card_progress ORDER BY card_id")
    fun observeAll(): Flow<List<CardProgressEntity>>
}

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(session: SessionEntity)

    @Update
    fun update(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE session_id = :sessionId")
    fun get(sessionId: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE status = 'ACTIVE' LIMIT 1")
    fun active(): SessionEntity?

    @Query("SELECT * FROM sessions WHERE status IN ('ACTIVE', 'PAUSED') ORDER BY updated_at DESC LIMIT 1")
    fun resumable(): SessionEntity?

    @Query("UPDATE sessions SET status = 'ABANDONED', updated_at = :updatedAt WHERE status IN ('ACTIVE', 'PAUSED')")
    fun abandonActive(updatedAt: java.time.Instant)
}

@Dao
interface SessionCardDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertAll(cards: List<SessionCardEntity>)

    @Query("SELECT * FROM session_cards WHERE session_id = :sessionId AND position = :position")
    fun get(
        sessionId: String,
        position: Int,
    ): SessionCardEntity?

    @Query("SELECT * FROM session_cards WHERE session_id = :sessionId ORDER BY position")
    fun forSession(sessionId: String): List<SessionCardEntity>

    @Query("SELECT COUNT(*) FROM session_cards WHERE session_id = :sessionId")
    fun countForSession(sessionId: String): Int

    @Query("DELETE FROM session_cards WHERE session_id = :sessionId AND position = :position")
    fun delete(sessionId: String, position: Int): Int

    @Query(
        """
        UPDATE session_cards
        SET position = :newPosition
        WHERE session_id = :sessionId
          AND position = :oldPosition
        """,
    )
    fun move(
        sessionId: String,
        oldPosition: Int,
        newPosition: Int,
    ): Int

    @Query("SELECT rated_at FROM session_cards WHERE rated_at IS NOT NULL")
    fun ratedAt(): List<java.time.Instant>

    @Query(
        """
        SELECT current_card.card_id, current_card.rated_at
        FROM session_cards AS current_card
        WHERE current_card.rating = 'REVIEW'
          AND current_card.rated_at IS NOT NULL
          AND NOT EXISTS (
              SELECT 1
              FROM session_cards AS later_card
              WHERE later_card.card_id = current_card.card_id
                AND later_card.rating IN ('GOT_IT', 'REVIEW')
                AND later_card.rated_at IS NOT NULL
                AND (
                    later_card.rated_at > current_card.rated_at
                    OR (
                        later_card.rated_at = current_card.rated_at
                        AND (
                            later_card.session_id > current_card.session_id
                            OR (
                                later_card.session_id = current_card.session_id
                                AND later_card.position > current_card.position
                            )
                        )
                    )
                )
          )
        ORDER BY rated_at DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    fun observeRecentReviews(
        limit: Int,
        offset: Int,
    ): Flow<List<ReviewCardRow>>

    @Query(
        """
        SELECT card_id, rating, rated_at
        FROM session_cards
        WHERE rating IN ('GOT_IT', 'REVIEW') AND rated_at IS NOT NULL
        ORDER BY rated_at DESC
        """,
    )
    fun observeRatingActivity(): Flow<List<RatingActivityRow>>

    @Query(
        """
        UPDATE session_cards
        SET answer_revealed_at = :revealedAt
        WHERE session_id = :sessionId
          AND position = :position
          AND card_id = :cardId
          AND answer_revealed_at IS NULL
        """,
    )
    fun markAnswerRevealed(
        sessionId: String,
        position: Int,
        cardId: String,
        revealedAt: java.time.Instant,
    ): Int

    @Update
    fun update(card: SessionCardEntity)
}

data class ReviewCardRow(
    @ColumnInfo(name = "card_id") val cardId: String,
    @ColumnInfo(name = "rated_at") val ratedAt: java.time.Instant,
)

data class RatingActivityRow(
    @ColumnInfo(name = "card_id") val cardId: String,
    val rating: String,
    @ColumnInfo(name = "rated_at") val ratedAt: java.time.Instant,
)

@Dao
interface RatingSnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(snapshot: RatingSnapshotEntity)

    @Query("SELECT * FROM rating_snapshot WHERE session_id = :sessionId")
    fun forSession(sessionId: String): RatingSnapshotEntity?

    @Query("DELETE FROM rating_snapshot WHERE session_id = :sessionId")
    fun delete(sessionId: String)
}

@Dao
interface CardReportDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(report: CardReportEntity)

    @Query("SELECT * FROM card_reports WHERE sync_state IN ('LOCAL_ONLY', 'PENDING', 'FAILED') ORDER BY created_at")
    fun observePending(): Flow<List<CardReportEntity>>

    @Query("SELECT COUNT(*) FROM card_reports")
    fun count(): Int

    @Query("SELECT COUNT(*) FROM card_reports WHERE report_id = :reportId")
    fun count(reportId: String): Int
}

@Dao
interface ContentStateDao {
    @Query("SELECT * FROM content_state WHERE id = 1")
    fun current(): ContentStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(state: ContentStateEntity)
}
