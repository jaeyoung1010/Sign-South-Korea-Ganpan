package com.signkorean.app.reminder

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.signkorean.app.MainActivity
import com.signkorean.app.navigation.AppNavigationIntent
import com.signkorean.domain.port.output.ClockPort
import com.signkorean.domain.port.output.LearningRepository
import com.signkorean.domain.reminder.ReminderPolicy
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalTime
import kotlinx.coroutines.flow.first

@HiltWorker
class ReviewReminderWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val learningRepository: LearningRepository,
        private val clock: ClockPort,
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result {
            val progress = learningRepository.observeProgress(clock.now()).first()
            val dueCount =
                progress.count { card ->
                    val nextReviewAt = card.nextReviewAt
                    nextReviewAt != null && !nextReviewAt.isAfter(clock.now())
                }
            if (dueCount == 0) return Result.success()

            NotificationChannels.ensure(applicationContext)
            val languageTag = inputData.getString(KEY_LANGUAGE_TAG) ?: "en"
            val localizedContext = applicationContext.localized(languageTag)
            val notification =
                Notification.Builder(localizedContext, NotificationChannels.REVIEW_CHANNEL_ID)
                    .setSmallIcon(com.signkorean.app.R.drawable.ic_app_icon)
                    .setContentTitle(localizedContext.getString(com.signkorean.app.R.string.reminder_notification_title))
                    .setContentText(
                        localizedContext.resources.getQuantityString(
                            com.signkorean.app.R.plurals.reminder_notification_message,
                            dueCount,
                            dueCount,
                        ),
                    ).setContentIntent(contentIntent())
                    .setAutoCancel(true)
                    .build()
            val manager = applicationContext.getSystemService(android.app.NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, notification)
            enqueueNext(languageTag)
            return Result.success()
        }

        private fun contentIntent(): PendingIntent {
            val intent =
                Intent(applicationContext, MainActivity::class.java).apply {
                    action = AppNavigationIntent.ACTION_OPEN_REVIEWS
                    putExtra(AppNavigationIntent.EXTRA_OPEN_REVIEWS, true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            return PendingIntent.getActivity(
                applicationContext,
                NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun enqueueNext(languageTag: String) {
            val localTime = inputData.getString(KEY_LOCAL_TIME)?.let(LocalTime::parse) ?: LocalTime.of(19, 0)
            val now = clock.now()
            val nextTrigger = ReminderPolicy.nextTrigger(true, 1, localTime, now, clock.zoneId()) ?: return
            val request =
                androidx.work.OneTimeWorkRequestBuilder<ReviewReminderWorker>()
                    .setInitialDelay(java.time.Duration.between(now, nextTrigger))
                    .setInputData(
                        Data.Builder()
                            .putString(KEY_LANGUAGE_TAG, languageTag)
                            .putString(KEY_LOCAL_TIME, localTime.toString())
                            .putString(KEY_ZONE_ID, clock.zoneId().id)
                            .build(),
                    ).build()
            androidx.work.WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                WorkManagerReminderScheduler.WORK_NAME,
                androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE,
                request,
            )
        }

        @SuppressLint("AppBundleLocaleChanges")
        private fun Context.localized(languageTag: String): Context {
            val locale = java.util.Locale.forLanguageTag(languageTag)
            val configuration = resources.configuration
            configuration.setLocale(locale)
            return createConfigurationContext(configuration)
        }

        companion object {
            const val KEY_LANGUAGE_TAG = "language_tag"
            const val KEY_LOCAL_TIME = "local_time"
            const val KEY_ZONE_ID = "zone_id"
            const val NOTIFICATION_ID = 3301
        }
    }
