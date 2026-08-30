package com.dailyrecord.app

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.dailyrecord.app.data.ReminderLogic
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val WORK_NAME = "daily-reminder"

    fun schedule(context: Context, hour: Int = 0, minute: Int = 30) {
        val zone = ZoneId.systemDefault()
        val delay = ReminderLogic.nextReminderDelay(Instant.now(), zone, hour, minute)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }
}
