package com.signkorean.app.di

import android.content.Context
import com.signkorean.app.BuildConfig
import com.signkorean.app.legal.AndroidCustomTabsPolicyBrowser
import com.signkorean.app.observability.AndroidDiagnosticLogger
import com.signkorean.app.reminder.WorkManagerReminderScheduler
import com.signkorean.data.observability.RedactingDiagnosticLogger
import com.signkorean.domain.port.output.AudioPlayerPort
import com.signkorean.domain.port.output.AppLocalePort
import com.signkorean.domain.port.output.DiagnosticLoggerPort
import com.signkorean.domain.port.output.PolicyBrowserPort
import com.signkorean.domain.port.output.ReminderSchedulerPort
import com.signkorean.platform.audio.ExoPlayerAudioPlayerFactory
import com.signkorean.platform.audio.Media3AudioPlayer
import com.signkorean.platform.locale.AndroidAppLocaleAdapter
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
    @SupportedLanguageTags
    fun provideSupportedLanguageTags(@ApplicationContext context: Context): Set<String> =
        context.resources.getStringArray(com.signkorean.app.R.array.supported_language_tags).toSet()

    @Provides
    @Singleton
    fun provideAppLocalePort(
        @SupportedLanguageTags supportedLanguageTags: Set<String>,
    ): AppLocalePort = AndroidAppLocaleAdapter(supportedLanguageTags)

    @Provides
    @Singleton
    fun provideDiagnosticLogger(): DiagnosticLoggerPort =
        RedactingDiagnosticLogger(AndroidDiagnosticLogger(enabled = BuildConfig.DEBUG))

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
