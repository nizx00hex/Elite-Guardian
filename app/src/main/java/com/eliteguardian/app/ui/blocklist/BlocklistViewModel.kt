package com.eliteguardian.app.ui.blocklist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteguardian.app.EliteGuardianApp
import com.eliteguardian.app.data.database.BlocklistEntity
import com.eliteguardian.app.data.repository.FilterRepository
import com.eliteguardian.app.domain.filtering.DomainValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BlocklistViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as EliteGuardianApp
    private val filterRepo: FilterRepository = app.filterRepository

    val searchQuery = MutableStateFlow("")
    val selectedTab = MutableStateFlow(0) // 0: Blocklist, 1: Allowlist

    private val _addDomainError = MutableStateFlow<String?>(null)
    val addDomainError: StateFlow<String?> = _addDomainError.asStateFlow()

    private val _importResult = MutableStateFlow<String?>(null)
    val importResult: StateFlow<String?> = _importResult.asStateFlow()

    val blocklistEntries: StateFlow<List<BlocklistEntity>> = combine(
        filterRepo.getBlocklistEntries(),
        searchQuery
    ) { entries, query ->
        if (query.isBlank()) entries
        else entries.filter { it.domain.contains(query.trim(), ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allowlistEntries: StateFlow<List<BlocklistEntity>> = combine(
        filterRepo.getAllowlistEntries(),
        searchQuery
    ) { entries, query ->
        if (query.isBlank()) entries
        else entries.filter { it.domain.contains(query.trim(), ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getSeedCount(): Int = filterRepo.matcher.getBlocklistCount()

    fun addDomain(domain: String, isAllowlist: Boolean, notes: String? = null, onComplete: () -> Unit) {
        viewModelScope.launch {
            val normalized = DomainValidator.normalize(domain)
            if (normalized == null) {
                _addDomainError.value = "Invalid domain syntax. Example: example.com"
                return@launch
            }
            val success = filterRepo.addEntry(normalized, isAllowlist, notes)
            if (success) {
                _addDomainError.value = null
                onComplete()
            } else {
                _addDomainError.value = "Could not add entry"
            }
        }
    }

    fun deleteEntry(entry: BlocklistEntity) {
        viewModelScope.launch {
            filterRepo.deleteEntry(entry)
        }
    }

    fun importList(rawText: String, isAllowlist: Boolean) {
        viewModelScope.launch {
            val count = filterRepo.importList(rawText, isAllowlist)
            _importResult.value = "Successfully imported $count domains."
        }
    }

    fun clearImportResult() {
        _importResult.value = null
    }

    fun clearAddDomainError() {
        _addDomainError.value = null
    }

    suspend fun exportCurrentList(isAllowlist: Boolean): String {
        return filterRepo.exportList(isAllowlist)
    }
}

