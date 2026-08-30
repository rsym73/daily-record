package com.dailyrecord.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.time.LocalDate

@Dao
interface DayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(day: DayEntity)

    @Delete
    suspend fun delete(day: DayEntity)

    @Query("SELECT * FROM days WHERE date = :date")
    suspend fun getDay(date: LocalDate): DayEntity?

    @Query("SELECT date FROM days")
    suspend fun getCompletedDates(): List<LocalDate>

    @Query("SELECT * FROM days")
    suspend fun getAllDays(): List<DayEntity>
}
