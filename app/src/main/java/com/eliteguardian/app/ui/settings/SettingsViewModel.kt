package com.eliteguardian.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteguardian.app.EliteGuardianApp
import com.eliteguardian.app.data.preferences.AppPreferences
import com.eliteguardian.app.data.repository.FilterRepository
import com.eliteguardian.app.security.PinManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as EliteGuardianApp
    private val appPreferences: AppPreferences = app.appPreferences
    private val filterRepo: FilterRepository = app.filterRepository

    val isSafeSearch = appPreferences.isSafeSearchEnabled
    val hasPinConfigured = MutableStateFlow(false)
    val isDeviceAdmin = appPreferences.isDeviceAdminEnabled
    val isFocusActive = appPreferences.isFocusModeActive
    val cooldownMinutes = appPreferences.cooldownMinutes

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        checkPinStatus()
    }

    private fun checkPinStatus() {
        viewModelScope.launch {
            val hash = appPreferences.pinHash.first()
            hasPinConfigured.value = !hash.isNullOrBlank()
        }
    }

    fun toggleSafeSearch(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setSafeSearchEnabled(enabled)
        }
    }

    fun setPin(pin: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            if (pin.length < 4) {
                _message.value = "PIN must be at least 4 digits."
                return@launch
            }
            val salt = PinManager.generateSalt()
            val hash = PinManager.hashPin(pin, salt)
            appPreferences.setPin(hash, salt)
            hasPinConfigured.value = true
            _message.value = "Security PIN set successfully."
            onComplete()
        }
    }

    fun removePin(currentPin: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val hash = appPreferences.pinHash.first()
            val salt = appPreferences.pinSalt.first()
            if (hash != null && salt != null) {
                if (PinManager.verifyPin(currentPin, salt, hash)) {
                    appPreferences.setPin(null, null)
                    hasPinConfigured.value = false
                    _message.value = "Security PIN removed."
                    onComplete()
                } else {
                    _message.value = "Incorrect PIN."
                }
            }
        }
    }

    fun setCooldown(minutes: Int) {
        viewModelScope.launch {
            appPreferences.setCooldownMinutes(minutes)
        }
    }

    fun resetAllSettings() {
        viewModelScope.launch {
            filterRepo.clearBlocklist()
            filterRepo.clearAllowlist()
            appPreferences.setPin(null, null)
            appPreferences.setSafeSearchEnabled(true)
            appPreferences.setFocusMode(false, 0L)
            hasPinConfigured.value = false
            _message.value = "All custom lists and settings have been reset."
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
