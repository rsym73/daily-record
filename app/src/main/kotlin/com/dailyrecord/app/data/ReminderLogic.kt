package com.dailyrecord.app.data

import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object ReminderLogic {
    /** 距下一次提醒还有多久。默认每天 0:30（凌晨 1 点边界前 30 分钟）。 */
    fun nextReminderDelay(now: Instant, zone: ZoneId, hour: Int = 0, minute: Int = 30): Duration {
        val zoned = now.atZone(zone)
        val today = zoned.toLocalDate()
        val candidate = ZonedDateTime.of(today, LocalTime.of(hour, minute), zone).toInstant()
        val next = if (candidate.isAfter(now)) {
            candidate
        } else {
            ZonedDateTime.of(today.plusDays(1), LocalTime.of(hour, minute), zone).toInstant()
        }
        return Duration.between(now, next)
    }
}
