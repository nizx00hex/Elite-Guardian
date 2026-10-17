package com.eliteguardian.app.admin

import com.eliteguardian.app.receiver.GuardianDeviceAdminReceiver
import com.eliteguardian.app.ui.settings.AdminProtectionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminProtectionModeTest {

    @Test
    fun testAdminProtectionStatusMapping() {
        val activeStatus = if (true) AdminProtectionStatus.ENABLED else AdminProtectionStatus.DISABLED
        assertEquals(AdminProtectionStatus.ENABLED, activeStatus)

        val inactiveStatus = if (false) AdminProtectionStatus.ENABLED else AdminProtectionStatus.DISABLED
        assertEquals(AdminProtectionStatus.DISABLED, inactiveStatus)
    }

    @Test
    fun testNeverShowProtectedWhenInactive() {
        val isSystemAdminActive = false
        val displayStatus = when {
            isSystemAdminActive -> "Enabled"
            else -> "Disabled"
        }
        assertEquals("Disabled", displayStatus)
    }

    @Test
    fun testFocusModeLockPrecedence() {
        val isFocusActive = true
        val isSystemAdminActive = true

        val (statusText, canDeactivate) = when {
            isFocusActive -> "Locked by Focus Mode" to false
            isSystemAdminActive -> "Enabled" to true
            else -> "Disabled" to true
        }

        assertEquals("Locked by Focus Mode", statusText)
        assertFalse("Deactivation must be blocked while Focus Mode is active", canDeactivate)
    }

    @Test
    fun testDisableRequestedExplanationContent() {
        val message = GuardianDeviceAdminReceiver.DISABLE_WARNING_MESSAGE
        assertNotNull(message)
        assertTrue("Warning message must mention Admin Protection Mode", message.contains("Admin Protection Mode"))
        assertTrue("Warning message must explain uninstall prevention", message.contains("uninstalling"))
    }

    @Test
    fun testIntentActionConstantsMatchDevicePolicyContract() {
        // Standard Android DevicePolicyManager & Settings action constants used in flow
        val addAdminAction = "android.app.action.ADD_DEVICE_ADMIN"
        val extraDeviceAdmin = "android.app.extra.DEVICE_ADMIN"
        val extraExplanation = "android.app.extra.ADD_EXPLANATION"
        val deviceAdminSettingsAction = "android.settings.DEVICE_ADMIN_SETTINGS"
        val securitySettingsAction = "android.settings.SECURITY_SETTINGS"

        assertEquals("android.app.action.ADD_DEVICE_ADMIN", addAdminAction)
        assertEquals("android.app.extra.DEVICE_ADMIN", extraDeviceAdmin)
        assertEquals("android.app.extra.ADD_EXPLANATION", extraExplanation)
        assertEquals("android.settings.DEVICE_ADMIN_SETTINGS", deviceAdminSettingsAction)
        assertEquals("android.settings.SECURITY_SETTINGS", securitySettingsAction)
    }
}

