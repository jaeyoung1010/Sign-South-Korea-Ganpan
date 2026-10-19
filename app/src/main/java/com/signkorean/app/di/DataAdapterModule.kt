package com.signkorean.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import com.signkorean.data.asset.AndroidAssetSource
import com.signkorean.data.asset.BundleContentAdapter
import com.signkorean.data.repository.BootstrapSessionRepository
import com.signkorean.data.settings.AndroidSettingsDiagnostics
import com.signkorean.data.settings.AndroidSystemReduceMotionSource
import com.signkorean.data.settings.ProtoUserSettingsRepository
import com.signkorean.data.settings.SettingsDiagnostics
import com.signkorean.data.settings.SystemReduceMotionSource
import com.signkorean.data.settings.UserSettingsDataStoreFactory
import com.signkorean.data.settings.proto.UserSettingsProto
import com.signkorean.data.time.SystemClockAdapter
import com.signkorean.domain.port.output.CardCatalog
import com.signkorean.domain.port.output.ClockPort
import com.signkorean.domain.port.output.SessionRepository
import com.signkorean.domain.port.output.UserSettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataAdapterModule {
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
    ): UserSettingsRepository = ProtoUserSettingsRepository(dataStore, systemReduceMotionSource)

    @Provides
    @Singleton
    fun provideSessionRepository(): SessionRepository = BootstrapSessionRepository()

    @Provides
    @Singleton
    fun provideClockPort(): ClockPort = SystemClockAdapter()
}
