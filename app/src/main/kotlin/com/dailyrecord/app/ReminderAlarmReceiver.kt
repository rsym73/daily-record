package com.dailyrecord.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as RecordApplication
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // 到点时，当前「记录页」未「完成」才提醒（凌晨 1 点边界那天）
                val completed = app.repository.isTodayCompleted(Instant.now())
                if (!completed) {
                    postNotification(context)
                }
                val hour = app.repository.getReminderHour()
                val minute = app.repository.getReminderMinute()
                ReminderAlarmScheduler.schedule(context.applicationContext, hour, minute)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun postNotification(context: Context) {
        val channelId = "daily-reminder"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(channelId, "每日提醒", NotificationManager.IMPORTANCE_HIGH).apply {
                // 静音渠道 + 横幅：不响铃不震动，避免凌晨打扰
                setSound(null, null)
                enableVibration(false)
            }
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("今天还没完成哦～")
            .setContentText("去完成今天吧，不然会断链啦 🥺")
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(1, notification)
        } catch (_: SecurityException) {
            // 通知权限未授予，忽略
        }
    }
}
