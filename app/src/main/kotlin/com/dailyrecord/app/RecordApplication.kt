package com.dailyrecord.app

import android.app.Application
import androidx.room.Room
import com.dailyrecord.app.data.AppDatabase
import com.dailyrecord.app.data.BackupService
import com.dailyrecord.app.data.RecordRepository
import com.dailyrecord.app.data.WallpaperStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.time.ZoneId

data class WallpaperState(val file: File?, val generation: Long)

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

    val wallpaperStore by lazy { WallpaperStore(filesDir) }

    private val _wallpaper by lazy { MutableStateFlow(WallpaperState(wallpaperStore.wallpaperFile(), 0L)) }
    val wallpaper: StateFlow<WallpaperState> get() = _wallpaper

    fun refreshWallpaper() {
        val current = _wallpaper.value
        _wallpaper.value = WallpaperState(wallpaperStore.wallpaperFile(), current.generation + 1)
    }
}
