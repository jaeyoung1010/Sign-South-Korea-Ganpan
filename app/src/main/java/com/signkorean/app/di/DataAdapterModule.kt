package com.signkorean.app.di

import com.signkorean.data.repository.BootstrapSessionRepository
import com.signkorean.data.repository.BootstrapUserSettingsRepository
import com.signkorean.domain.port.output.SessionRepository
import com.signkorean.domain.port.output.UserSettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataAdapterModule {
    @Provides
    @Singleton
    fun provideUserSettingsRepository(): UserSettingsRepository = BootstrapUserSettingsRepository()

    @Provides
    @Singleton
    fun provideSessionRepository(): SessionRepository = BootstrapSessionRepository()
}
