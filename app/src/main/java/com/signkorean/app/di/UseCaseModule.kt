package com.signkorean.app.di

import com.signkorean.domain.port.input.CompleteOnboarding
import com.signkorean.domain.port.input.InitializeApp
import com.signkorean.domain.port.input.LoadLearnHome
import com.signkorean.domain.port.input.LoadLearnSession
import com.signkorean.domain.port.input.LoadSessionCompletion
import com.signkorean.domain.port.input.PauseSession
import com.signkorean.domain.port.input.RateCard
import com.signkorean.domain.port.input.ResolveLaunchDestination
import com.signkorean.domain.port.input.ResumeSession
import com.signkorean.domain.port.input.RevealAnswer
import com.signkorean.domain.port.input.StartOnboarding
import com.signkorean.domain.port.input.StartSession
import com.signkorean.domain.port.input.UndoRating
import com.signkorean.domain.port.output.AnalyticsSink
import com.signkorean.domain.port.output.CardCatalog
import com.signkorean.domain.port.output.ClockPort
import com.signkorean.domain.port.output.ContentMigrationRepository
import com.signkorean.domain.port.output.HomeActivityRepository
import com.signkorean.domain.port.output.LearningRepository
import com.signkorean.domain.port.output.SessionCreationRepository
import com.signkorean.domain.port.output.SessionIdGenerator
import com.signkorean.domain.port.output.SessionRepository
import com.signkorean.domain.port.output.UserSettingsRepository
import com.signkorean.domain.session.SessionBuilder
import com.signkorean.domain.usecase.CompleteOnboardingUseCase
import com.signkorean.domain.usecase.InitializeAppUseCase
import com.signkorean.domain.usecase.LoadLearnHomeUseCase
import com.signkorean.domain.usecase.LoadLearnSessionUseCase
import com.signkorean.domain.usecase.LoadSessionCompletionUseCase
import com.signkorean.domain.usecase.PauseSessionUseCase
import com.signkorean.domain.usecase.RateCardUseCase
import com.signkorean.domain.usecase.ResolveLaunchDestinationUseCase
import com.signkorean.domain.usecase.ResumeSessionUseCase
import com.signkorean.domain.usecase.RevealAnswerUseCase
import com.signkorean.domain.usecase.StartOnboardingUseCase
import com.signkorean.domain.usecase.StartSessionUseCase
import com.signkorean.domain.usecase.UndoRatingUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
@Suppress("TooManyFunctions")
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
    fun provideSessionBuilder(): SessionBuilder = SessionBuilder()

    @Provides
    fun provideStartSession(
        userSettingsRepository: UserSettingsRepository,
        cardCatalog: CardCatalog,
        homeActivityRepository: HomeActivityRepository,
        sessionRepository: SessionRepository,
        sessionCreationRepository: SessionCreationRepository,
        sessionIdGenerator: SessionIdGenerator,
        sessionBuilder: SessionBuilder,
        analyticsSink: AnalyticsSink,
        clock: ClockPort,
    ): StartSession =
        StartSessionUseCase(
            userSettingsRepository = userSettingsRepository,
            cardCatalog = cardCatalog,
            homeActivityRepository = homeActivityRepository,
            sessionRepository = sessionRepository,
            sessionCreationRepository = sessionCreationRepository,
            sessionIdGenerator = sessionIdGenerator,
            sessionBuilder = sessionBuilder,
            analyticsSink = analyticsSink,
            clock = clock,
        )

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

    @Provides
    fun provideLoadLearnSession(
        sessionRepository: SessionRepository,
        cardCatalog: CardCatalog,
        userSettingsRepository: UserSettingsRepository,
    ): LoadLearnSession = LoadLearnSessionUseCase(sessionRepository, cardCatalog, userSettingsRepository)

    @Provides
    fun provideLoadSessionCompletion(sessionRepository: SessionRepository): LoadSessionCompletion =
        LoadSessionCompletionUseCase(sessionRepository)

    @Provides
    fun provideRevealAnswer(
        sessionRepository: SessionRepository,
        analyticsSink: AnalyticsSink,
        clock: ClockPort,
    ): RevealAnswer = RevealAnswerUseCase(sessionRepository, analyticsSink, clock)

    @Provides
    fun provideRateCard(
        learningRepository: LearningRepository,
        analyticsSink: AnalyticsSink,
        clock: ClockPort,
    ): RateCard = RateCardUseCase(learningRepository, analyticsSink, clock)

    @Provides
    fun provideUndoRating(
        learningRepository: LearningRepository,
        analyticsSink: AnalyticsSink,
    ): UndoRating = UndoRatingUseCase(learningRepository, analyticsSink)

    @Provides
    fun providePauseSession(
        sessionRepository: SessionRepository,
        analyticsSink: AnalyticsSink,
    ): PauseSession = PauseSessionUseCase(sessionRepository, analyticsSink)

    @Provides
    fun provideResumeSession(
        sessionRepository: SessionRepository,
        analyticsSink: AnalyticsSink,
    ): ResumeSession = ResumeSessionUseCase(sessionRepository, analyticsSink)
}
