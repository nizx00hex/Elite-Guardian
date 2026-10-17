package com.eliteguardian.app.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "streak_records",
    indices = [Index(value = ["local_date"], unique = true)]
)
data class StreakRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "local_date")
    val localDate: String, // ISO-8601 YYYY-MM-DD
    @ColumnInfo(name = "status")
    val status: String, // "COMPLETED" or "LAPSE"
    @ColumnInfo(name = "note")
    val note: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

