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
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
class RecordRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: EntryDao
    private lateinit var repo: RecordRepository
    private val zone = ZoneId.of("Asia/Shanghai")
    private val jan1 = LocalDate.of(2024, 1, 1)

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int) =
        ZonedDateTime.of(y, m, d, h, min, 0, 0, zone).toInstant()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.entryDao()
        repo = RecordRepository(db.dayDao(), dao, db.keyValueDao(), zone)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `空文本被拒绝`() = runBlocking {
        val result = repo.addEntry(at(2024, 1, 1, 10, 0), "   ")
        assertEquals(AddEntryResult.Blank, result)
        assertEquals(0, dao.getEntriesForDay(jan1).size)
    }

    @Test
    fun `非空文本按今天持久化`() = runBlocking {
        val result = repo.addEntry(at(2024, 1, 1, 10, 0), "写完了登录模块")
        assertEquals(AddEntryResult.Ok, result)
        val entries = dao.getEntriesForDay(jan1)
        assertEquals(1, entries.size)
        assertEquals("写完了登录模块", entries[0].text)
    }

    @Test
    fun `凌晨 1 点前属于前一天`() = runBlocking {
        repo.addEntry(at(2024, 1, 2, 0, 30), "深夜写的")
        val entries = dao.getEntriesForDay(jan1)
        assertEquals(1, entries.size)
        assertEquals("深夜写的", entries[0].text)
    }

    @Test
    fun `编辑今天的条目成功`() = runBlocking {
        repo.addEntry(at(2024, 1, 1, 10, 0), "旧文本")
        val id = dao.getEntriesForDay(jan1)[0].id

        val result = repo.editEntry(at(2024, 1, 1, 11, 0), id, "新文本")

        assertEquals(EditResult.Ok, result)
        assertEquals("新文本", dao.getEntriesForDay(jan1)[0].text)
    }

    @Test
    fun `编辑空白文本被拒绝`() = runBlocking {
        repo.addEntry(at(2024, 1, 1, 10, 0), "旧文本")
        val id = dao.getEntriesForDay(jan1)[0].id

        val result = repo.editEntry(at(2024, 1, 1, 11, 0), id, "   ")

        assertEquals(EditResult.Blank, result)
        assertEquals("旧文本", dao.getEntriesForDay(jan1)[0].text)
    }

    @Test
    fun `编辑过去的条目被拒绝`() = runBlocking {
        repo.addEntry(at(2024, 1, 1, 10, 0), "旧文本")
        val id = dao.getEntriesForDay(jan1)[0].id

        // 1月2日：1月1日的条目已冻结
        val result = repo.editEntry(at(2024, 1, 2, 10, 0), id, "新文本")

        assertEquals(EditResult.Frozen, result)
        assertEquals("旧文本", dao.getEntriesForDay(jan1)[0].text)
    }

    @Test
    fun `删除今天的条目成功`() = runBlocking {
        repo.addEntry(at(2024, 1, 1, 10, 0), "要删的")
        val id = dao.getEntriesForDay(jan1)[0].id

        val result = repo.deleteEntry(at(2024, 1, 1, 11, 0), id)

        assertEquals(DeleteResult.Ok, result)
        assertEquals(0, dao.getEntriesForDay(jan1).size)
    }

    @Test
    fun `删除过去的条目被拒绝`() = runBlocking {
        repo.addEntry(at(2024, 1, 1, 10, 0), "要删的")
        val id = dao.getEntriesForDay(jan1)[0].id

        val result = repo.deleteEntry(at(2024, 1, 2, 10, 0), id)

        assertEquals(DeleteResult.Frozen, result)
        assertEquals(1, dao.getEntriesForDay(jan1).size)
    }

    @Test
    fun `完成后添加条目被拒绝`() = runBlocking {
        repo.completeToday(at(2024, 1, 1, 10, 0))
        val result = repo.addEntry(at(2024, 1, 1, 11, 0), "新条目")
        assertEquals(AddEntryResult.Completed, result)
        assertEquals(0, dao.getEntriesForDay(jan1).size)
    }

    @Test
    fun `完成后删除条目被拒绝`() = runBlocking {
        repo.addEntry(at(2024, 1, 1, 9, 0), "条目")
        repo.completeToday(at(2024, 1, 1, 10, 0))
        val id = dao.getEntriesForDay(jan1)[0].id
        val result = repo.deleteEntry(at(2024, 1, 1, 11, 0), id)
        assertEquals(DeleteResult.Completed, result)
        assertEquals(1, dao.getEntriesForDay(jan1).size)
    }

    @Test
    fun `撤销完成后可继续添加`() = runBlocking {
        repo.completeToday(at(2024, 1, 1, 10, 0))
        repo.undoToday(at(2024, 1, 1, 11, 0))
        val result = repo.addEntry(at(2024, 1, 1, 12, 0), "新条目")
        assertEquals(AddEntryResult.Ok, result)
    }

    @Test
    fun `编辑未来的条目被拒绝为 Future`() = runBlocking {
        val future = jan1.plusDays(1)
        val id = dao.insert(EntryEntity(dayDate = future, text = "未来的"))

        val result = repo.editEntry(at(2024, 1, 1, 10, 0), id, "改")

        assertEquals(EditResult.Future, result)
        assertEquals("未来的", dao.getById(id)!!.text)
    }

    @Test
    fun `删除未来的条目被拒绝为 Future`() = runBlocking {
        val future = jan1.plusDays(1)
        val id = dao.insert(EntryEntity(dayDate = future, text = "未来的"))

        val result = repo.deleteEntry(at(2024, 1, 1, 10, 0), id)

        assertEquals(DeleteResult.Future, result)
        assertEquals(1, dao.getEntriesForDay(future).size)
    }
}
