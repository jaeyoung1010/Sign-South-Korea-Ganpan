package com.signkorean.app.di

import android.content.Context
import com.signkorean.domain.port.output.AudioPlayerPort
import com.signkorean.domain.port.output.PolicyBrowserPort
import com.signkorean.domain.port.output.ReminderSchedulerPort
import com.signkorean.app.legal.AndroidCustomTabsPolicyBrowser
import com.signkorean.app.reminder.WorkManagerReminderScheduler
import com.signkorean.platform.audio.ExoPlayerAudioPlayerFactory
import com.signkorean.platform.audio.Media3AudioPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlatformModule {
    @Provides
    @Singleton
    @AppVersionName
    fun provideAppVersionName(@ApplicationContext context: Context): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"

    @Provides
    @Singleton
    fun provideAudioPlayer(
        @ApplicationContext context: Context,
    ): AudioPlayerPort = Media3AudioPlayer(ExoPlayerAudioPlayerFactory(context))

    @Provides
    @Singleton
    fun provideReminderScheduler(
        @ApplicationContext context: Context,
    ): ReminderSchedulerPort = WorkManagerReminderScheduler(context)

    @Provides
    @Singleton
    fun providePolicyBrowser(
        @ApplicationContext context: Context,
    ): PolicyBrowserPort = AndroidCustomTabsPolicyBrowser(context)
}
