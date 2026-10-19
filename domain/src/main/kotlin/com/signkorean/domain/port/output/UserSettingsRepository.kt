package com.signkorean.domain.port.output

import com.signkorean.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface UserSettingsRepository {
    val settings: Flow<UserSettings>

    /**
     * Atomically applies a change to the most recently persisted settings.
     *
     * Callers changing one preference should prefer this form so concurrent changes to other
     * preferences are retained.
     */
    suspend fun updateSettings(transform: (UserSettings) -> UserSettings): UserSettings
}
