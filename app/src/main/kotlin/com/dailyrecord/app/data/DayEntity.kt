package com.dailyrecord.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "days")
data class DayEntity(
    @PrimaryKey val date: LocalDate,
    val completedAt: Instant,
)
