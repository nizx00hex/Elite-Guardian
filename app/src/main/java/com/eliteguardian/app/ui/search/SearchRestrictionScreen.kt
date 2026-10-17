package com.eliteguardian.app.ui.search

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eliteguardian.app.data.database.CustomSearchDomainEntity
import com.eliteguardian.app.domain.search.SupportedSearchEngine
import com.eliteguardian.app.ui.theme.*
import com.eliteguardian.app.vpn.VpnStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchRestrictionScreen(
    viewModel: SearchRestrictionViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isMasterEnabled by viewModel.isMasterEnabled.collectAsState()
    val isFocusRestrictionEnabled by viewModel.isFocusRestrictionEnabled.collectAsState()
    val isSafeSearchEnforced by viewModel.isSafeSearchEnforced.collectAsState()
    val isFocusModeActive by viewModel.isFocusModeActive.collectAsState()
    val disabledEngines by viewModel.disabledEngines.collectAsState()
    val customDomains by viewModel.customDomains.collectAsState()
    val activeBlockedDomains by viewModel.activeBlockedDomains.collectAsState()
    val vpnStatus by viewModel.vpnStatus.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val testResult by viewModel.testResult.collectAsState()

    var showAddDomainDialog by remember { mutableStateOf(false) }
    var showTestDialog by remember { mutableStateOf(false) }
    var showLimitationsDialog by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    var newDomainInput by remember { mutableStateOf("") }
    var newLabelInput by remember { mutableStateOf("") }
    var testDomainInput by remember { mutableStateOf("") }

    val isVpnConnected = vpnStatus is VpnStatus.Connected
    val isCurrentlyRestricting = (isMasterEnabled || (isFocusModeActive && isFocusRestrictionEnabled)) && isVpnConnected

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Search Restrictions",
                        style = Typography.titleLarge,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showLimitationsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info & Limitations",
                            tint = TextSecondary
                        )
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // VPN Status Banner if protection is paused
            if (!isVpnConnected) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "VPN Protection is paused. Enable Protection on Dashboard for domain-level restrictions to apply.",
                            style = Typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Master Hero Card: "Disable Search Access"
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Disable Search Access",
                                style = Typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isCurrentlyRestricting)
                                    "Active: ${activeBlockedDomains.size} search domains currently sinkholed"
                                else if (!isVpnConnected && (isMasterEnabled || isFocusModeActive))
                                    "Configured: Waiting for VPN Protection to be active"
                                else
                                    "Inactive: Search engine domains are currently reachable",
                                style = Typography.bodyMedium,
                                color = if (isCurrentlyRestricting) ShieldGreen else TextMuted
                            )
                        }

                        Switch(
                            checked = isMasterEnabled,
                            onCheckedChange = { viewModel.toggleMasterSwitch(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkBackground,
                                checkedTrackColor = GlowWhite,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = DarkSurfaceVariant
                            )
                        )
                    }

                    if (isFocusModeActive && isFocusRestrictionEnabled && !isMasterEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PrimaryIndigo.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.HourglassTop, contentDescription = null, tint = PrimaryIndigoLight, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Active via Focus Mode session override",
                                    style = Typography.labelSmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Operational Modes Configuration
            Text(
                text = "Restriction Modes",
                style = Typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // SafeSearch Mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SafeSearch VIP Enforcement", style = Typography.titleSmall, color = TextPrimary)
                            Text(
                                "Redirects Google, Bing, DuckDuckGo & YouTube queries to strict safe search endpoints.",
                                style = Typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = isSafeSearchEnforced,
                            onCheckedChange = { viewModel.toggleSafeSearchEnforcement(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkBackground,
                                checkedTrackColor = GlowWhite,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = DarkSurfaceVariant
                            )
                        )
                    }

                    HorizontalDivider(color = DarkSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))

                    // Focus Mode Sync
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Focus Mode Search Lockdown", style = Typography.titleSmall, color = TextPrimary)
                            Text(
                                "Automatically enforce all search restrictions whenever Focus Mode is activated.",
                                style = Typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = isFocusRestrictionEnabled,
                            onCheckedChange = { viewModel.toggleFocusRestriction(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkBackground,
                                checkedTrackColor = GlowWhite,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = DarkSurfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Supported Search Engines List
            Text(
                text = "Supported Search Engines",
                style = Typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SupportedSearchEngine.entries.forEachIndexed { index, engine ->
                        val isBlocked = disabledEngines.contains(engine.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = engine.displayName,
                                    style = Typography.titleSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = engine.description,
                                    style = Typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                            Switch(
                                checked = isBlocked,
                                onCheckedChange = { block ->
                                    viewModel.toggleEngine(engine, block)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ErrorRose,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = DarkSurfaceVariant
                                )
                            )
                        }
                        if (index < SupportedSearchEngine.entries.size - 1) {
                            HorizontalDivider(color = DarkSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Custom Blocked Search Domains
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Custom Search Domains (${customDomains.size})",
                    style = Typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Button(
                    onClick = { showAddDomainDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color.White,
                        contentColor = androidx.compose.ui.graphics.Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = androidx.compose.ui.graphics.Color.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Domain", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (customDomains.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No custom search domains added.",
                                style = Typography.bodySmall,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { showAddDomainDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = androidx.compose.ui.graphics.Color.White,
                                    contentColor = androidx.compose.ui.graphics.Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = androidx.compose.ui.graphics.Color.Black
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Search Domain", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Column {
                        customDomains.forEachIndexed { index, entity ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = entity.domain, style = Typography.bodyMedium, color = TextPrimary)
                                    entity.label?.let {
                                        Text(text = it, style = Typography.labelSmall, color = TextMuted)
                                    }
                                }
                                Switch(
                                    checked = entity.isEnabled,
                                    onCheckedChange = { viewModel.toggleCustomDomain(entity, it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DarkBackground,
                                        checkedTrackColor = GlowWhite,
                                        uncheckedThumbColor = TextSecondary,
                                        uncheckedTrackColor = DarkSurfaceVariant
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { viewModel.removeCustomDomain(entity) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted)
                                }
                            }
                            if (index < customDomains.size - 1) {
                                HorizontalDivider(color = DarkSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Diagnostic & Tools
            Text(
                text = "Verification & Management",
                style = Typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTestDialog = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = SecondaryCyan, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Test Search Domain Status", style = Typography.titleSmall, color = TextPrimary)
                            Text("Verify whether a given domain is currently blocked or allowed", style = Typography.bodySmall, color = TextMuted)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                    }

                    HorizontalDivider(color = DarkSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRestoreConfirmDialog = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Restore Default Configuration", style = Typography.titleSmall, color = TextPrimary)
                            Text("Reset all search restriction switches to default settings", style = Typography.bodySmall, color = TextMuted)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Add Custom Domain Dialog
    if (showAddDomainDialog) {
        AlertDialog(
            onDismissRequest = { showAddDomainDialog = false },
            containerColor = DarkSurface,
            title = { Text("Add Custom Search Domain", color = TextPrimary) },
            text = {
                Column {
                    Text("Enter search engine domain to sinkhole (exact domain or parent domain):", style = Typography.bodySmall, color = TextMuted)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newDomainInput,
                        onValueChange = { newDomainInput = it },
                        label = { Text("Domain (e.g. search.brave.com)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newLabelInput,
                        onValueChange = { newLabelInput = it },
                        label = { Text("Label / Notes (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDomainInput.isNotBlank()) {
                            viewModel.addCustomDomain(newDomainInput, newLabelInput.ifBlank { null }) {
                                newDomainInput = ""
                                newLabelInput = ""
                                showAddDomainDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color.White,
                        contentColor = androidx.compose.ui.graphics.Color.Black
                    )
                ) {
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDomainDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Test Domain Status Dialog
    if (showTestDialog) {
        AlertDialog(
            onDismissRequest = {
                showTestDialog = false
                viewModel.dismissTestResult()
            },
            containerColor = DarkSurface,
            title = { Text("Test Search Domain", color = TextPrimary) },
            text = {
                Column {
                    Text("Enter a domain to check against active EliteGuardian rules:", style = Typography.bodySmall, color = TextMuted)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = testDomainInput,
                        onValueChange = { testDomainInput = it },
                        label = { Text("Domain (e.g. google.com or bing.com)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                    testResult?.let { res ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = res,
                            style = Typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (res.startsWith("BLOCKED")) ShieldGreen else if (res.startsWith("ALLOWED")) WarningAmber else ErrorRose
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (testDomainInput.isNotBlank()) {
                            viewModel.testDomainStatus(testDomainInput)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color.White,
                        contentColor = androidx.compose.ui.graphics.Color.Black
                    )
                ) {
                    Text("Check Rule", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTestDialog = false
                    viewModel.dismissTestResult()
                }) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }

    // Info & Limitations Dialog
    if (showLimitationsDialog) {
        AlertDialog(
            onDismissRequest = { showLimitationsDialog = false },
            containerColor = DarkSurface,
            title = { Text("Search Restriction Scope & Limitations", color = TextPrimary) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "1. Domain-Level Blocking (DNS Sinkhole):\n" +
                                "• EliteGuardian redirects DNS queries for configured search engines (such as google.com or bing.com) to 0.0.0.0, preventing web and app search traffic on that domain.\n" +
                                "• Note: Blocking a search domain (e.g. google.com) may also affect non-search services under that domain.\n\n" +
                                "2. SafeSearch Enforcement:\n" +
                                "• Uses official VIP addresses (e.g. forcesafesearch.google.com) to enforce strict adult content filtering without breaking the whole search engine.\n\n" +
                                "3. Home-Screen Search Widgets:\n" +
                                "• On standard non-root Android, third-party apps cannot unilaterally remove launcher search widgets. To remove the Google Search widget from your home screen, long-press the widget or use an alternative launcher such as Nova or Lawnchair.\n\n" +
                                "4. Application Blocking:\n" +
                                "• To completely prevent opening dedicated search apps (such as the Google App), use EliteGuardian's 'App Blocker' feature.",
                        style = Typography.bodySmall,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showLimitationsDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color.White,
                        contentColor = androidx.compose.ui.graphics.Color.Black
                    )
                ) {
                    Text("Got It", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Restore Defaults Confirmation Dialog
    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            containerColor = DarkSurface,
            title = { Text("Restore Default Settings?", color = TextPrimary) },
            text = {
                Text(
                    "This will unblock all search engines and clear custom search domains. Your adult-content blocklist and app restrictions will NOT be affected.",
                    style = Typography.bodySmall,
                    color = TextMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreDefaults()
                        showRestoreConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRose)
                ) {
                    Text("Restore Defaults")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

