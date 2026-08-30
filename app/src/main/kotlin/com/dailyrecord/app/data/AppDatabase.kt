package com.dailyrecord.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [EntryEntity::class, DayEntity::class, KeyValueEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun dayDao(): DayDao
    abstract fun keyValueDao(): KeyValueDao
}
