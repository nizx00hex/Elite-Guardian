package com.eliteguardian.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.eliteguardian.app.data.preferences.AppPreferences
import com.eliteguardian.app.data.repository.AppBlockerRepository
import com.eliteguardian.app.data.repository.KeywordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GuardianAccessibilityService : AccessibilityService() {

    private lateinit var keywordRepo: KeywordRepository
    private lateinit var appBlockerRepo: AppBlockerRepository
    private lateinit var appPreferences: AppPreferences
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO)

    private var lastActionTimestamp = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        keywordRepo = KeywordRepository.getInstance(applicationContext)
        appBlockerRepo = AppBlockerRepository.getInstance(applicationContext)
        appPreferences = AppPreferences(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val currentPackage = event.packageName?.toString() ?: return

        // Never block our own app or essential system navigation
        if (currentPackage == packageName ||
            currentPackage == "com.android.systemui" ||
            currentPackage == "android"
        ) {
            return
        }

        val now = System.currentTimeMillis()

        // 1. Check if the active application is blocked
        if (appBlockerRepo.isAppBlocked(currentPackage)) {
            if (now - lastActionTimestamp > 1500) {
                lastActionTimestamp = now
                performGlobalAction(GLOBAL_ACTION_HOME)
                showToast("EliteGuardian: Application is blocked!")
                scope.launch { appPreferences.incrementBlockedCount() }
            }
            return
        }

        // 2. Keyword detection in typed text or view content
        val eventText = event.text.joinToString(" ")
        val sourceText = try {
            event.source?.text?.toString() ?: ""
        } catch (e: Exception) {
            ""
        }

        val combinedText = "$eventText $sourceText".trim()
        if (combinedText.isNotEmpty()) {
            val matchedKeyword = keywordRepo.containsRestrictedKeyword(combinedText)
            if (matchedKeyword != null) {
                if (now - lastActionTimestamp > 1500) {
                    lastActionTimestamp = now
                    // Immediately close the app where the user is typing
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    showToast("EliteGuardian: Restricted keyword '$matchedKeyword' detected! App closed.")
                    scope.launch { appPreferences.incrementBlockedCount() }
                }
            }
        }
    }

    override fun onInterrupt() {
        // Required by AccessibilityService interface
    }

    private fun showToast(message: String) {
        mainHandler.post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }
}

