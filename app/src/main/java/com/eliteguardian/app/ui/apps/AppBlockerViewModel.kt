package com.eliteguardian.app.ui.apps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteguardian.app.data.repository.AppBlockerRepository
import com.eliteguardian.app.data.repository.AppInfoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppBlockerViewModel(application: Application) : AndroidViewModel(application) {

    private val appBlockerRepo = AppBlockerRepository.getInstance(application)

    private val _rawApps = MutableStateFlow<List<AppInfoItem>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val searchQuery = MutableStateFlow("")

    val filteredApps: StateFlow<List<AppInfoItem>> = combine(
        _rawApps,
        searchQuery
    ) { apps, query ->
        if (query.isBlank()) apps
        else apps.filter {
            it.appName.contains(query.trim(), ignoreCase = true) ||
                    it.packageName.contains(query.trim(), ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadInstalledApps()
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoading.value = true
            _rawApps.value = appBlockerRepo.getInstalledApps()
            _isLoading.value = false
        }
    }

    fun toggleApp(item: AppInfoItem, isBlocked: Boolean) {
        viewModelScope.launch {
            appBlockerRepo.toggleAppBlock(item.packageName, item.appName, isBlocked)
            _rawApps.value = _rawApps.value.map {
                if (it.packageName == item.packageName) it.copy(isBlocked = isBlocked)
                else it
            }
        }
    }
}

