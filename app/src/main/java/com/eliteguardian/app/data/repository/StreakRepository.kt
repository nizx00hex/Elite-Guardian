package com.eliteguardian.app.data.repository

import android.content.Context
import com.eliteguardian.app.data.database.AppDatabase
import com.eliteguardian.app.data.database.StreakDao
import com.eliteguardian.app.data.database.StreakRecordEntity
import com.eliteguardian.app.domain.streak.StreakCalculator
import com.eliteguardian.app.domain.streak.StreakStatus
import com.eliteguardian.app.domain.streak.StreakSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class StreakRepository(
    private val streakDao: StreakDao
) {

    fun getAllRecordsFlow(): Flow<List<StreakRecordEntity>> = streakDao.getAllRecordsFlow()

    fun getStreakSummaryFlow(): Flow<StreakSummary> {
        return streakDao.getAllRecordsFlow().map { records ->
            StreakCalculator.generateSummary(records, LocalDate.now())
        }
    }

    suspend fun getAllRecords(): List<StreakRecordEntity> = withContext(Dispatchers.IO) {
        streakDao.getAllRecords()
    }

    suspend fun getRecordForDate(date: String): StreakRecordEntity? = withContext(Dispatchers.IO) {
        streakDao.getRecordByDate(date)
    }

    suspend fun recordCheckIn(date: String, note: String? = null) = withContext(Dispatchers.IO) {
        val existing = streakDao.getRecordByDate(date)
        streakDao.deleteByDate(date)
        val record = StreakRecordEntity(
            id = 0,
            localDate = date,
            status = StreakStatus.COMPLETED.name,
            note = note,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        streakDao.insertOrUpdate(record)
    }

    suspend fun recordLapse(date: String, note: String? = null) = withContext(Dispatchers.IO) {
        val existing = streakDao.getRecordByDate(date)
        streakDao.deleteByDate(date)
        val record = StreakRecordEntity(
            id = 0,
            localDate = date,
            status = StreakStatus.LAPSE.name,
            note = note,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        streakDao.insertOrUpdate(record)
    }

    suspend fun updateNote(date: String, note: String?) = withContext(Dispatchers.IO) {
        val existing = streakDao.getRecordByDate(date)
        if (existing != null) {
            val updated = existing.copy(
                note = note,
                updatedAt = System.currentTimeMillis()
            )
            streakDao.insertOrUpdate(updated)
        }
    }

    suspend fun deleteRecord(date: String) = withContext(Dispatchers.IO) {
        streakDao.deleteByDate(date)
    }

    suspend fun resetAllStreakData() = withContext(Dispatchers.IO) {
        streakDao.deleteAllStreakData()
    }

    /**
     * Exports all streak records to a clean JSON string for local backup.
     */
    suspend fun exportDataAsJson(): String = withContext(Dispatchers.IO) {
        val records = streakDao.getAllRecords()
        val jsonArray = JSONArray()
        records.forEach { record ->
            val obj = JSONObject()
            obj.put("localDate", record.localDate)
            obj.put("status", record.status)
            obj.put("note", record.note ?: JSONObject.NULL)
            obj.put("createdAt", record.createdAt)
            obj.put("updatedAt", record.updatedAt)
            jsonArray.put(obj)
        }
        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "EliteGuardian Streak Tracker")
        root.put("exportedAt", System.currentTimeMillis())
        root.put("records", jsonArray)
        root.toString(2)
    }

    /**
     * Imports streak records from JSON string.
     */
    suspend fun importDataFromJson(jsonString: String): Int = withContext(Dispatchers.IO) {
        val root = JSONObject(jsonString)
        val array = root.getJSONArray("records")
        val entities = mutableListOf<StreakRecordEntity>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val date = obj.getString("localDate")
            val status = obj.getString("status")
            val note = if (obj.isNull("note")) null else obj.getString("note")
            val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            val updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
            entities.add(
                StreakRecordEntity(
                    localDate = date,
                    status = status,
                    note = note,
                    createdAt = createdAt,
                    updatedAt = updatedAt
                )
            )
        }
        streakDao.insertAll(entities)
        entities.size
    }

    companion object {
        @Volatile
        private var INSTANCE: StreakRepository? = null

        fun getInstance(context: Context): StreakRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getInstance(context)
                val instance = StreakRepository(db.streakDao())
                INSTANCE = instance
                instance
            }
        }
    }
}

