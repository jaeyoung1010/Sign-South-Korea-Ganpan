package com.signkorean.app.di

import com.signkorean.domain.port.input.CompleteOnboarding
import com.signkorean.domain.port.input.InitializeApp
import com.signkorean.domain.port.input.LoadLearnHome
import com.signkorean.domain.port.input.ResolveLaunchDestination
import com.signkorean.domain.port.input.StartOnboarding
import com.signkorean.domain.port.output.AnalyticsSink
import com.signkorean.domain.port.output.CardCatalog
import com.signkorean.domain.port.output.ClockPort
import com.signkorean.domain.port.output.ContentMigrationRepository
import com.signkorean.domain.port.output.HomeActivityRepository
import com.signkorean.domain.port.output.SessionRepository
import com.signkorean.domain.port.output.UserSettingsRepository
import com.signkorean.domain.usecase.CompleteOnboardingUseCase
import com.signkorean.domain.usecase.InitializeAppUseCase
import com.signkorean.domain.usecase.LoadLearnHomeUseCase
import com.signkorean.domain.usecase.ResolveLaunchDestinationUseCase
import com.signkorean.domain.usecase.StartOnboardingUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    fun provideInitializeApp(
        userSettingsRepository: UserSettingsRepository,
        sessionRepository: SessionRepository,
        cardCatalog: CardCatalog,
        contentMigrationRepository: ContentMigrationRepository,
        analyticsSink: AnalyticsSink,
    ): InitializeApp =
        InitializeAppUseCase(
            userSettingsRepository = userSettingsRepository,
            sessionRepository = sessionRepository,
            cardCatalog = cardCatalog,
            contentMigrationRepository = contentMigrationRepository,
            analyticsSink = analyticsSink,
        )

    @Provides
    fun provideResolveLaunchDestination(
        userSettingsRepository: UserSettingsRepository,
        sessionRepository: SessionRepository,
    ): ResolveLaunchDestination =
        ResolveLaunchDestinationUseCase(
            userSettingsRepository = userSettingsRepository,
            sessionRepository = sessionRepository,
        )

    @Provides
    fun provideStartOnboarding(analyticsSink: AnalyticsSink): StartOnboarding = StartOnboardingUseCase(analyticsSink)

    @Provides
    fun provideCompleteOnboarding(
        userSettingsRepository: UserSettingsRepository,
        analyticsSink: AnalyticsSink,
    ): CompleteOnboarding = CompleteOnboardingUseCase(userSettingsRepository, analyticsSink)

    @Provides
    fun provideLoadLearnHome(
        userSettingsRepository: UserSettingsRepository,
        cardCatalog: CardCatalog,
        homeActivityRepository: HomeActivityRepository,
        sessionRepository: SessionRepository,
        clock: ClockPort,
    ): LoadLearnHome =
        LoadLearnHomeUseCase(
            userSettingsRepository = userSettingsRepository,
            cardCatalog = cardCatalog,
            homeActivityRepository = homeActivityRepository,
            sessionRepository = sessionRepository,
            clock = clock,
        )
}
