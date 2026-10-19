package com.signkorean.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CardProgressEntity::class,
        SessionEntity::class,
        SessionCardEntity::class,
        RatingSnapshotEntity::class,
        CardReportEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(RoomTimeConverters::class)
abstract class SignKoreanDatabase : RoomDatabase() {
    abstract fun cardProgressDao(): CardProgressDao

    abstract fun sessionDao(): SessionDao

    abstract fun sessionCardDao(): SessionCardDao

    abstract fun ratingSnapshotDao(): RatingSnapshotDao

    abstract fun cardReportDao(): CardReportDao

    companion object {
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `rating_snapshot` (
                            `session_id` TEXT NOT NULL,
                            `card_id` TEXT NOT NULL,
                            `position` INTEGER NOT NULL,
                            `had_progress` INTEGER NOT NULL,
                            `stage` INTEGER,
                            `mastered` INTEGER,
                            `last_rating` TEXT,
                            `last_reviewed_at` INTEGER,
                            `next_review_at` INTEGER,
                            `review_count` INTEGER,
                            `got_it_count` INTEGER,
                            `version` INTEGER,
                            `previous_status` TEXT NOT NULL,
                            `previous_completed_at` INTEGER,
                            PRIMARY KEY(`session_id`),
                            FOREIGN KEY(`session_id`) REFERENCES `sessions`(`session_id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                }
            }
    }
}
