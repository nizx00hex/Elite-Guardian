package com.eliteguardian.app.ui.keywords

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eliteguardian.app.data.database.KeywordEntity
import com.eliteguardian.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeywordScreen(
    viewModel: KeywordViewModel,
    onNavigateBack: () -> Unit
) {
    val keywords by viewModel.filteredKeywords.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val addError by viewModel.addError.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var newKeywordInput by remember { mutableStateOf("") }

    val context = androidx.compose.ui.platform.LocalContext.current
    var isAccessibilityEnabled by remember {
        mutableStateOf(com.eliteguardian.app.accessibility.AccessibilityUtils.isAccessibilityServiceEnabled(context))
    }

    DisposableEffect(Unit) {
        isAccessibilityEnabled = com.eliteguardian.app.accessibility.AccessibilityUtils.isAccessibilityServiceEnabled(context)
        onDispose { }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Keyword Blocker", style = Typography.titleLarge, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    newKeywordInput = ""
                    viewModel.clearError()
                    showAddDialog = true
                },
                containerColor = PrimaryIndigo,
                contentColor = TextPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Keyword")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            // Live Status Banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isAccessibilityEnabled) ShieldGreen.copy(alpha = 0.12f) else ErrorRose.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isAccessibilityEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isAccessibilityEnabled) ShieldGreen else ErrorRose,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAccessibilityEnabled) "Keyword Shield Active" else "Keyword Service is OFF",
                            style = Typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isAccessibilityEnabled)
                                "When any restricted word is typed (e.g. 'porn'), the typing app will be immediately closed."
                            else
                                "Tap Enable to turn on EliteGuardian in Accessibility Settings. (On Android 13+, tap 3 dots on App Info -> Allow restricted settings if required).",
                            style = Typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                    if (!isAccessibilityEnabled) {
                        Button(
                            onClick = {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Enable")
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search keywords...", color = TextMuted) },
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

            Spacer(modifier = Modifier.height(16.dp))

            if (keywords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No keywords found. Tap + to add one.", color = TextMuted)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(keywords, key = { it.id }) { item ->
                        KeywordItem(
                            item = item,
                            onDelete = { viewModel.deleteKeyword(item) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Restricted Keyword", color = TextPrimary) },
            text = {
                Column {
                    Text(
                        "Enter the word or phrase to block when typed:",
                        style = Typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newKeywordInput,
                        onValueChange = {
                            newKeywordInput = it
                            viewModel.clearError()
                        },
                        singleLine = true,
                        placeholder = { Text("e.g. porn", color = TextMuted) },
                        isError = addError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    addError?.let { err ->
                        Text(err, color = ErrorRose, style = Typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addKeyword(newKeywordInput) {
                            showAddDialog = false
                        }
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
}

@Composable
private fun KeywordItem(
    item: KeywordEntity,
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
            Text(item.keyword, style = Typography.titleMedium, color = TextPrimary)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = ErrorRose)
            }
        }
    }
}

