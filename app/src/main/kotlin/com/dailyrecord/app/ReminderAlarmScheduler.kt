package com.dailyrecord.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.dailyrecord.app.data.ReminderLogic
import java.time.Instant
import java.time.ZoneId

/**
 * 用系统 AlarmManager 直连调度提醒，替代 WorkManager：
 * 系统在「到点」时拉起 [ReminderAlarmReceiver]，比 WorkManager 更抗国产 ROM 的「划掉即杀」。
 */
object ReminderAlarmScheduler {
    private const val REQUEST_CODE = 1001

    /** 按下次提醒时间（默认 0:30）排一个一次性闹钟。 */
    fun schedule(context: Context, hour: Int = 0, minute: Int = 30) {
        val zone = ZoneId.systemDefault()
        val delay = ReminderLogic.nextReminderDelay(Instant.now(), zone, hour, minute)
        scheduleAt(context, System.currentTimeMillis() + delay.toMillis())
    }

    /** 在指定时刻排闹钟：能精确则精确，否则回退到允许延迟。 */
    fun scheduleAt(context: Context, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context)
        val useExact = ReminderLogic.shouldUseExactAlarm(
            sdkInt = Build.VERSION.SDK_INT,
            canScheduleExactAlarms = canScheduleExactAlarms(alarmManager),
        )
        if (useExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun canScheduleExactAlarms(alarmManager: AlarmManager): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
