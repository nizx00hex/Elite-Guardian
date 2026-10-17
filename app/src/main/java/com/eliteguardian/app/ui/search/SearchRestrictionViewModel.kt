package com.eliteguardian.app.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteguardian.app.EliteGuardianApp
import com.eliteguardian.app.data.database.CustomSearchDomainEntity
import com.eliteguardian.app.data.preferences.AppPreferences
import com.eliteguardian.app.data.repository.FilterRepository
import com.eliteguardian.app.data.repository.SearchRestrictionRepository
import com.eliteguardian.app.domain.filtering.DomainValidator
import com.eliteguardian.app.domain.search.SupportedSearchEngine
import com.eliteguardian.app.vpn.GuardianVpnService
import com.eliteguardian.app.vpn.VpnStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SearchRestrictionViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as EliteGuardianApp
    private val searchRepo = SearchRestrictionRepository.getInstance(application)
    private val filterRepo = FilterRepository.getInstance(application)
    private val appPreferences: AppPreferences = app.appPreferences

    val isMasterEnabled: StateFlow<Boolean> = searchRepo.isMasterEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isFocusRestrictionEnabled: StateFlow<Boolean> = searchRepo.isFocusRestrictionEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val disabledEngines: StateFlow<Set<String>> = searchRepo.disabledEngineIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val customDomains: StateFlow<List<CustomSearchDomainEntity>> = searchRepo.customDomains
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeBlockedDomains: StateFlow<Set<String>> = searchRepo.activeSearchBlockedDomains
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val vpnStatus: StateFlow<VpnStatus> = GuardianVpnService.vpnStatus

    val isSafeSearchEnforced: StateFlow<Boolean> = appPreferences.isSafeSearchEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isFocusModeActive: StateFlow<Boolean> = appPreferences.isFocusModeActive
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult: StateFlow<String?> = _testResult.asStateFlow()

    fun toggleMasterSwitch(enabled: Boolean) {
        viewModelScope.launch {
            searchRepo.setMasterEnabled(enabled)
            filterRepo.refreshRules()
            _uiMessage.value = if (enabled) "Search restrictions activated." else "Search restrictions disabled."
        }
    }

    fun toggleFocusRestriction(enabled: Boolean) {
        viewModelScope.launch {
            searchRepo.setFocusRestrictionEnabled(enabled)
            filterRepo.refreshRules()
            _uiMessage.value = if (enabled) "Focus Mode will restrict search." else "Focus Mode search restrictions turned off."
        }
    }

    fun toggleSafeSearchEnforcement(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setSafeSearchEnabled(enabled)
            _uiMessage.value = if (enabled) "SafeSearch enforcement enabled." else "SafeSearch enforcement disabled."
        }
    }

    fun toggleEngine(engine: SupportedSearchEngine, block: Boolean) {
        viewModelScope.launch {
            searchRepo.toggleSearchEngine(engine.id, block)
            filterRepo.refreshRules()
            _uiMessage.value = if (block) "${engine.displayName} blocked." else "${engine.displayName} unblocked."
        }
    }

    fun addCustomDomain(domain: String, label: String? = null, onComplete: () -> Unit) {
        viewModelScope.launch {
            val normalized = DomainValidator.normalize(domain)
            if (normalized == null) {
                _uiMessage.value = "Invalid domain syntax. Example: search.brave.com"
                return@launch
            }
            val success = searchRepo.addCustomDomain(normalized, label)
            if (success) {
                filterRepo.refreshRules()
                _uiMessage.value = "Added $normalized to search blocklist."
                onComplete()
            } else {
                _uiMessage.value = "Failed to add domain."
            }
        }
    }

    fun toggleCustomDomain(entity: CustomSearchDomainEntity, enabled: Boolean) {
        viewModelScope.launch {
            searchRepo.toggleCustomDomain(entity, enabled)
            filterRepo.refreshRules()
        }
    }

    fun removeCustomDomain(entity: CustomSearchDomainEntity) {
        viewModelScope.launch {
            searchRepo.removeCustomDomain(entity)
            filterRepo.refreshRules()
            _uiMessage.value = "Removed ${entity.domain}"
        }
    }

    fun testDomainStatus(domainInput: String) {
        val normalized = DomainValidator.normalize(domainInput)
        if (normalized == null) {
            _testResult.value = "INVALID: '$domainInput' is not a valid domain syntax."
            return
        }

        val isBlocked = filterRepo.matcher.isBlocked(normalized)
        if (isBlocked) {
            _testResult.value = "BLOCKED: Traffic to '$normalized' is actively sinkholed by EliteGuardian."
        } else {
            _testResult.value = "ALLOWED: Traffic to '$normalized' is currently permitted by active rules."
        }
    }

    fun restoreDefaults() {
        viewModelScope.launch {
            searchRepo.restoreDefaultConfiguration()
            filterRepo.refreshRules()
            _uiMessage.value = "Restored default search restriction settings."
        }
    }

    fun dismissMessage() {
        _uiMessage.value = null
    }

    fun dismissTestResult() {
        _testResult.value = null
    }
}

