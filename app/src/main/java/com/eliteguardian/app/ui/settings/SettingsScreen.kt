package com.eliteguardian.app.ui.settings

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isSafeSearch by viewModel.isSafeSearch.collectAsState(initial = true)
    val hasPin by viewModel.hasPinConfigured.collectAsState()
    val isDeviceAdmin by viewModel.isDeviceAdmin.collectAsState(initial = false)
    val message by viewModel.message.collectAsState()

    var showSetPinDialog by remember { mutableStateOf(false) }
    var showRemovePinDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

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
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryIndigo)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tamper Protection Section
            SectionHeader("Anti-Tamper & Security")
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsRow(
                        title = if (hasPin) "Change or Remove PIN" else "Set Security PIN",
                        subtitle = if (hasPin) "PIN is active. Required to stop protection or focus sessions." else "Protect your settings against impulsive tampering.",
                        icon = Icons.Default.Lock,
                        onClick = {
                            pinInput = ""
                            confirmPinInput = ""
                            pinDialogError = null
                            if (hasPin) showRemovePinDialog = true else showSetPinDialog = true
                        }
                    )
                    HorizontalDivider(color = DarkSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        title = "Device Administrator",
                        subtitle = if (isDeviceAdmin) "Active. Prevents uninstallation without admin removal." else "Make uninstallation harder by registering as Device Admin.",
                        icon = Icons.Default.Security,
                        onClick = {
                            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                            val adminComponent = ComponentName(context, GuardianDeviceAdminReceiver::class.java)
                            if (!dpm.isAdminActive(adminComponent)) {
                                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "EliteGuardian uses Device Admin to prevent impulsive uninstallation.")
                                }
                                context.startActivity(intent)
                            } else {
                                dpm.removeActiveAdmin(adminComponent)
                                Toast.makeText(context, "Device admin privileges removed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
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
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("Save PIN")
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
                Button(onClick = { showPrivacyDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)) {
                    Text("Understood")
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = Typography.titleMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = Typography.bodyMedium, color = TextMuted)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
    }
}

