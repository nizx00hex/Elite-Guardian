package com.eliteguardian.app.data.repository

import android.content.Context
import com.eliteguardian.app.data.database.AppDatabase
import com.eliteguardian.app.data.database.BlocklistEntity
import com.eliteguardian.app.domain.filtering.DomainMatcher
import com.eliteguardian.app.domain.filtering.DomainValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class FilterRepository(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val database = AppDatabase.getInstance(context)
    private val dao = database.blocklistDao()
    val matcher = DomainMatcher()

    private val seedBlocklist = mutableListOf<String>()

    init {
        loadSeedBlocklist()
        observeDatabaseEntries()
    }

    private fun loadSeedBlocklist() {
        try {
            context.assets.open("seed_blocklist.txt").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                    lines.forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                            DomainValidator.normalize(trimmed)?.let { domain ->
                                seedBlocklist.add(domain)
                            }
                        }
                    }
                }
            }
            matcher.setBlocklist(seedBlocklist)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun observeDatabaseEntries() {
        scope.launch {
            dao.getAllEntries().collect { entries ->
                val allows = entries.filter { it.isAllowlist }.map { it.domain }
                val customBlocks = entries.filter { !it.isAllowlist }.map { it.domain }

                matcher.setAllowlist(allows)
                matcher.setBlocklist(seedBlocklist + customBlocks)
            }
        }
    }

    suspend fun refreshRules() = withContext(Dispatchers.IO) {
        val entries = dao.getAllEntriesSnapshot()
        val allows = entries.filter { it.isAllowlist }.map { it.domain }
        val customBlocks = entries.filter { !it.isAllowlist }.map { it.domain }
        matcher.setAllowlist(allows)
        matcher.setBlocklist(seedBlocklist + customBlocks)
    }

    fun getAllEntries(): Flow<List<BlocklistEntity>> = dao.getAllEntries()
    fun getBlocklistEntries(): Flow<List<BlocklistEntity>> = dao.getBlocklistEntries()
    fun getAllowlistEntries(): Flow<List<BlocklistEntity>> = dao.getAllowlistEntries()
    fun search(query: String): Flow<List<BlocklistEntity>> = dao.search(query)

    suspend fun addEntry(domain: String, isAllowlist: Boolean, notes: String? = null): Boolean {
        val normalized = DomainValidator.normalize(domain) ?: return false
        dao.insert(
            BlocklistEntity(
                domain = normalized,
                isAllowlist = isAllowlist,
                notes = notes
            )
        )
        refreshRules()
        return true
    }

    suspend fun deleteEntry(entry: BlocklistEntity) {
        dao.delete(entry)
        refreshRules()
    }

    suspend fun clearBlocklist() {
        dao.clearBlocklist()
        refreshRules()
    }

    suspend fun clearAllowlist() {
        dao.clearAllowlist()
        refreshRules()
    }

    suspend fun importList(rawText: String, isAllowlist: Boolean): Int = withContext(Dispatchers.IO) {
        var count = 0
        val entries = mutableListOf<BlocklistEntity>()
        rawText.lines().forEach { line ->
            var cleaned = line.trim()
            // Ignore hosts file syntax "0.0.0.0 domain" or "127.0.0.1 domain"
            if (cleaned.startsWith("0.0.0.0") || cleaned.startsWith("127.0.0.1")) {
                cleaned = cleaned.substringAfter(" ").trim()
            }
            if (cleaned.isNotEmpty() && !cleaned.startsWith("#")) {
                DomainValidator.normalize(cleaned)?.let { norm ->
                    entries.add(
                        BlocklistEntity(
                            domain = norm,
                            isAllowlist = isAllowlist,
                            notes = "Imported"
                        )
                    )
                    count++
                }
            }
        }
        if (entries.isNotEmpty()) {
            dao.insertAll(entries)
            refreshRules()
        }
        count
    }

    suspend fun exportList(isAllowlist: Boolean): String = withContext(Dispatchers.IO) {
        val entries = if (isAllowlist) dao.getAllowlistEntries().first() else dao.getBlocklistEntries().first()
        entries.joinToString("\n") { it.domain }
    }

    companion object {
        @Volatile
        private var INSTANCE: FilterRepository? = null

        fun getInstance(context: Context): FilterRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = FilterRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

