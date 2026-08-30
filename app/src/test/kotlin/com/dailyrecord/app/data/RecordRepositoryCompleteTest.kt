package com.dailyrecord.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
class RecordRepositoryCompleteTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: RecordRepository
    private val zone = ZoneId.of("Asia/Shanghai")

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int) =
        ZonedDateTime.of(y, m, d, h, min, 0, 0, zone).toInstant()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repo = RecordRepository(db.dayDao(), db.entryDao(), db.keyValueDao(), zone)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `新链 streak 为 0`() = runBlocking {
        assertEquals(0, repo.streak(at(2024, 1, 1, 10, 0)))
    }

    @Test
    fun `完成今天后 streak 为 1 且持久化`() = runBlocking {
        val result = repo.completeToday(at(2024, 1, 1, 10, 0))
        assertTrue(result is CompleteTodayResult.Ok)
        assertEquals(1, (result as CompleteTodayResult.Ok).streak)
        // 持久化：同一 DB 再次读取仍是 1
        assertEquals(1, repo.streak(at(2024, 1, 1, 11, 0)))
    }

    @Test
    fun `连续完成两天 streak 为 2`() = runBlocking {
        repo.completeToday(at(2024, 1, 1, 10, 0))
        val result = repo.completeToday(at(2024, 1, 2, 10, 0))
        assertTrue(result is CompleteTodayResult.Ok)
        assertEquals(2, (result as CompleteTodayResult.Ok).streak)
    }

    @Test
    fun `空页也能完成`() = runBlocking {
        val result = repo.completeToday(at(2024, 1, 1, 10, 0))
        assertTrue(result is CompleteTodayResult.Ok)
    }

    @Test
    fun `漏一天后完成被拒绝`() = runBlocking {
        repo.completeToday(at(2024, 1, 1, 10, 0))
        // 跳过 1 月 2 日，1 月 3 日断链锁死
        val result = repo.completeToday(at(2024, 1, 3, 10, 0))
        assertEquals(CompleteTodayResult.Locked, result)
    }

    @Test
    fun `撤销完成后 streak 回退`() = runBlocking {
        repo.completeToday(at(2024, 1, 1, 10, 0))
        val result = repo.undoToday(at(2024, 1, 1, 11, 0))
        assertTrue(result is UndoTodayResult.Ok)
        assertEquals(0, (result as UndoTodayResult.Ok).streak)
        assertEquals(0, repo.streak(at(2024, 1, 1, 12, 0)))
    }

    @Test
    fun `撤销未完成的天被拒绝`() = runBlocking {
        val result = repo.undoToday(at(2024, 1, 1, 10, 0))
        assertEquals(UndoTodayResult.NotCompleted, result)
    }

    @Test
    fun `完成状态可查询`() = runBlocking {
        assertEquals(false, repo.isTodayCompleted(at(2024, 1, 1, 10, 0)))
        repo.completeToday(at(2024, 1, 1, 10, 0))
        assertEquals(true, repo.isTodayCompleted(at(2024, 1, 1, 11, 0)))
        repo.undoToday(at(2024, 1, 1, 12, 0))
        assertEquals(false, repo.isTodayCompleted(at(2024, 1, 1, 13, 0)))
    }

    @Test
    fun `漏一天后 isBroken 为 true`() = runBlocking {
        repo.completeToday(at(2024, 1, 1, 10, 0))
        assertEquals(false, repo.isBroken(at(2024, 1, 2, 10, 0)))
        assertEquals(true, repo.isBroken(at(2024, 1, 3, 10, 0)))
    }

    @Test
    fun `重置后从今天重新起链`() = runBlocking {
        repo.completeToday(at(2024, 1, 1, 10, 0))
        // 漏掉 1 月 2 日，1 月 3 日断链后重置
        assertEquals(0, repo.reset(at(2024, 1, 3, 10, 0)))
        assertEquals(false, repo.isBroken(at(2024, 1, 3, 10, 0)))
        // 今天可以重新完成，且只计今天（旧历史不再累加）
        val result = repo.completeToday(at(2024, 1, 3, 10, 0))
        assertTrue(result is CompleteTodayResult.Ok)
        assertEquals(1, (result as CompleteTodayResult.Ok).streak)
    }

    @Test
    fun `提醒时间默认 0 点 30 分且可设置`() = runBlocking {
        assertEquals(0, repo.getReminderHour())
        assertEquals(30, repo.getReminderMinute())
        repo.setReminderTime(23, 45)
        assertEquals(23, repo.getReminderHour())
        assertEquals(45, repo.getReminderMinute())
    }
}
