package com.dailyrecord.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class DayDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: DayDao
    private val jan1 = LocalDate.of(2024, 1, 1)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.dayDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `upsert 后能查回完成的天`() = runBlocking {
        dao.upsert(DayEntity(date = jan1, completedAt = Instant.parse("2024-01-01T02:00:00Z")))

        val day = dao.getDay(jan1)

        assertNotNull(day)
        assertEquals(jan1, day!!.date)
    }

    @Test
    fun `getCompletedDates 返回所有完成的天`() = runBlocking {
        dao.upsert(DayEntity(jan1, Instant.parse("2024-01-01T02:00:00Z")))
        dao.upsert(DayEntity(jan1.plusDays(1), Instant.parse("2024-01-02T02:00:00Z")))

        assertEquals(setOf(jan1, jan1.plusDays(1)), dao.getCompletedDates().toSet())
    }
}
