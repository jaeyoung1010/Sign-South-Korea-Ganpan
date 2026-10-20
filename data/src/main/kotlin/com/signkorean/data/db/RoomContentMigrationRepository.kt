package com.signkorean.data.db

import com.signkorean.domain.model.AppContent
import com.signkorean.domain.model.ContentMigrationResult
import com.signkorean.domain.port.output.ClockPort
import com.signkorean.domain.port.output.ContentMigrationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class RoomContentMigrationRepository(
    private val database: SignKoreanDatabase,
    private val dispatcher: CoroutineDispatcher,
    private val clock: ClockPort,
) : ContentMigrationRepository {
    override suspend fun migrate(content: AppContent): ContentMigrationResult =
        withContext(dispatcher) {
            database.runInTransaction<ContentMigrationResult> {
                val contentState = database.contentStateDao().current()
                if (contentState?.contentVersion == content.version) {
                    return@runInTransaction ContentMigrationResult(
                        migrated = false,
                        activeSessionInvalidated = false,
                    )
                }

                if (contentState == null) {
                    database.contentStateDao().upsert(ContentStateEntity(contentVersion = content.version))
                    return@runInTransaction ContentMigrationResult(
                        migrated = false,
                        activeSessionInvalidated = false,
                    )
                }

                database.cardProgressDao().deleteNotIn(content.cardIds.toList())
                val invalidatedActiveSession = database.invalidateActiveSessionIfNeeded(content.cardIds)
                database.contentStateDao().upsert(ContentStateEntity(contentVersion = content.version))
                ContentMigrationResult(
                    migrated = true,
                    activeSessionInvalidated = invalidatedActiveSession,
                )
            }
        }

    private fun SignKoreanDatabase.invalidateActiveSessionIfNeeded(validCardIds: Set<String>): Boolean {
        val session = sessionDao().active() ?: return false
        val containsUnknownCard = sessionCardDao().forSession(session.sessionId).any { it.cardId !in validCardIds }
        if (!containsUnknownCard) return false

        sessionDao().update(
            session.copy(
                status = "ABANDONED",
                updatedAt = clock.now(),
            ),
        )
        return true
    }
}
