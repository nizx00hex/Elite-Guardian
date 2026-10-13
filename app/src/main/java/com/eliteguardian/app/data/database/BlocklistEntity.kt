package com.eliteguardian.app.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blocklist_entries",
    indices = [
        Index(value = ["domain", "isAllowlist"], unique = true)
    ]
)
data class BlocklistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domain: String,
    val isAllowlist: Boolean,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

