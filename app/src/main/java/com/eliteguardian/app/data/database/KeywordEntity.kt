package com.eliteguardian.app.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blocked_keywords",
    indices = [Index(value = ["keyword"], unique = true)]
)
data class KeywordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val keyword: String,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

