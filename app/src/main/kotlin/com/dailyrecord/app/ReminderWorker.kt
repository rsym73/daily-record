package com.dailyrecord.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.Instant

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as RecordApplication
        // 0:30 时，「今天」= 即将在 1 点冻结的那一天；未完成才提醒
        val completed = app.repository.isTodayCompleted(Instant.now())
        if (!completed) {
            postNotification()
        }
        // 排定下一天的提醒（用配置的时间）
        val hour = app.repository.getReminderHour()
        val minute = app.repository.getReminderMinute()
        ReminderScheduler.schedule(applicationContext, hour, minute)
        return Result.success()
    }

    private fun postNotification() {
        val context = applicationContext
        val channelId = "daily-reminder"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(channelId, "每日提醒", NotificationManager.IMPORTANCE_HIGH)
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("今天还没记录哦～")
            .setContentText("去写写今天做了什么吧，不然连续天数会断掉啦 🥺")
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(1, notification)
        } catch (_: SecurityException) {
            // 通知权限未授予，忽略
        }
    }
}
