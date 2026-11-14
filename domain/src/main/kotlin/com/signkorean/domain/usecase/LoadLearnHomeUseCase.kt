package com.signkorean.domain.usecase

import com.signkorean.domain.model.LearnHomeCategory
import com.signkorean.domain.model.LearnHomeDashboard
import com.signkorean.domain.model.LearnHomeDay
import com.signkorean.domain.model.LearnHomeSession
import com.signkorean.domain.port.input.LoadLearnHome
import com.signkorean.domain.port.output.CardCatalog
import com.signkorean.domain.port.output.ClockPort
import com.signkorean.domain.port.output.HomeActivityRepository
import com.signkorean.domain.port.output.SessionRepository
import com.signkorean.domain.port.output.UserSettingsRepository
import kotlinx.coroutines.flow.first

class LoadLearnHomeUseCase(
    private val userSettingsRepository: UserSettingsRepository,
    private val cardCatalog: CardCatalog,
    private val homeActivityRepository: HomeActivityRepository,
    private val sessionRepository: SessionRepository,
    private val clock: ClockPort,
) : LoadLearnHome {
    override suspend fun invoke(): LearnHomeDashboard {
        val settings = userSettingsRepository.settings.first()
        val now = clock.now()
        val activity = homeActivityRepository.loadHomeActivity(now)
        val activeSession = sessionRepository.getActiveSession()
        val cards = cardCatalog.getCards()
        val knownCardIds = cards.mapTo(mutableSetOf()) { it.id }
        val dueCount = activity.dueCardIds.count { it in knownCardIds }
        val newCardCount = (knownCardIds - activity.progressedCardIds).size
        val sessionLength = activeSession?.totalCount ?: settings.sessionLength
        val reviewCount = dueCount.coerceAtMost(sessionLength)
        val newCount = newCardCount.coerceAtMost(sessionLength - reviewCount)
        return LearnHomeDashboard(
            streakDays = ProgressStatistics.streak(activity.reviewedAt, now, clock.zoneId()),
            session =
                LearnHomeSession(
                    length = sessionLength,
                    estimatedMinutes = sessionLength.estimatedMinutes(),
                    reviewCount = reviewCount,
                    newCount = newCount,
                    activeSession = activeSession,
                ),
            categories = cards.categories(),
            weeklyActivity =
                ProgressStatistics
                    .lastSevenDays(activity.reviewedAt, now, clock.zoneId())
                    .map { LearnHomeDay(it.date, it.count) },
        )
    }

    private fun List<com.signkorean.domain.model.SignCard>.categories(): List<LearnHomeCategory> =
        CATEGORY_LABELS
            .map { (key, label) ->
                LearnHomeCategory(
                    key = key,
                    label = label,
                    cardCount = count { it.category == key },
                )
            }.filter { it.cardCount > 0 }

    private fun Int.estimatedMinutes(): Int =
        when (this) {
            5 -> 2
            10 -> 4
            20 -> 8
            else -> (this * MINUTES_PER_SIGN).toInt().coerceAtLeast(1)
        }

    private companion object {
        const val MINUTES_PER_SIGN = 0.4
        val CATEGORY_LABELS =
            listOf(
                "LANDMARK" to "Landmarks",
                "NEIGHBORHOOD" to "Neighborhoods",
                "MARKET" to "Markets",
                "TRANSPORT" to "Transport",
                "FOOD" to "Food",
                "SHOPPING" to "Shopping",
                "EVERYDAY" to "Everyday places",
                "NOTICE" to "Notices",
            )
    }
}
