package com.eliteguardian.app.ui.dashboard

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eliteguardian.app.ui.theme.*
import com.eliteguardian.app.vpn.VpnStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToBlocklist: () -> Unit,
    onNavigateToAppBlocker: () -> Unit,
    onNavigateToKeywordBlocker: () -> Unit,
    onNavigateToFocus: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val vpnStatus by viewModel.vpnStatus.collectAsState()
    val isProtected = vpnStatus is VpnStatus.Connected
    val totalBlocked by viewModel.totalBlockedCount.collectAsState(initial = 0L)
    val isSafeSearch by viewModel.isSafeSearchEnabled.collectAsState(initial = true)
    val isFocusMode by viewModel.isFocusModeActive.collectAsState(initial = false)
    val lastInitTime by viewModel.lastFilterInitTime.collectAsState(initial = 0L)
    val diagnosticResult by viewModel.diagnosticResult.collectAsState()

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.toggleProtection(context)
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.eliteguardian.app.R.drawable.app_logo),
                            contentDescription = "EliteGuardian Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "EliteGuardian",
                            style = Typography.titleLarge,
                            color = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Diagnostic Alert Banner
            diagnosticResult?.let { result ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.startsWith("PASS")) DarkSurfaceVariant else ErrorRose.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (result.startsWith("PASS")) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (result.startsWith("PASS")) ShieldGreen else ErrorRose
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = result,
                            style = Typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.dismissDiagnostic() }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextSecondary)
                        }
                    }
                }
            }

            // Hero Protection Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(
                                if (isProtected) ShieldGreen.copy(alpha = 0.2f)
                                else DarkSurfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.eliteguardian.app.R.drawable.app_logo),
                            contentDescription = "EliteGuardian Logo",
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isProtected) "Protection Active" else "Protection Paused",
                        style = Typography.headlineMedium,
                        color = if (isProtected) ShieldGreen else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isProtected)
                            "Adult content & malicious domains blocked system-wide."
                        else
                            "Tap below to activate secure DNS filtering.",
                        style = Typography.bodyMedium,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val prepareIntent = VpnService.prepare(context)
                            if (prepareIntent != null) {
                                vpnLauncher.launch(prepareIntent)
                            } else {
                                viewModel.toggleProtection(context)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isProtected) ErrorRose else PrimaryIndigo
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = if (isProtected) "Disable Protection" else "Enable Protection",
                            style = Typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Blocked Requests",
                    value = totalBlocked.toString(),
                    icon = Icons.Default.Block,
                    iconTint = ErrorRose
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "SafeSearch",
                    value = if (isSafeSearch) "Active" else "Off",
                    icon = Icons.Default.Search,
                    iconTint = PrimaryIndigoLight
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Focus Mode",
                    value = if (isFocusMode) "Active" else "Inactive",
                    icon = Icons.Default.Timer,
                    iconTint = WarningAmber
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Last Filter Init",
                    value = if (lastInitTime > 0)
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastInitTime))
                    else "None",
                    icon = Icons.Default.Refresh,
                    iconTint = SecondaryCyan
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Controls
            Text(
                text = "Controls & Blockers",
                style = Typography.titleMedium,
                color = TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            ActionItemRow(
                title = "Domain Blocklist & Allowlist",
                subtitle = "Manage custom websites and permitted domains",
                icon = Icons.Default.Language,
                onClick = onNavigateToBlocklist
            )

            Spacer(modifier = Modifier.height(10.dp))

            ActionItemRow(
                title = "App Blocker",
                subtitle = "Block installed apps from opening",
                icon = Icons.Default.Apps,
                onClick = onNavigateToAppBlocker
            )

            Spacer(modifier = Modifier.height(10.dp))

            ActionItemRow(
                title = "Keyword Filter Shield",
                subtitle = "Closes typing apps when words like 'porn' are entered",
                icon = Icons.Default.Spellcheck,
                onClick = onNavigateToKeywordBlocker
            )

            Spacer(modifier = Modifier.height(10.dp))

            ActionItemRow(
                title = "Focus Mode & Schedule",
                subtitle = "Strict locked focus with Device Administrator required",
                icon = Icons.Default.HourglassTop,
                onClick = onNavigateToFocus
            )

            Spacer(modifier = Modifier.height(10.dp))

            ActionItemRow(
                title = "Run Filter Self-Test",
                subtitle = "Verify local DNS sinkhole with test domain",
                icon = Icons.Default.BugReport,
                onClick = { viewModel.runDiagnosticTest() }
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = title, style = Typography.bodyMedium, color = TextSecondary)
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = value, style = Typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ActionItemRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = PrimaryIndigoLight, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = Typography.titleMedium, color = TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = Typography.bodyMedium, color = TextMuted)
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
        }
    }
}
