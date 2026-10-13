package com.eliteguardian.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.eliteguardian.app.data.preferences.AppPreferences
import com.eliteguardian.app.vpn.GuardianVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val appPreferences = AppPreferences(context)
            CoroutineScope(Dispatchers.IO).launch {
                val wasProtected = appPreferences.isProtectionEnabled.first()
                if (wasProtected) {
                    GuardianVpnService.startService(context)
                }
            }
        }
    }
}

