package com.eliteguardian.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.eliteguardian.app.data.database.AppDatabase
import com.eliteguardian.app.data.preferences.AppPreferences
import com.eliteguardian.app.data.repository.FilterRepository

class EliteGuardianApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var appPreferences: AppPreferences
        private set
    lateinit var filterRepository: FilterRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        appPreferences = AppPreferences(this)
        filterRepository = FilterRepository.getInstance(this)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vpnChannel = NotificationChannel(
                "elite_guardian_vpn_channel",
                getString(R.string.vpn_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.vpn_channel_desc)
                setShowBadge(false)
            }

            val alertChannel = NotificationChannel(
                "elite_guardian_alerts",
                "Protection Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts regarding focus sessions and filter events"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(vpnChannel)
            manager?.createNotificationChannel(alertChannel)
        }
    }

    companion object {
        lateinit var instance: EliteGuardianApp
            private set
    }
}

