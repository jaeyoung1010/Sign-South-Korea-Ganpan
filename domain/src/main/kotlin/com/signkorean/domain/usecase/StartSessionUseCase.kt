package com.signkorean.domain.usecase

import com.signkorean.domain.model.SessionCardSource
import com.signkorean.domain.model.SessionCardToCreate
import com.signkorean.domain.model.SessionStartedEvent
import com.signkorean.domain.model.SessionToCreate
import com.signkorean.domain.port.input.StartSession
import com.signkorean.domain.port.input.StartSessionRequest
import com.signkorean.domain.port.input.StartSessionResult
import com.signkorean.domain.port.output.AnalyticsSink
import com.signkorean.domain.port.output.CardCatalog
import com.signkorean.domain.port.output.ClockPort
import com.signkorean.domain.port.output.HomeActivityRepository
import com.signkorean.domain.port.output.SessionCreationRepository
import com.signkorean.domain.port.output.SessionIdGenerator
import com.signkorean.domain.port.output.SessionRepository
import com.signkorean.domain.port.output.UserSettingsRepository
import com.signkorean.domain.session.SessionBuildRequest
import com.signkorean.domain.session.SessionBuilder
import com.signkorean.domain.session.SessionCandidate
import com.signkorean.domain.session.SessionType
import kotlinx.coroutines.flow.first

class StartSessionUseCase(
    private val userSettingsRepository: UserSettingsRepository,
    private val cardCatalog: CardCatalog,
    private val homeActivityRepository: HomeActivityRepository,
    private val sessionRepository: SessionRepository,
    private val sessionCreationRepository: SessionCreationRepository,
    private val sessionIdGenerator: SessionIdGenerator,
    private val sessionBuilder: SessionBuilder,
    private val analyticsSink: AnalyticsSink,
    private val clock: ClockPort,
) : StartSession {
    override suspend fun invoke(request: StartSessionRequest): StartSessionResult {
        val activeSession = sessionRepository.getActiveSession()
        if (activeSession != null) {
            if (request.type == SessionType.DAILY) return StartSessionResult.Resumed(activeSession)
            if (!request.replaceActiveSession) return StartSessionResult.ReplacementRequired(activeSession)
        }

        val settings = userSettingsRepository.settings.first()
        val now = clock.now()
        val activity = homeActivityRepository.loadHomeActivity(now)
        val candidates =
            cardCatalog.getCards().map { card ->
                SessionCandidate(
                    cardId = card.id,
                    order = card.order,
                    category = card.category,
                    isDue = card.id in activity.dueCardIds,
                    isNew = card.id !in activity.progressedCardIds,
                )
            }
        val plan =
            sessionBuilder.build(
                request.toBuildRequest(
                    length = settings.sessionLength,
                    candidates = candidates,
                    interests = settings.interests,
                    level = settings.level,
                ),
            )
        if (plan.cardIds.isEmpty()) return StartSessionResult.NoCardsAvailable

        val candidatesById = candidates.associateBy(SessionCandidate::cardId)
        val cards =
            plan.cardIds.mapIndexed { position, cardId ->
                val candidate = checkNotNull(candidatesById[cardId]) { "Missing candidate: $cardId" }
                SessionCardToCreate(
                    position = position,
                    cardId = cardId,
                    source = if (candidate.isDue) SessionCardSource.REVIEW else SessionCardSource.NEW,
                )
            }
        val session =
            SessionToCreate(
                id = sessionIdGenerator.nextId(),
                requestedLength = cards.size,
                source = request.type,
                category = request.category,
                createdAt = now,
                cards = cards,
            )
        sessionCreationRepository.save(session, replaceActiveSession = request.replaceActiveSession)
        // The persisted session is the source of truth; observability must not make a successful
        // transaction appear to fail to the learner.
        runCatching {
            analyticsSink.recordSessionStarted(
                SessionStartedEvent(
                    source = request.type,
                    dueCount = cards.count { it.source == SessionCardSource.REVIEW },
                    newCount = cards.count { it.source == SessionCardSource.NEW },
                    category = request.category,
                ),
            )
        }
        return StartSessionResult.Started(session.id)
    }

    private fun StartSessionRequest.toBuildRequest(
        length: Int,
        candidates: List<SessionCandidate>,
        interests: Set<com.signkorean.domain.model.Interest>,
        level: com.signkorean.domain.model.LearningLevel,
    ): SessionBuildRequest =
        when (type) {
            SessionType.DAILY -> SessionBuildRequest.daily(length, candidates, interests, level, seed())
            SessionType.CATEGORY -> SessionBuildRequest.category(length, checkNotNull(category), candidates, interests, level, seed())
            SessionType.PRACTICE -> SessionBuildRequest.practice(checkNotNull(practiceCardId), candidates, seed())
        }

    private fun StartSessionRequest.seed(): Long =
        listOf(type.name, category.orEmpty(), practiceCardId.orEmpty()).joinToString(":").hashCode().toLong()
}
