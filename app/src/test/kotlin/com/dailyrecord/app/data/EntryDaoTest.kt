package com.dailyrecord.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class EntryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: EntryDao
    private val day = LocalDate.of(2024, 1, 1)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.entryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `插入条目后能按天查回`() = runBlocking {
        dao.insert(EntryEntity(dayDate = day, text = "写完了登录模块"))

        val entries = dao.getEntriesForDay(day)

        assertEquals(1, entries.size)
        assertEquals("写完了登录模块", entries[0].text)
    }

    @Test
    fun `更新条目后持久化`() = runBlocking {
        val id = dao.insert(EntryEntity(dayDate = day, text = "旧文本"))
        val original = dao.getById(id)!!

        dao.update(original.copy(text = "新文本"))

        val entries = dao.getEntriesForDay(day)
        assertEquals(1, entries.size)
        assertEquals("新文本", entries[0].text)
    }

    @Test
    fun `删除条目后消失`() = runBlocking {
        val id = dao.insert(EntryEntity(dayDate = day, text = "要删的"))

        dao.delete(dao.getById(id)!!)

        assertEquals(0, dao.getEntriesForDay(day).size)
    }
}
