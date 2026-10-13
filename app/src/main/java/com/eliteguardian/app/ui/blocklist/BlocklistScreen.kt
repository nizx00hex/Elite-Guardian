package com.eliteguardian.app.ui.blocklist

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.eliteguardian.app.data.database.BlocklistEntity
import com.eliteguardian.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlocklistScreen(
    viewModel: BlocklistViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val blocklistEntries by viewModel.blocklistEntries.collectAsState()
    val allowlistEntries by viewModel.allowlistEntries.collectAsState()
    val addDomainError by viewModel.addDomainError.collectAsState()
    val importResult by viewModel.importResult.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var newDomainInput by remember { mutableStateOf("") }
    var importInputText by remember { mutableStateOf("") }

    importResult?.let { msg ->
        LaunchedEffect(msg) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearImportResult()
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Filter Lists", style = Typography.titleLarge, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Import", tint = TextSecondary)
                    }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            val exported = viewModel.exportCurrentList(selectedTab == 1)
                            clipboardManager.setText(AnnotatedString(exported))
                            Toast.makeText(context, "Exported list copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Export", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    newDomainInput = ""
                    viewModel.clearAddDomainError()
                    showAddDialog = true
                },
                containerColor = PrimaryIndigo,
                contentColor = TextPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Domain")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            // Seed Engine Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = ShieldGreen)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Seed Adult Engine", style = Typography.titleMedium, color = TextPrimary)
                        Text(
                            "${viewModel.getSeedCount()} domains bundled & active offline",
                            style = Typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }

            // Tabs: Custom Blocklist vs Allowlist
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = PrimaryIndigo,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectedTab.value = 0 },
                    text = { Text("Blocked (${blocklistEntries.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectedTab.value = 1 },
                    text = { Text("Allowed (${allowlistEntries.size})") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search domains...", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // List of items
            val activeList = if (selectedTab == 0) blocklistEntries else allowlistEntries
            if (activeList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No matching domains found."
                        else if (selectedTab == 0) "No custom blocked domains.\nTap + to add one."
                        else "No custom allowed domains.\nTap + to add one.",
                        style = Typography.bodyLarge,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeList, key = { it.id }) { entry ->
                        DomainListItem(entry = entry, onDelete = { viewModel.deleteEntry(entry) })
                    }
                }
            }
        }
    }

    // Add Domain Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    if (selectedTab == 0) "Add to Blocklist" else "Add to Allowlist",
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        "Enter the domain name (e.g., example.com):",
                        style = Typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newDomainInput,
                        onValueChange = {
                            newDomainInput = it
                            viewModel.clearAddDomainError()
                        },
                        singleLine = true,
                        placeholder = { Text("example.com", color = TextMuted) },
                        isError = addDomainError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    addDomainError?.let { err ->
                        Text(err, color = ErrorRose, style = Typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addDomain(
                            domain = newDomainInput,
                            isAllowlist = selectedTab == 1,
                            onComplete = { showAddDialog = false }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Domain List", color = TextPrimary) },
            text = {
                Column {
                    Text(
                        "Paste domains or hosts file content below (one per line):",
                        style = Typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importInputText,
                        onValueChange = { importInputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        placeholder = { Text("badsite.com\n0.0.0.0 anothersite.org", color = TextMuted) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importList(importInputText, isAllowlist = selectedTab == 1)
                        showImportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
private fun DomainListItem(
    entry: BlocklistEntity,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.domain, style = Typography.titleMedium, color = TextPrimary)
                entry.notes?.let { note ->
                    Text(note, style = Typography.bodyMedium, color = TextMuted)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = ErrorRose)
            }
        }
    }
}

