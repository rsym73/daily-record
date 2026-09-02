package com.dailyrecord.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 开机 / 手动改时间 / 改时区后重排提醒（AlarmManager 闹钟在重启后会丢失）。
 */
class ReminderBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED
        ) {
            return
        }
        val app = context.applicationContext as RecordApplication
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val hour = app.repository.getReminderHour()
                val minute = app.repository.getReminderMinute()
                ReminderAlarmScheduler.schedule(context.applicationContext, hour, minute)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
