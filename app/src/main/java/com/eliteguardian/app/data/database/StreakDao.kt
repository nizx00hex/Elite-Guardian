package com.eliteguardian.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakDao {

    @Query("SELECT * FROM streak_records ORDER BY local_date ASC")
    fun getAllRecordsFlow(): Flow<List<StreakRecordEntity>>

    @Query("SELECT * FROM streak_records ORDER BY local_date ASC")
    suspend fun getAllRecords(): List<StreakRecordEntity>

    @Query("SELECT * FROM streak_records WHERE local_date = :date LIMIT 1")
    suspend fun getRecordByDate(date: String): StreakRecordEntity?

    @Query("SELECT * FROM streak_records WHERE local_date = :date LIMIT 1")
    fun getRecordByDateFlow(date: String): Flow<StreakRecordEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: StreakRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<StreakRecordEntity>)

    @Query("DELETE FROM streak_records WHERE local_date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM streak_records")
    suspend fun deleteAllStreakData()

    @Query("SELECT COUNT(*) FROM streak_records WHERE status = 'COMPLETED'")
    suspend fun getCompletedCount(): Int

    @Query("SELECT COUNT(*) FROM streak_records WHERE status = 'LAPSE'")
    suspend fun getLapseCount(): Int
}

