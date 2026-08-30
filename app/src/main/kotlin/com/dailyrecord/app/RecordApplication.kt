package com.dailyrecord.app

import android.app.Application
import androidx.room.Room
import com.dailyrecord.app.data.AppDatabase
import com.dailyrecord.app.data.BackupService
import com.dailyrecord.app.data.RecordRepository
import java.time.ZoneId

class RecordApplication : Application() {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "daily-record.db").build()
    }

    val repository: RecordRepository by lazy {
        RecordRepository(
            database.dayDao(),
            database.entryDao(),
            database.keyValueDao(),
            ZoneId.systemDefault(),
        )
    }

    val backupService: BackupService by lazy {
        BackupService(database.dayDao(), database.entryDao(), database.keyValueDao())
    }
}
