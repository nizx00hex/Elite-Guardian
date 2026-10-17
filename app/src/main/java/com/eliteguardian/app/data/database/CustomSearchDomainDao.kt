package com.eliteguardian.app.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomSearchDomainDao {

    @Query("SELECT * FROM custom_search_domains ORDER BY domain ASC")
    fun getAllFlow(): Flow<List<CustomSearchDomainEntity>>

    @Query("SELECT * FROM custom_search_domains ORDER BY domain ASC")
    suspend fun getAllSnapshot(): List<CustomSearchDomainEntity>

    @Query("SELECT * FROM custom_search_domains WHERE is_enabled = 1")
    suspend fun getEnabledDomains(): List<CustomSearchDomainEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CustomSearchDomainEntity): Long

    @Update
    suspend fun update(entity: CustomSearchDomainEntity)

    @Delete
    suspend fun delete(entity: CustomSearchDomainEntity)

    @Query("DELETE FROM custom_search_domains WHERE domain = :domain")
    suspend fun deleteByDomain(domain: String)

    @Query("DELETE FROM custom_search_domains")
    suspend fun clearAll()
}

