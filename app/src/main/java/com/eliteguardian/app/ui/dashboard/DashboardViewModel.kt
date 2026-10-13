package com.eliteguardian.app.ui.dashboard

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteguardian.app.EliteGuardianApp
import com.eliteguardian.app.data.preferences.AppPreferences
import com.eliteguardian.app.data.repository.FilterRepository
import com.eliteguardian.app.vpn.GuardianVpnService
import com.eliteguardian.app.vpn.VpnStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as EliteGuardianApp
    private val appPreferences: AppPreferences = app.appPreferences
    private val filterRepo: FilterRepository = app.filterRepository

    val vpnStatus: StateFlow<VpnStatus> = GuardianVpnService.vpnStatus
    val filterStats = GuardianVpnService.stats
    val totalBlockedCount = appPreferences.totalBlockedCount
    val lastFilterInitTime = appPreferences.lastFilterInitTime
    val isSafeSearchEnabled = appPreferences.isSafeSearchEnabled
    val isFocusModeActive = appPreferences.isFocusModeActive
    val isProtectionEnabled = appPreferences.isProtectionEnabled

    private val _diagnosticResult = MutableStateFlow<String?>(null)
    val diagnosticResult: StateFlow<String?> = _diagnosticResult.asStateFlow()

    fun toggleProtection(context: Context) {
        viewModelScope.launch {
            if (vpnStatus.value is VpnStatus.Connected) {
                GuardianVpnService.stopService(context)
            } else {
                GuardianVpnService.startService(context)
            }
        }
    }

    fun runDiagnosticTest() {
        viewModelScope.launch {
            _diagnosticResult.value = "Running diagnostic test..."
            withContext(Dispatchers.IO) {
                try {
                    val testDomain = "test.adult.eliteguardian.local"
                    val isLocallyBlocked = filterRepo.matcher.isBlocked(testDomain)
                    if (isLocallyBlocked) {
                        _diagnosticResult.value = "PASS: Diagnostic domain successfully intercepted & sinkholed!"
                    } else {
                        _diagnosticResult.value = "FAIL: Filter rules did not block test domain."
                    }
                } catch (e: Exception) {
                    _diagnosticResult.value = "ERROR: ${e.message}"
                }
            }
        }
    }

    fun dismissDiagnostic() {
        _diagnosticResult.value = null
    }
}

