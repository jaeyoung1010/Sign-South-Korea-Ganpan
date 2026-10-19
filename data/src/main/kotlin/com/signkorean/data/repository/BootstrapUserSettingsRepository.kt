package com.signkorean.data.repository

import com.signkorean.domain.model.UserSettings
import com.signkorean.domain.port.output.UserSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BootstrapUserSettingsRepository(
    initialSettings: UserSettings = UserSettings(),
) : UserSettingsRepository {
    private val mutableSettings = MutableStateFlow(initialSettings)
    private val updateMutex = Mutex()

    override val settings: Flow<UserSettings> = mutableSettings.asStateFlow()

    override suspend fun updateSettings(transform: (UserSettings) -> UserSettings): UserSettings =
        updateMutex.withLock {
            transform(mutableSettings.value).also { mutableSettings.value = it }
        }
}
