package com.eliteguardian.app.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.eliteguardian.app.data.preferences.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GuardianDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        val appPreferences = AppPreferences(context)
        CoroutineScope(Dispatchers.IO).launch {
            appPreferences.setDeviceAdminEnabled(true)
        }
        Toast.makeText(context, "Admin Protection Mode Enabled", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val DISABLE_WARNING_MESSAGE =
            "Disabling Admin Protection Mode removes the extra step required before uninstalling EliteGuardian."
        const val FOCUS_MODE_LOCKED_MESSAGE =
            "⛔ ACTIVE FOCUS SESSION LOCKED!\nDevice Administrator cannot be deactivated while Focus Mode is running. Please wait until your focus timer completes."
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        val appPreferences = AppPreferences(context)
        if (appPreferences.isFocusModeActiveSync()) {
            return FOCUS_MODE_LOCKED_MESSAGE
        }
        return DISABLE_WARNING_MESSAGE
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        val appPreferences = AppPreferences(context)
        CoroutineScope(Dispatchers.IO).launch {
            appPreferences.setDeviceAdminEnabled(false)
        }
        Toast.makeText(context, "Admin Protection Mode Disabled", Toast.LENGTH_SHORT).show()
    }
}

