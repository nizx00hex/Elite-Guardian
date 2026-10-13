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
        Toast.makeText(context, "EliteGuardian Tamper Protection Enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        val appPreferences = AppPreferences(context)
        CoroutineScope(Dispatchers.IO).launch {
            appPreferences.setDeviceAdminEnabled(false)
        }
        Toast.makeText(context, "EliteGuardian Tamper Protection Disabled", Toast.LENGTH_SHORT).show()
    }
}

