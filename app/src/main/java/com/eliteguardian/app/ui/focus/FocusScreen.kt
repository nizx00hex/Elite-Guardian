package com.eliteguardian.app.ui.focus

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eliteguardian.app.receiver.GuardianDeviceAdminReceiver
import com.eliteguardian.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    viewModel: FocusViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isActive by viewModel.isFocusActive.collectAsState(initial = false)
    val remainingTime by viewModel.remainingTimeFormatted.collectAsState()

    var showCustomTimeDialog by remember { mutableStateOf(false) }
    var showDeviceAdminPromptDialog by remember { mutableStateOf(false) }
    var pendingMinutes by remember { mutableStateOf(0) }

    var customHoursInput by remember { mutableStateOf("0") }
    var customMinutesInput by remember { mutableStateOf("30") }
    var customTimeError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Focus Mode", style = Typography.titleLarge, color = TextPrimary) },
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
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Timer Hero Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(if (isActive) WarningAmber.copy(alpha = 0.15f) else DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Default.Lock else Icons.Default.HourglassBottom,
                            contentDescription = null,
                            tint = if (isActive) WarningAmber else TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = if (isActive) remainingTime else "No Active Session",
                        style = Typography.headlineLarge.copy(fontSize = 36.sp),
                        color = if (isActive) WarningAmber else TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isActive)
                            "🔒 Session Strictly Locked. You cannot exit, end, or modify the timer until time completes."
                        else
                            "Select duration or set custom time. Device Administrator is strictly required.",
                        style = Typography.bodyMedium,
                        color = if (isActive) WarningAmber else TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isActive) {
                // Strict Lock Banner (Cannot end or change time!)
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = ShieldGreen)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Device Administrator is active. EliteGuardian cannot be uninstalled or disabled while Focus Mode is running.",
                            style = Typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            } else {
                Text(
                    text = "Quick Presets",
                    style = Typography.titleMedium,
                    color = TextSecondary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PresetButton(modifier = Modifier.weight(1f), label = "15m") {
                        triggerStart(15, context, viewModel, { showDeviceAdminPromptDialog = true })
                    }
                    PresetButton(modifier = Modifier.weight(1f), label = "30m") {
                        triggerStart(30, context, viewModel, { showDeviceAdminPromptDialog = true })
                    }
                    PresetButton(modifier = Modifier.weight(1f), label = "1h") {
                        triggerStart(60, context, viewModel, { showDeviceAdminPromptDialog = true })
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PresetButton(modifier = Modifier.weight(1f), label = "2h") {
                        triggerStart(120, context, viewModel, { showDeviceAdminPromptDialog = true })
                    }
                    PresetButton(modifier = Modifier.weight(1f), label = "4h") {
                        triggerStart(240, context, viewModel, { showDeviceAdminPromptDialog = true })
                    }
                    PresetButton(modifier = Modifier.weight(1f), label = "8h") {
                        triggerStart(480, context, viewModel, { showDeviceAdminPromptDialog = true })
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Manual Time Button
                OutlinedButton(
                    onClick = {
                        customHoursInput = "1"
                        customMinutesInput = "0"
                        customTimeError = null
                        showCustomTimeDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryIndigoLight)
                ) {
                    Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manually Set Custom Time", style = Typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    // Custom Time Dialog
    if (showCustomTimeDialog) {
        AlertDialog(
            onDismissRequest = { showCustomTimeDialog = false },
            title = { Text("Set Custom Focus Duration", color = TextPrimary) },
            text = {
                Column {
                    Text(
                        "Enter the duration for strict focus locking:",
                        style = Typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = customHoursInput,
                            onValueChange = { customHoursInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Hours") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = customMinutesInput,
                            onValueChange = { customMinutesInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Minutes") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    customTimeError?.let { err ->
                        Text(err, color = ErrorRose, style = Typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val hours = customHoursInput.toIntOrNull() ?: 0
                        val mins = customMinutesInput.toIntOrNull() ?: 0
                        val totalMins = (hours * 60) + mins
                        if (totalMins < 1) {
                            customTimeError = "Duration must be at least 1 minute."
                        } else {
                            showCustomTimeDialog = false
                            triggerStart(totalMins, context, viewModel, {
                                pendingMinutes = totalMins
                                showDeviceAdminPromptDialog = true
                            })
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryIndigo,
                        contentColor = DarkBackground
                    )
                ) {
                    Text("Start Focus", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomTimeDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Device Admin Prompt Dialog
    if (showDeviceAdminPromptDialog) {
        AlertDialog(
            onDismissRequest = { showDeviceAdminPromptDialog = false },
            title = { Text("Device Administrator Required", color = TextPrimary) },
            text = {
                Text(
                    "To activate strict Focus Mode, Device Administrator must be enabled. This ensures that the application cannot be deleted or uninstalled while your focus session is active.\n\nTap 'Enable Admin' to authorize in system settings.",
                    style = Typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeviceAdminPromptDialog = false
                        val adminComponent = ComponentName(context, GuardianDeviceAdminReceiver::class.java)
                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                            putExtra(
                                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                "EliteGuardian requires Device Administrator to prevent uninstallation during active Focus Mode."
                            )
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryIndigo,
                        contentColor = DarkBackground
                    )
                ) {
                    Text("Enable Admin", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeviceAdminPromptDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

private fun triggerStart(
    durationMinutes: Int,
    context: Context,
    viewModel: FocusViewModel,
    onPromptAdmin: () -> Unit
) {
    viewModel.startFocusSession(
        context = context,
        durationMinutes = durationMinutes,
        onRequireDeviceAdmin = onPromptAdmin,
        onStarted = {
            Toast.makeText(context, "Focus Mode locked for $durationMinutes minutes!", Toast.LENGTH_SHORT).show()
        }
    )
}

@Composable
private fun PresetButton(
    modifier: Modifier = Modifier,
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceVariant)
        )
    ) {
        Text(label, style = Typography.titleMedium)
    }
}
