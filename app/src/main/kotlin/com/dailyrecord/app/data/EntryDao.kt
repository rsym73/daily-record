package com.dailyrecord.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import java.time.LocalDate

@Dao
interface EntryDao {
    @Insert
    suspend fun insert(entry: EntryEntity): Long

    @Update
    suspend fun update(entry: EntryEntity)

    @Delete
    suspend fun delete(entry: EntryEntity)

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getById(id: Long): EntryEntity?

    @Query("SELECT * FROM entries WHERE dayDate = :day ORDER BY createdAt ASC, id ASC")
    suspend fun getEntriesForDay(day: LocalDate): List<EntryEntity>

    @Query("SELECT * FROM entries ORDER BY dayDate ASC, createdAt ASC, id ASC")
    suspend fun getAllEntries(): List<EntryEntity>
}
