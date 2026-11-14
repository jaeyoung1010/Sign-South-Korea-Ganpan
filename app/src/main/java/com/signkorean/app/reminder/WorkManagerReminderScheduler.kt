package com.signkorean.app.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.signkorean.domain.port.output.ReminderPermission
import com.signkorean.domain.port.output.ReminderSchedule
import com.signkorean.domain.port.output.ReminderSchedulerPort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Duration
import java.time.Instant

class WorkManagerReminderScheduler(
    private val context: Context,
) : ReminderSchedulerPort {
    override fun observePermission(): Flow<ReminderPermission> = flowOf(permission())

    override suspend fun schedule(schedule: ReminderSchedule) {
        if (permission() == ReminderPermission.DENIED) return
        NotificationChannels.ensure(context)
        val request =
            OneTimeWorkRequestBuilder<ReviewReminderWorker>()
                .setInitialDelay(Duration.between(Instant.now(), schedule.triggerAt).coerceAtLeast(Duration.ZERO))
                .setInputData(
                    Data
                        .Builder()
                        .putString(ReviewReminderWorker.KEY_LANGUAGE_TAG, schedule.appLanguageTag)
                        .putString(ReviewReminderWorker.KEY_LOCAL_TIME, schedule.localTime.toString())
                        .putString(ReviewReminderWorker.KEY_ZONE_ID, schedule.zoneId.id)
                        .build(),
                ).build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    override suspend fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private fun permission(): ReminderPermission =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            ReminderPermission.NOT_REQUIRED
        } else if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            ReminderPermission.GRANTED
        } else {
            ReminderPermission.DENIED
        }

    companion object {
        const val WORK_NAME = "sign-korean-review-reminder"
    }
}

object NotificationChannels {
    const val REVIEW_CHANNEL_ID = "review-reminders"

    fun ensure(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                REVIEW_CHANNEL_ID,
                context.getString(com.signkorean.app.R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(com.signkorean.app.R.string.reminder_channel_description)
            },
        )
    }
}
