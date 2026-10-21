package com.signkorean.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import com.signkorean.data.asset.AndroidAssetSource
import com.signkorean.data.asset.BundleContentAdapter
import com.signkorean.data.db.RoomContentMigrationRepository
import com.signkorean.data.db.RoomSessionTransactionStore
import com.signkorean.data.db.SignKoreanDatabase
import com.signkorean.data.observability.PrivacySafeAnalyticsAdapter
import com.signkorean.data.repository.RoomCardReportRepository
import com.signkorean.data.repository.RoomHomeActivityRepository
import com.signkorean.data.repository.RoomSessionRepository
import com.signkorean.data.settings.AndroidSettingsDiagnostics
import com.signkorean.data.settings.AndroidSystemReduceMotionSource
import com.signkorean.data.settings.ProtoUserSettingsRepository
import com.signkorean.data.settings.SettingsDiagnostics
import com.signkorean.data.settings.SystemReduceMotionSource
import com.signkorean.data.settings.UserSettingsDataStoreFactory
import com.signkorean.data.settings.proto.UserSettingsProto
import com.signkorean.data.time.SystemClockAdapter
import com.signkorean.app.legal.BundledPolicyDocumentRepository
import com.signkorean.domain.port.output.AnalyticsPort
import com.signkorean.domain.port.output.AppLocalePort
import com.signkorean.domain.port.output.AnalyticsSink
import com.signkorean.domain.port.output.CardCatalog
import com.signkorean.domain.port.output.CardReportIdGenerator
import com.signkorean.domain.port.output.CardReportRepository
import com.signkorean.domain.port.output.ClockPort
import com.signkorean.domain.port.output.ContentMigrationRepository
import com.signkorean.domain.port.output.DiagnosticLoggerPort
import com.signkorean.domain.port.output.HomeActivityRepository
import com.signkorean.domain.port.output.SessionRepository
import com.signkorean.domain.port.output.UserSettingsRepository
import com.signkorean.domain.port.output.PolicyDocumentRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.util.UUID
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataAdapterModule {
    @Provides
    @Singleton
    fun providePolicyDocumentRepository(
        @ApplicationContext context: Context,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): PolicyDocumentRepository = BundledPolicyDocumentRepository(context, dispatcher)

    @Provides
    @Singleton
    fun provideCardCatalog(
        @ApplicationContext context: Context,
    ): CardCatalog = BundleContentAdapter(AndroidAssetSource(context.assets))

    @Provides
    @Singleton
    fun provideUserSettingsDataStore(
        @ApplicationContext context: Context,
        diagnostics: SettingsDiagnostics,
    ): DataStore<UserSettingsProto> =
        UserSettingsDataStoreFactory.create(
            file = context.filesDir.resolve("datastore/user_settings.pb"),
            diagnostics = diagnostics,
        )

    @Provides
    @Singleton
    fun provideSettingsDiagnostics(): SettingsDiagnostics = AndroidSettingsDiagnostics()

    @Provides
    @Singleton
    fun provideSystemReduceMotionSource(): SystemReduceMotionSource = AndroidSystemReduceMotionSource()

    @Provides
    @Singleton
    fun provideUserSettingsRepository(
        dataStore: DataStore<UserSettingsProto>,
        systemReduceMotionSource: SystemReduceMotionSource,
        appLocalePort: AppLocalePort,
    ): UserSettingsRepository = ProtoUserSettingsRepository(dataStore, systemReduceMotionSource, appLocalePort)

    @Provides
    @Singleton
    fun provideSessionRepository(
        database: SignKoreanDatabase,
        @IoDispatcher dispatcher: CoroutineDispatcher,
        clock: ClockPort,
    ): SessionRepository = RoomSessionRepository(database, dispatcher, clock)

    @Provides
    @Singleton
    fun provideHomeActivityRepository(
        database: SignKoreanDatabase,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): HomeActivityRepository = RoomHomeActivityRepository(database, dispatcher)

    @Provides
    @Singleton
    fun provideContentMigrationRepository(
        database: SignKoreanDatabase,
        @IoDispatcher dispatcher: CoroutineDispatcher,
        clock: ClockPort,
    ): ContentMigrationRepository = RoomContentMigrationRepository(database, dispatcher, clock)

    @Provides
    @Singleton
    fun provideCardReportRepository(
        database: SignKoreanDatabase,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): CardReportRepository = RoomCardReportRepository(database, dispatcher)

    @Provides
    @Singleton
    fun provideCardReportIdGenerator(): CardReportIdGenerator = CardReportIdGenerator { UUID.randomUUID().toString() }

    @Provides
    @Singleton
    fun provideAnalyticsSink(diagnosticLogger: DiagnosticLoggerPort): AnalyticsSink =
        PrivacySafeAnalyticsAdapter(diagnosticLogger)

    @Provides
    @Singleton
    fun provideAnalyticsPort(analyticsSink: AnalyticsSink): AnalyticsPort = analyticsSink

    @Provides
    @Singleton
    fun provideClockPort(): ClockPort = SystemClockAdapter()

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
