package com.signkorean.app.di

import com.signkorean.domain.port.input.ResolveLaunchDestination
import com.signkorean.domain.port.output.SessionRepository
import com.signkorean.domain.port.output.UserSettingsRepository
import com.signkorean.domain.usecase.ResolveLaunchDestinationUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    fun provideResolveLaunchDestination(
        userSettingsRepository: UserSettingsRepository,
        sessionRepository: SessionRepository,
    ): ResolveLaunchDestination =
        ResolveLaunchDestinationUseCase(
            userSettingsRepository = userSettingsRepository,
            sessionRepository = sessionRepository,
        )
}
