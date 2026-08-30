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
class BackupServiceTest {

    private val zone = ZoneId.of("Asia/Shanghai")
    private lateinit var sourceDb: AppDatabase
    private lateinit var targetDb: AppDatabase

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int) =
        ZonedDateTime.of(y, m, d, h, min, 0, 0, zone).toInstant()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        sourceDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        targetDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        sourceDb.close()
        targetDb.close()
    }

    @Test
    fun `导出再导入数据一致`() = runBlocking {
        // 源库：完成 1 月 1 日，加两条条目
        val sourceRepo = RecordRepository(sourceDb.dayDao(), sourceDb.entryDao(), sourceDb.keyValueDao(), zone)
        sourceRepo.addEntry(at(2024, 1, 1, 9, 0), "条目一")
        sourceRepo.addEntry(at(2024, 1, 1, 9, 30), "条目二")
        sourceRepo.completeToday(at(2024, 1, 1, 10, 0))

        val json = BackupService(sourceDb.dayDao(), sourceDb.entryDao(), sourceDb.keyValueDao()).exportData()

        BackupService(targetDb.dayDao(), targetDb.entryDao(), targetDb.keyValueDao()).importData(json)

        // 目标库状态一致
        val targetRepo = RecordRepository(targetDb.dayDao(), targetDb.entryDao(), targetDb.keyValueDao(), zone)
        assertEquals(1, targetRepo.streak(at(2024, 1, 1, 13, 0)))
        val entries = targetDb.entryDao().getEntriesForDay(LocalDate.of(2024, 1, 1))
        assertEquals(listOf("条目一", "条目二"), entries.map { it.text })
    }

    @Test
    fun `未完成天的条目也能导出导入`() = runBlocking {
        val sourceRepo = RecordRepository(sourceDb.dayDao(), sourceDb.entryDao(), sourceDb.keyValueDao(), zone)
        sourceRepo.addEntry(at(2024, 1, 1, 10, 0), "未完成的条目")
        // 注意：不 completeToday，让 1 月 1 日保持「未完成但有条目」

        val json = BackupService(sourceDb.dayDao(), sourceDb.entryDao(), sourceDb.keyValueDao()).exportData()
        BackupService(targetDb.dayDao(), targetDb.entryDao(), targetDb.keyValueDao()).importData(json)

        val entries = targetDb.entryDao().getEntriesForDay(LocalDate.of(2024, 1, 1))
        assertEquals(listOf("未完成的条目"), entries.map { it.text })
    }
}
