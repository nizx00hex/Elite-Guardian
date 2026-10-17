package com.eliteguardian.app.ui.settings

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eliteguardian.app.receiver.GuardianDeviceAdminReceiver
import com.eliteguardian.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToSearchRestrictions: () -> Unit = {},
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isSafeSearch by viewModel.isSafeSearch.collectAsState(initial = true)
    val hasPin by viewModel.hasPinConfigured.collectAsState()
    val isDeviceAdminPref by viewModel.isDeviceAdmin.collectAsState(initial = false)
    val isFocusActive by viewModel.isFocusActive.collectAsState(initial = false)
    val message by viewModel.message.collectAsState()

    val dpm = remember { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    val adminComponent = remember { ComponentName(context, GuardianDeviceAdminReceiver::class.java) }
    var isSystemAdminActive by remember { mutableStateOf(dpm.isAdminActive(adminComponent)) }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val isAdmin = dpm.isAdminActive(adminComponent)
                isSystemAdminActive = isAdmin
                if (isAdmin) {
                    viewModel.syncDeviceAdminState(true)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showSetPinDialog by remember { mutableStateOf(false) }
    var showRemovePinDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAdminEnableDialog by remember { mutableStateOf(false) }
    var showAdminDisableDialog by remember { mutableStateOf(false) }
    var showAdminInfoDialog by remember { mutableStateOf(false) }

    var pinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var pinDialogError by remember { mutableStateOf<String?>(null) }

    message?.let { msg ->
        LaunchedEffect(msg) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = Typography.titleLarge, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Filtering Section
            SectionHeader("Filtering Rules")
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enforce SafeSearch", style = Typography.titleMedium, color = TextPrimary)
                        Text(
                            "Locks Google, Bing, YouTube & DuckDuckGo into strict safe mode.",
                            style = Typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = isSafeSearch,
                        onCheckedChange = { viewModel.toggleSafeSearch(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkBackground,
                            checkedTrackColor = GlowWhite,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = DarkSurfaceVariant
                        )
                    )
                }

                HorizontalDivider(color = DarkSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))

                SettingsRow(
                    title = "Search Restrictions",
                    subtitle = "Block search engine portals & manage custom search domains",
                    icon = Icons.Default.SearchOff,
                    onClick = onNavigateToSearchRestrictions
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tamper Protection Section
            SectionHeader("Anti-Tamper & Security")
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingsRow(
                    title = if (hasPin) "Change or Remove PIN" else "Set Security PIN",
                    subtitle = if (hasPin) "PIN is active. Required to stop protection or modify rules." else "Protect your settings against impulsive tampering.",
                    icon = Icons.Default.Lock,
                    onClick = {
                        pinInput = ""
                        confirmPinInput = ""
                        pinDialogError = null
                        if (hasPin) showRemovePinDialog = true else showSetPinDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dedicated Admin Protection Mode Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(DarkSurfaceVariant, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isFocusActive) Icons.Default.Lock else Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (isFocusActive) WarningAmber else GlowWhite,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Admin Protection Mode",
                                    style = Typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val (statusText, statusColor) = when {
                                    isFocusActive && isSystemAdminActive -> "Locked by Focus Mode" to WarningAmber
                                    isSystemAdminActive -> "Enabled" to GlowWhite
                                    isDeviceAdminPref -> "Permission Required" to WarningAmber
                                    else -> "Disabled" to TextSecondary
                                }
                                Box(
                                    modifier = Modifier
                                        .background(DarkSurfaceVariant, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Status: $statusText",
                                        style = Typography.labelSmall,
                                        color = statusColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Switch(
                            checked = isSystemAdminActive,
                            enabled = !isFocusActive,
                            onCheckedChange = { targetChecked ->
                                if (isFocusActive) {
                                    Toast.makeText(context, "🔒 Locked during active Focus Mode. Cannot be deactivated until Focus Mode finishes.", Toast.LENGTH_LONG).show()
                                } else if (targetChecked) {
                                    showAdminEnableDialog = true
                                } else {
                                    showAdminDisableDialog = true
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkBackground,
                                checkedTrackColor = GlowWhite,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = DarkSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Adds an extra step before protection can be disabled.",
                        style = Typography.bodyMedium,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = DarkSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdminInfoDialog = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "How it works & limitations (Tap to learn more)",
                            style = Typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Privacy & Information Section
            SectionHeader("Privacy & Transparency")
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsRow(
                        title = "Privacy Policy",
                        subtitle = "100% offline, zero accounts, zero analytics, zero data sharing.",
                        icon = Icons.Default.PrivacyTip,
                        onClick = { showPrivacyDialog = true }
                    )
                    HorizontalDivider(color = DarkSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        title = "About EliteGuardian",
                        subtitle = "Version 1.0.0 (Free & Open Source)",
                        icon = Icons.Default.Info,
                        onClick = {
                            Toast.makeText(context, "EliteGuardian v1.0.0 — Built with Kotlin & Jetpack Compose", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Danger Zone
            SectionHeader("Maintenance")
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingsRow(
                    title = "Reset All Settings & Lists",
                    subtitle = "Clears all custom domains, reset PIN, and restore defaults.",
                    icon = Icons.Default.DeleteForever,
                    iconTint = ErrorRose,
                    enabled = !isFocusActive,
                    onClick = { showResetConfirmDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Set PIN Dialog
    if (showSetPinDialog) {
        AlertDialog(
            onDismissRequest = { showSetPinDialog = false },
            title = { Text("Set Security PIN", color = TextPrimary) },
            text = {
                Column {
                    Text("Enter a 4-digit or longer PIN:", style = Typography.bodyMedium, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        singleLine = true,
                        placeholder = { Text("Enter PIN", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = { confirmPinInput = it },
                        singleLine = true,
                        placeholder = { Text("Confirm PIN", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    pinDialogError?.let { err ->
                        Text(err, color = ErrorRose, style = Typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput != confirmPinInput) {
                            pinDialogError = "PINs do not match."
                        } else {
                            viewModel.setPin(pinInput) {
                                showSetPinDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryIndigo,
                        contentColor = DarkBackground
                    )
                ) {
                    Text("Save PIN", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetPinDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Remove PIN Dialog
    if (showRemovePinDialog) {
        AlertDialog(
            onDismissRequest = { showRemovePinDialog = false },
            title = { Text("Remove Security PIN", color = TextPrimary) },
            text = {
                Column {
                    Text("Enter your current PIN to remove protection:", style = Typography.bodyMedium, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        singleLine = true,
                        placeholder = { Text("Current PIN", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removePin(pinInput) {
                            showRemovePinDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRose)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemovePinDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Privacy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("EliteGuardian Privacy Policy", color = TextPrimary) },
            text = {
                Text(
                    "• 100% On-Device: All domain filtering occurs strictly within a local loopback VPN.\n\n" +
                            "• Zero Browsing History Uploads: Your network queries never leave your device.\n\n" +
                            "• No Accounts or Telemetry: EliteGuardian does not collect analytics, emails, or hardware IDs.\n\n" +
                            "• Open & Auditable: Free, open source, and privacy-first.",
                    style = Typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryIndigo,
                        contentColor = DarkBackground
                    )
                ) {
                    Text("Understood", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Reset Confirmation Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Confirm Full Reset", color = TextPrimary) },
            text = {
                Text(
                    "Are you sure you want to reset all custom blocklists, allowlists, and PIN settings? This action cannot be undone.",
                    style = Typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllSettings()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRose)
                ) {
                    Text("Reset Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Admin Protection Enable Confirmation Dialog
    if (showAdminEnableDialog) {
        AlertDialog(
            onDismissRequest = { showAdminEnableDialog = false },
            title = { Text("Enable Admin Protection Mode", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Admin Protection Mode registers EliteGuardian as an Android Device Administrator.",
                        style = Typography.bodyMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Why this is needed:",
                        style = Typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = GlowWhite
                    )
                    Text(
                        "• Creates a friction barrier against impulsive uninstallation during moments of temptation.\n• Android requires you to explicitly deactivate administrator rights before the app can be removed.",
                        style = Typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Transparency & Safety:",
                        style = Typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = GlowWhite
                    )
                    Text(
                        "• Zero invasive policies: no screen lock, no remote wipe, no password resets, no camera blocking.\n• EliteGuardian does not block access to Android settings.",
                        style = Typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Tap below to proceed to Android's official Device Administrator screen.",
                        style = Typography.bodySmall,
                        color = TextMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAdminEnableDialog = false
                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                            putExtra(
                                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                "EliteGuardian uses Device Administrator privileges solely to prevent accidental or impulsive uninstallation."
                            )
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open Device Admin screen: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlowWhite,
                        contentColor = DarkBackground
                    )
                ) {
                    Text("Continue to Android Settings", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminEnableDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Admin Protection Disable Confirmation Dialog
    if (showAdminDisableDialog) {
        AlertDialog(
            onDismissRequest = { showAdminDisableDialog = false },
            title = { Text("Disable Admin Protection Mode?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Disabling administrator access removes the extra friction protecting your habits, making it easier to uninstall or bypass protection.",
                        style = Typography.bodyMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Official Android Deactivation Steps:",
                        style = Typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = GlowWhite
                    )
                    Text(
                        "1. Tap 'Open Android Settings' below.\n2. Select EliteGuardian from the Device Administrators list.\n3. Tap 'Deactivate this device admin app'.",
                        style = Typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "EliteGuardian will automatically verify the permission state when you return.",
                        style = Typography.bodySmall,
                        color = TextMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAdminDisableDialog = false
                        var launched = false
                        val adminSettingsIntent = Intent().apply {
                            action = "android.settings.DEVICE_ADMIN_SETTINGS"
                        }
                        try {
                            context.startActivity(adminSettingsIntent)
                            launched = true
                        } catch (_: Exception) {}

                        if (!launched) {
                            try {
                                context.startActivity(Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS))
                                launched = true
                            } catch (_: Exception) {}
                        }

                        if (!launched) {
                            Toast.makeText(context, "Please navigate to Android Settings > Security > Device Admin apps to deactivate.", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlowWhite,
                        contentColor = DarkBackground
                    )
                ) {
                    Text("Open Android Settings", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminDisableDialog = false }) {
                    Text("Keep Active", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Admin Protection Information & Limitations Dialog
    if (showAdminInfoDialog) {
        AlertDialog(
            onDismissRequest = { showAdminInfoDialog = false },
            title = { Text("Admin Protection Details & Limitations", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "What Admin Protection Mode Does:",
                        style = Typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = GlowWhite
                    )
                    Text(
                        "• Prevents instantaneous uninstallation by registering as an Android Device Administrator.\n• Requires a deliberate deactivation step in Android Settings before uninstallation can occur.",
                        style = Typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Limitations & Scope:",
                        style = Typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = GlowWhite
                    )
                    Text(
                        "• This is NOT Mobile Device Management (MDM) or Device Owner mode.\n• It does not take over your phone, lock you out, or block Android settings.\n• Zero access to personal photos, files, messages, or calls.\n• You can deactivate it at any time directly through Android system settings.",
                        style = Typography.bodySmall,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAdminInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlowWhite,
                        contentColor = DarkBackground
                    )
                ) {
                    Text("Got It", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = Typography.titleMedium,
        color = TextSecondary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color = PrimaryIndigoLight,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) iconTint else TextMuted,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = Typography.titleMedium,
                color = if (enabled) TextPrimary else TextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = Typography.bodyMedium,
                color = TextMuted
            )
        }
        if (enabled) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
        }
    }
}
