package com.dailyrecord.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
class HistoryTest {

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
    fun `历史视图标记完成和漏掉的天`() = runBlocking {
        repo.completeToday(at(2024, 1, 1, 10, 0))
        repo.completeToday(at(2024, 1, 2, 10, 0))
        // 1 月 3 日漏掉，现在 1 月 4 日
        val days = repo.getHistory(at(2024, 1, 4, 10, 0), LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 4))

        val jan1 = days.first { it.date == LocalDate.of(2024, 1, 1) }
        val jan2 = days.first { it.date == LocalDate.of(2024, 1, 2) }
        val jan3 = days.first { it.date == LocalDate.of(2024, 1, 3) }
        val jan4 = days.first { it.date == LocalDate.of(2024, 1, 4) }

        assertTrue(jan1.completed); assertFalse(jan1.missed)
        assertTrue(jan2.completed); assertFalse(jan2.missed)
        assertFalse(jan3.completed); assertTrue(jan3.missed)
        assertFalse(jan4.completed); assertFalse(jan4.missed)
    }
}
