package com.eliteguardian.app.ui.focus

import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteguardian.app.EliteGuardianApp
import com.eliteguardian.app.data.preferences.AppPreferences
import com.eliteguardian.app.receiver.GuardianDeviceAdminReceiver
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

    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, GuardianDeviceAdminReceiver::class.java)
        return dpm.isAdminActive(adminComponent)
    }

    fun startFocusSession(
        context: Context,
        durationMinutes: Int,
        onRequireDeviceAdmin: () -> Unit,
        onStarted: () -> Unit
    ) {
        if (!isDeviceAdminActive(context)) {
            onRequireDeviceAdmin()
            return
        }

        viewModelScope.launch {
            val endTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
            appPreferences.setFocusMode(true, endTime)
            onStarted()
        }
    }
}
