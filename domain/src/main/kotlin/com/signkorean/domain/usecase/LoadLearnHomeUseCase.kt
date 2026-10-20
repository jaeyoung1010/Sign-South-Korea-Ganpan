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
import java.time.LocalDate

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
        val today = now.atZone(clock.zoneId()).toLocalDate()

        return LearnHomeDashboard(
            streakDays = activity.reviewedAt.streakEndingOn(today),
            session =
                LearnHomeSession(
                    length = sessionLength,
                    estimatedMinutes = sessionLength.estimatedMinutes(),
                    reviewCount = reviewCount,
                    newCount = newCount,
                    activeSession = activeSession,
                ),
            categories = cards.categories(),
            weeklyActivity = activity.reviewedAt.lastSevenDaysEndingOn(today),
        )
    }

    private fun List<com.signkorean.domain.model.SignCard>.categories(): List<LearnHomeCategory> =
        CATEGORY_LABELS.map { (key, label) ->
            LearnHomeCategory(
                key = key,
                label = label,
                cardCount = count { it.category == key },
            )
        }

    private fun List<java.time.Instant>.streakEndingOn(today: LocalDate): Int {
        val activityDays = map { it.atZone(clock.zoneId()).toLocalDate() }.toSet()
        var date = today
        var streak = 0
        while (date in activityDays) {
            streak += 1
            date = date.minusDays(1)
        }
        return streak
    }

    private fun List<java.time.Instant>.lastSevenDaysEndingOn(today: LocalDate): List<LearnHomeDay> =
        (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            LearnHomeDay(date, count { it.atZone(clock.zoneId()).toLocalDate() == date })
        }

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
            )
    }
}
