package com.eliteguardian.app.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KeywordDao {

    @Query("SELECT * FROM blocked_keywords ORDER BY keyword ASC")
    fun getAllKeywords(): Flow<List<KeywordEntity>>

    @Query("SELECT keyword FROM blocked_keywords WHERE isEnabled = 1")
    fun getActiveKeywords(): Flow<List<String>>

    @Query("SELECT keyword FROM blocked_keywords WHERE isEnabled = 1")
    suspend fun getActiveKeywordsSnapshot(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(keyword: KeywordEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(keywords: List<KeywordEntity>)

    @Delete
    suspend fun delete(keyword: KeywordEntity)

    @Query("DELETE FROM blocked_keywords WHERE keyword = :keyword")
    suspend fun deleteByKeyword(keyword: String)

    @Query("UPDATE blocked_keywords SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateEnabledStatus(id: Long, isEnabled: Boolean)

    @Query("SELECT COUNT(*) FROM blocked_keywords")
    suspend fun getCount(): Int
}

