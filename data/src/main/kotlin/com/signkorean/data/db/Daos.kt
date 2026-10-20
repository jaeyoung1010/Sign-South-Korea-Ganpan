package com.signkorean.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

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

    @Query("UPDATE sessions SET status = 'ABANDONED', updated_at = :updatedAt WHERE status = 'ACTIVE'")
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

    @Query("SELECT rated_at FROM session_cards WHERE rated_at IS NOT NULL")
    fun ratedAt(): List<java.time.Instant>

    @Update
    fun update(card: SessionCardEntity)
}

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

    @Query("SELECT COUNT(*) FROM card_reports")
    fun count(): Int
}

@Dao
interface ContentStateDao {
    @Query("SELECT * FROM content_state WHERE id = 1")
    fun current(): ContentStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(state: ContentStateEntity)
}
