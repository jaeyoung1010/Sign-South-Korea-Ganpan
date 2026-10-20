package com.signkorean.domain.session

/** Builds a deterministic, duplicate-free session without depending on persistence or Android. */
class SessionBuilder(
    private val candidateScorer: CandidateScorer = CandidateScorer(),
) {
    fun build(request: SessionBuildRequest): SessionPlan {
        val candidates = coalesceByCardId(request.candidates)
        val selected =
            when (request.type) {
                SessionType.DAILY -> buildDaily(request, candidates)
                SessionType.CATEGORY -> buildCategory(request, candidates)
                SessionType.PRACTICE -> buildPractice(request, candidates)
            }

        return SessionPlan(cardIds = selected.map(SessionCandidate::cardId))
    }

    private fun buildDaily(
        request: SessionBuildRequest,
        candidates: List<SessionCandidate>,
    ): List<SessionCandidate> {
        val due = candidates.filter(SessionCandidate::isDue)
        val selectedDue = prioritize(request, due).take(maxDueCount(request.length))
        val selectedIds = selectedDue.mapTo(mutableSetOf(), SessionCandidate::cardId)
        val nonDue = candidates.filterNot { it.cardId in selectedIds || it.isDue }

        return selectedDue + prioritize(request, nonDue).take(request.length - selectedDue.size)
    }

    private fun buildCategory(
        request: SessionBuildRequest,
        candidates: List<SessionCandidate>,
    ): List<SessionCandidate> {
        val prioritized = prioritize(request, candidates)
        val categoryCards = prioritized.filter { it.category == request.category }
        val fallbackCards = prioritized.filter { it.category != request.category }

        return (categoryCards + fallbackCards).take(request.length)
    }

    private fun buildPractice(
        request: SessionBuildRequest,
        candidates: List<SessionCandidate>,
    ): List<SessionCandidate> = candidates.filter { it.cardId == request.practiceCardId }.take(1)

    private fun prioritize(
        request: SessionBuildRequest,
        candidates: Collection<SessionCandidate>,
    ): List<SessionCandidate> =
        candidateScorer.prioritize(
            candidates = candidates,
            interests = request.interests,
            level = request.level,
            seed = request.seed,
        )

    private fun coalesceByCardId(candidates: List<SessionCandidate>): List<SessionCandidate> =
        candidates
            .groupBy(SessionCandidate::cardId)
            .map { (_, matchingCandidates) ->
                matchingCandidates.reduce { retained, duplicate ->
                    retained.copy(
                        isDue = retained.isDue || duplicate.isDue,
                        isNew = retained.isNew && duplicate.isNew,
                    )
                }
            }

    private fun maxDueCount(length: Int): Int = length * MAX_DUE_PERCENT / 100

    private companion object {
        const val MAX_DUE_PERCENT = 70
    }
}
