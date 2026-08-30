package com.dailyrecord.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderLogicTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int) =
        ZonedDateTime.of(y, m, d, h, min, 0, 0, zone).toInstant()

    @Test
    fun `距下次提醒的时间（默认 0 点 30 分）`() {
        assertEquals(Duration.ofMinutes(30), ReminderLogic.nextReminderDelay(at(2024, 1, 1, 0, 0), zone, 0, 30))
        assertEquals(Duration.ofMinutes(1), ReminderLogic.nextReminderDelay(at(2024, 1, 1, 0, 29), zone, 0, 30))
        assertEquals(Duration.ofHours(23).plusMinutes(59), ReminderLogic.nextReminderDelay(at(2024, 1, 1, 0, 31), zone, 0, 30))
        assertEquals(Duration.ofMinutes(90), ReminderLogic.nextReminderDelay(at(2024, 1, 1, 23, 0), zone, 0, 30))
    }

    @Test
    fun `自定义时和分`() {
        // 23:45 → 23:00 时下一次是 45 分钟后
        assertEquals(Duration.ofMinutes(45), ReminderLogic.nextReminderDelay(at(2024, 1, 1, 23, 0), zone, 23, 45))
        // 23:45 → 23:46 时下一次是明天的 23:45（23h59m）
        assertEquals(Duration.ofHours(23).plusMinutes(59), ReminderLogic.nextReminderDelay(at(2024, 1, 1, 23, 46), zone, 23, 45))
    }
}
