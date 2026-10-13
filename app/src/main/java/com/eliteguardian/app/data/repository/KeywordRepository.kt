package com.eliteguardian.app.data.repository

import android.content.Context
import com.eliteguardian.app.data.database.AppDatabase
import com.eliteguardian.app.data.database.KeywordEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet

class KeywordRepository(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val database = AppDatabase.getInstance(context)
    private val dao = database.keywordDao()

    val activeKeywordSet = CopyOnWriteArraySet<String>()

    private val defaultSeedKeywords = listOf(
        "porn", "xxx", "hentai", "sex", "nude", "erotic", "nsfw",
        "xvideos", "xnxx", "redtube", "youporn", "chaturbate",
        "onlyfans", "camsoda", "erome", "fapello", "cam4", "pornhub"
    )

    init {
        scope.launch {
            if (dao.getCount() == 0) {
                dao.insertAll(defaultSeedKeywords.map { KeywordEntity(keyword = it) })
            }
            dao.getActiveKeywords().distinctUntilChanged().collect { keywords ->
                activeKeywordSet.clear()
                keywords.forEach { activeKeywordSet.add(it.lowercase().trim()) }
            }
        }
    }

    fun getAllKeywords(): Flow<List<KeywordEntity>> = dao.getAllKeywords()

    suspend fun addKeyword(keyword: String): Boolean {
        val trimmed = keyword.trim().lowercase()
        if (trimmed.length < 2) return false
        dao.insert(KeywordEntity(keyword = trimmed))
        activeKeywordSet.add(trimmed)
        return true
    }

    suspend fun deleteKeyword(entity: KeywordEntity) {
        dao.delete(entity)
        activeKeywordSet.remove(entity.keyword.lowercase())
    }

    suspend fun toggleKeyword(id: Long, isEnabled: Boolean) {
        dao.updateEnabledStatus(id, isEnabled)
    }

    fun containsRestrictedKeyword(text: String): String? {
        val lower = text.lowercase()
        for (kw in activeKeywordSet) {
            // Check word boundary or substring match
            if (lower.contains(kw)) {
                return kw
            }
        }
        return null
    }

    companion object {
        @Volatile
        private var INSTANCE: KeywordRepository? = null

        fun getInstance(context: Context): KeywordRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = KeywordRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

