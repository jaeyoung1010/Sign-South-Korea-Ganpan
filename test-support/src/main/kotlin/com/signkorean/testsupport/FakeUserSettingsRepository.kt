package com.signkorean.testsupport

import com.signkorean.domain.model.UserSettings
import com.signkorean.domain.port.output.UserSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeUserSettingsRepository(
    initialSettings: UserSettings = UserSettings(),
) : UserSettingsRepository {
    private val mutableSettings = MutableStateFlow(initialSettings)

    override val settings: Flow<UserSettings> = mutableSettings.asStateFlow()

    override suspend fun updateSettings(settings: UserSettings): UserSettings = settings.also { mutableSettings.value = it }
}
