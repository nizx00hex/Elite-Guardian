package com.eliteguardian.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.eliteguardian.app.data.preferences.AppPreferences
import com.eliteguardian.app.data.repository.AppBlockerRepository
import com.eliteguardian.app.data.repository.KeywordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GuardianAccessibilityService : AccessibilityService() {

    private lateinit var keywordRepo: KeywordRepository
    private lateinit var appBlockerRepo: AppBlockerRepository
    private lateinit var appPreferences: AppPreferences
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO)

    private var lastToastTimestamp = 0L

    override fun onCreate() {
        super.onCreate()
        keywordRepo = KeywordRepository.getInstance(applicationContext)
        appBlockerRepo = AppBlockerRepository.getInstance(applicationContext)
        appPreferences = AppPreferences(applicationContext)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        keywordRepo = KeywordRepository.getInstance(applicationContext)
        appBlockerRepo = AppBlockerRepository.getInstance(applicationContext)
        appPreferences = AppPreferences(applicationContext)

        // Programmatically guarantee all event types and view flags are subscribed
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = info.flags or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        info.notificationTimeout = 50
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val currentPackage = event.packageName?.toString() ?: return

        // Never intercept EliteGuardian itself or critical Android system framework
        if (currentPackage == packageName ||
            currentPackage == "com.android.systemui" ||
            currentPackage == "android"
        ) {
            return
        }

        val now = System.currentTimeMillis()

        // 0. Focus Mode Lock: Block any attempt to deactivate Device Admin or uninstall EliteGuardian
        if (appPreferences.isFocusModeActiveSync()) {
            if (isTamperAttemptDuringFocus(event, currentPackage)) {
                performGlobalAction(GLOBAL_ACTION_HOME)
                performGlobalAction(GLOBAL_ACTION_BACK)
                if (now - lastToastTimestamp > 2500) {
                    lastToastTimestamp = now
                    showToast("🔒 Locked by Focus Mode: EliteGuardian cannot be deactivated or uninstalled until your focus timer completes.")
                }
                return
            }
        }

        // 1. Check if the active or root window application is in blocked list
        val activeWindowPackage = try {
            rootInActiveWindow?.packageName?.toString()
        } catch (e: Exception) {
            null
        }

        val isCurrentBlocked = appBlockerRepo.isAppBlocked(currentPackage)
        val isActiveWindowBlocked = activeWindowPackage != null &&
                activeWindowPackage != packageName &&
                activeWindowPackage != "com.android.systemui" &&
                activeWindowPackage != "android" &&
                appBlockerRepo.isAppBlocked(activeWindowPackage)

        if (isCurrentBlocked || isActiveWindowBlocked) {
            // Kick immediately out to launcher
            performGlobalAction(GLOBAL_ACTION_HOME)
            performGlobalAction(GLOBAL_ACTION_BACK)
            if (now - lastToastTimestamp > 2500) {
                lastToastTimestamp = now
                showToast("EliteGuardian: This application is blocked!")
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
        val contentDesc = event.contentDescription?.toString() ?: ""

        val combinedText = "$eventText $sourceText $contentDesc".trim()
        if (combinedText.isNotEmpty()) {
            val matchedKeyword = keywordRepo.containsRestrictedKeyword(combinedText)
            if (matchedKeyword != null) {
                // Immediately close the application where the user is typing
                performGlobalAction(GLOBAL_ACTION_HOME)
                performGlobalAction(GLOBAL_ACTION_BACK)
                if (now - lastToastTimestamp > 2500) {
                    lastToastTimestamp = now
                    showToast("EliteGuardian: Restricted keyword '$matchedKeyword' detected! App closed.")
                    scope.launch { appPreferences.incrementBlockedCount() }
                }
            }
        }
    }

    override fun onInterrupt() {
        // Required by AccessibilityService
    }

    private fun isTamperAttemptDuringFocus(event: AccessibilityEvent, currentPackage: String): Boolean {
        val targetPackages = setOf(
            "com.android.settings",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller"
        )

        val isTargetPkg = targetPackages.contains(currentPackage) ||
                currentPackage.contains("packageinstaller") ||
                currentPackage.contains("settings")

        if (!isTargetPkg) return false

        val eventText = event.text.joinToString(" ")
        val contentDesc = event.contentDescription?.toString() ?: ""
        val className = event.className?.toString() ?: ""

        val mentionsEliteGuardianInEvent = eventText.contains("EliteGuardian", ignoreCase = true) ||
                contentDesc.contains("EliteGuardian", ignoreCase = true) ||
                className.contains("DeviceAdminAdd", ignoreCase = true)

        var mentionsEliteGuardianInWindow = false
        val rootNode = try {
            rootInActiveWindow
        } catch (_: Exception) {
            null
        }

        if (rootNode != null) {
            try {
                val matches = rootNode.findAccessibilityNodeInfosByText("EliteGuardian")
                if (!matches.isNullOrEmpty()) {
                    mentionsEliteGuardianInWindow = true
                }
            } catch (_: Exception) {}
        }

        if (!mentionsEliteGuardianInEvent && !mentionsEliteGuardianInWindow) {
            return false
        }

        val tamperKeywords = listOf(
            "deactivate",
            "uninstall",
            "turn off",
            "disable",
            "force stop",
            "clear data",
            "clear storage",
            "remove",
            "device admin",
            "administrator"
        )

        val fullWindowText = buildString {
            append(eventText).append(" ")
            append(contentDesc).append(" ")
            append(className).append(" ")
            rootNode?.let { extractTextRecursive(it, this, maxDepth = 4) }
        }.lowercase()

        return tamperKeywords.any { fullWindowText.contains(it) }
    }

    private fun extractTextRecursive(node: android.view.accessibility.AccessibilityNodeInfo, sb: StringBuilder, maxDepth: Int) {
        if (maxDepth <= 0) return
        node.text?.let { sb.append(it).append(" ") }
        node.contentDescription?.let { sb.append(it).append(" ") }
        for (i in 0 until node.childCount) {
            val child = try { node.getChild(i) } catch (_: Exception) { null }
            if (child != null) {
                extractTextRecursive(child, sb, maxDepth - 1)
            }
        }
    }

    private fun showToast(message: String) {
        mainHandler.post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }
}
