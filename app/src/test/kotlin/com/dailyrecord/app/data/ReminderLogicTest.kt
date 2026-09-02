package com.dailyrecord.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun `Android 11 及以下始终用精确闹钟`() {
        assertTrue(ReminderLogic.shouldUseExactAlarm(24, false))
        assertTrue(ReminderLogic.shouldUseExactAlarm(30, false))
    }

    @Test
    fun `Android 12 及以上按权限决定精确或回退`() {
        assertTrue(ReminderLogic.shouldUseExactAlarm(31, true))
        assertFalse(ReminderLogic.shouldUseExactAlarm(31, false))
        assertTrue(ReminderLogic.shouldUseExactAlarm(34, true))
        assertFalse(ReminderLogic.shouldUseExactAlarm(34, false))
    }
}
