package com.dailyrecord.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface KeyValueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: KeyValueEntity)

    @Query("SELECT value FROM key_value WHERE key = :key")
    suspend fun get(key: String): String?
}
