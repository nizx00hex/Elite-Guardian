package com.eliteguardian.app.data.repository

import android.content.Context
import com.eliteguardian.app.data.database.AppDatabase
import com.eliteguardian.app.data.database.CustomSearchDomainDao
import com.eliteguardian.app.data.database.CustomSearchDomainEntity
import com.eliteguardian.app.data.preferences.AppPreferences
import com.eliteguardian.app.domain.filtering.DomainValidator
import com.eliteguardian.app.domain.search.SupportedSearchEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchRestrictionRepository(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val database = AppDatabase.getInstance(context)
    private val customDao: CustomSearchDomainDao = database.customSearchDomainDao()
    private val appPreferences = AppPreferences(context)

    val isMasterEnabled: Flow<Boolean> = appPreferences.isSearchRestrictionsEnabled
    val isFocusRestrictionEnabled: Flow<Boolean> = appPreferences.isSearchFocusRestrictionEnabled
    val disabledEngineIds: Flow<Set<String>> = appPreferences.disabledSearchEngines
    val customDomains: Flow<List<CustomSearchDomainEntity>> = customDao.getAllFlow()

    private val _activeSearchBlockedDomains = MutableStateFlow<Set<String>>(emptySet())
    val activeSearchBlockedDomains = _activeSearchBlockedDomains.asStateFlow()

    init {
        observeRestrictions()
    }

    private fun observeRestrictions() {
        scope.launch {
            combine(
                appPreferences.isSearchRestrictionsEnabled,
                appPreferences.isFocusModeActive,
                appPreferences.isSearchFocusRestrictionEnabled,
                appPreferences.disabledSearchEngines,
                customDao.getAllFlow()
            ) { masterEnabled, isFocus, focusRestricted, disabledEngines, customList ->
                computeEffectiveBlockedDomains(
                    masterEnabled = masterEnabled,
                    isFocus = isFocus,
                    focusRestricted = focusRestricted,
                    disabledEngines = disabledEngines,
                    customList = customList
                )
            }.collect { domains ->
                _activeSearchBlockedDomains.value = domains
            }
        }
    }

    fun computeEffectiveBlockedDomains(
        masterEnabled: Boolean,
        isFocus: Boolean,
        focusRestricted: Boolean,
        disabledEngines: Set<String>,
        customList: List<CustomSearchDomainEntity>
    ): Set<String> {
        val shouldApply = masterEnabled || (isFocus && focusRestricted)
        if (!shouldApply) return emptySet()

        val domains = mutableSetOf<String>()

        // 1. Built-in engines configured to be restricted
        disabledEngines.forEach { id ->
            val engine = SupportedSearchEngine.fromId(id)
            if (engine != null) {
                domains.addAll(engine.domains)
            }
        }

        // 2. Custom search domains that are enabled
        customList.filter { it.isEnabled }.forEach { custom ->
            domains.add(custom.domain)
        }

        return domains
    }

    suspend fun setMasterEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        appPreferences.setSearchRestrictionsEnabled(enabled)
    }

    suspend fun setFocusRestrictionEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        appPreferences.setSearchFocusRestrictionEnabled(enabled)
    }

    suspend fun toggleSearchEngine(engineId: String, disableEngine: Boolean) = withContext(Dispatchers.IO) {
        val current = appPreferences.disabledSearchEngines.first().toMutableSet()
        if (disableEngine) {
            current.add(engineId)
        } else {
            current.remove(engineId)
        }
        appPreferences.setDisabledSearchEngines(current)
    }

    suspend fun addCustomDomain(domain: String, label: String? = null): Boolean = withContext(Dispatchers.IO) {
        val normalized = DomainValidator.normalize(domain) ?: return@withContext false
        customDao.insert(
            CustomSearchDomainEntity(
                domain = normalized,
                isEnabled = true,
                label = label
            )
        )
        true
    }

    suspend fun toggleCustomDomain(entity: CustomSearchDomainEntity, enabled: Boolean) = withContext(Dispatchers.IO) {
        customDao.update(entity.copy(isEnabled = enabled))
    }

    suspend fun removeCustomDomain(entity: CustomSearchDomainEntity) = withContext(Dispatchers.IO) {
        customDao.delete(entity)
    }

    suspend fun restoreDefaultConfiguration() = withContext(Dispatchers.IO) {
        // Defaults: Master disabled, Focus restriction enabled, all supported engines unblocked, clear custom search domains
        appPreferences.setSearchRestrictionsEnabled(false)
        appPreferences.setSearchFocusRestrictionEnabled(true)
        appPreferences.setDisabledSearchEngines(emptySet())
        customDao.clearAll()
    }

    companion object {
        @Volatile
        private var INSTANCE: SearchRestrictionRepository? = null

        fun getInstance(context: Context): SearchRestrictionRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = SearchRestrictionRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

