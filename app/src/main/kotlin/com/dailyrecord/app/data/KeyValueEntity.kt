package com.dailyrecord.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "key_value")
data class KeyValueEntity(
    @PrimaryKey val key: String,
    val value: String,
)
