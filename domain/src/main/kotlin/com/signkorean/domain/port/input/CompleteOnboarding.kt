package com.signkorean.domain.port.input

import com.signkorean.domain.model.Interest
import com.signkorean.domain.model.LearningLevel
import com.signkorean.domain.model.ProfileAvatar
import com.signkorean.domain.model.UserSettings

fun interface CompleteOnboarding {
    suspend operator fun invoke(
        level: LearningLevel,
        interests: Set<Interest>,
        profileName: String,
        profileAvatar: ProfileAvatar,
    ): UserSettings
}
