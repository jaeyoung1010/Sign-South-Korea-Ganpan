package com.signkorean.app.di

import android.content.Context
import com.signkorean.data.asset.AndroidAssetSource
import com.signkorean.data.asset.BundleContentAdapter
import com.signkorean.data.repository.BootstrapSessionRepository
import com.signkorean.data.repository.BootstrapUserSettingsRepository
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
    fun provideUserSettingsRepository(): UserSettingsRepository = BootstrapUserSettingsRepository()

    @Provides
    @Singleton
    fun provideSessionRepository(): SessionRepository = BootstrapSessionRepository()

    @Provides
    @Singleton
    fun provideClockPort(): ClockPort = SystemClockAdapter()
}
