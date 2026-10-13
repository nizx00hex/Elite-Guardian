package com.eliteguardian.app.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlocklistDao {

    @Query("SELECT * FROM blocklist_entries ORDER BY createdAt DESC")
    fun getAllEntries(): Flow<List<BlocklistEntity>>

    @Query("SELECT * FROM blocklist_entries WHERE isAllowlist = 0 ORDER BY domain ASC")
    fun getBlocklistEntries(): Flow<List<BlocklistEntity>>

    @Query("SELECT * FROM blocklist_entries WHERE isAllowlist = 1 ORDER BY domain ASC")
    fun getAllowlistEntries(): Flow<List<BlocklistEntity>>

    @Query("SELECT * FROM blocklist_entries WHERE domain LIKE '%' || :query || '%' ORDER BY domain ASC")
    fun search(query: String): Flow<List<BlocklistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: BlocklistEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<BlocklistEntity>)

    @Delete
    suspend fun delete(entry: BlocklistEntity)

    @Query("DELETE FROM blocklist_entries WHERE domain = :domain")
    suspend fun deleteByDomain(domain: String)

    @Query("DELETE FROM blocklist_entries WHERE isAllowlist = 0")
    suspend fun clearBlocklist()

    @Query("DELETE FROM blocklist_entries WHERE isAllowlist = 1")
    suspend fun clearAllowlist()

    @Query("SELECT COUNT(*) FROM blocklist_entries WHERE isAllowlist = 0")
    fun getBlocklistCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM blocklist_entries WHERE isAllowlist = 1")
    fun getAllowlistCount(): Flow<Int>

    @Query("SELECT * FROM blocklist_entries")
    suspend fun getAllEntriesSnapshot(): List<BlocklistEntity>
}

