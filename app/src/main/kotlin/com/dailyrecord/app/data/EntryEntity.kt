package com.dailyrecord.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "entries", indices = [Index("dayDate")])
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayDate: LocalDate,
    val text: String,
    val createdAt: Instant = Instant.now(),
)
