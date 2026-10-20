package com.signkorean.data.repository

import com.signkorean.data.db.RoomSessionTransactionStore
import com.signkorean.data.db.SessionCardEntity
import com.signkorean.data.db.SessionCreationCommand
import com.signkorean.data.db.SessionEntity
import com.signkorean.data.db.SignKoreanDatabase
import com.signkorean.domain.model.SessionToCreate
import com.signkorean.domain.port.output.ClockPort
import com.signkorean.domain.port.output.SessionCreationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class RoomSessionCreationRepository(
    database: SignKoreanDatabase,
    clock: ClockPort,
    private val dispatcher: CoroutineDispatcher,
) : SessionCreationRepository {
    private val transactionStore = RoomSessionTransactionStore(database, clock)

    override suspend fun save(
        session: SessionToCreate,
        replaceActiveSession: Boolean,
    ) {
        withContext(dispatcher) {
            val command =
                SessionCreationCommand(
                    session =
                        SessionEntity.active(
                            id = session.id,
                            requestedLength = session.requestedLength,
                            source = session.source.name,
                            category = session.category,
                            now = session.createdAt,
                        ),
                    cards =
                        session.cards.map { card ->
                            SessionCardEntity(
                                sessionId = session.id,
                                position = card.position,
                                cardId = card.cardId,
                                source = card.source.name,
                            )
                        },
                )
            if (replaceActiveSession) {
                transactionStore.replaceActiveSession(command)
            } else {
                transactionStore.createSession(command)
            }
        }
    }
}
