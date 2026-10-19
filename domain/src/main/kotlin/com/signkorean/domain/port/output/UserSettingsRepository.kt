package com.signkorean.domain.port.output

import com.signkorean.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface UserSettingsRepository {
    val settings: Flow<UserSettings>

    suspend fun updateSettings(settings: UserSettings): UserSettings
}
