package com.signkorean.domain.port.input

import com.signkorean.domain.model.Interest
import com.signkorean.domain.model.LearningLevel
import com.signkorean.domain.model.UserSettings

fun interface CompleteOnboarding {
    suspend operator fun invoke(
        level: LearningLevel,
        interests: Set<Interest>,
    ): UserSettings
}
