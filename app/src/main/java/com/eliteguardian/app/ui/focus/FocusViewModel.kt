package com.eliteguardian.app.ui.focus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteguardian.app.EliteGuardianApp
import com.eliteguardian.app.data.preferences.AppPreferences
import com.eliteguardian.app.security.PinManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as EliteGuardianApp
    private val appPreferences: AppPreferences = app.appPreferences

    val isFocusActive = appPreferences.isFocusModeActive
    val focusEndTime = appPreferences.focusEndTime

    private val _remainingTimeFormatted = MutableStateFlow("00:00:00")
    val remainingTimeFormatted: StateFlow<String> = _remainingTimeFormatted.asStateFlow()

    private val _pinPromptRequired = MutableStateFlow(false)
    val pinPromptRequired: StateFlow<Boolean> = _pinPromptRequired.asStateFlow()

    init {
        startTimerLoop()
    }

    private fun startTimerLoop() {
        viewModelScope.launch {
            while (true) {
                val isActive = isFocusActive.first()
                val endTime = focusEndTime.first()
                val now = System.currentTimeMillis()

                if (isActive && endTime > now) {
                    val diff = endTime - now
                    val hours = diff / (1000 * 60 * 60)
                    val minutes = (diff / (1000 * 60)) % 60
                    val seconds = (diff / 1000) % 60
                    _remainingTimeFormatted.value = "%02d:%02d:%02d".format(hours, minutes, seconds)
                } else if (isActive && endTime <= now) {
                    // Session naturally expired
                    appPreferences.setFocusMode(false, 0L)
                    _remainingTimeFormatted.value = "00:00:00"
                } else {
                    _remainingTimeFormatted.value = "00:00:00"
                }
                delay(1000)
            }
        }
    }

    fun startFocusSession(durationMinutes: Int) {
        viewModelScope.launch {
            val endTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
            appPreferences.setFocusMode(true, endTime)
        }
    }

    fun requestStopFocusSession(onRequiresPin: () -> Unit, onStopped: () -> Unit) {
        viewModelScope.launch {
            val savedHash = appPreferences.pinHash.first()
            if (savedHash.isNullOrBlank()) {
                // No PIN configured, can stop directly
                appPreferences.setFocusMode(false, 0L)
                onStopped()
            } else {
                onRequiresPin()
            }
        }
    }

    fun verifyPinAndStop(pin: String, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val savedHash = appPreferences.pinHash.first()
            val savedSalt = appPreferences.pinSalt.first()
            if (savedHash != null && savedSalt != null) {
                if (PinManager.verifyPin(pin, savedSalt, savedHash)) {
                    appPreferences.setFocusMode(false, 0L)
                    onSuccess()
                } else {
                    onError()
                }
            } else {
                appPreferences.setFocusMode(false, 0L)
                onSuccess()
            }
        }
    }
}

