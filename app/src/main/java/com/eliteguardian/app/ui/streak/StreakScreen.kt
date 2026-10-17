package com.eliteguardian.app.ui.streak

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eliteguardian.app.domain.streak.StreakStatus
import com.eliteguardian.app.ui.theme.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakScreen(
    viewModel: StreakViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val summary by viewModel.summary.collectAsState()
    val allRecords by viewModel.allRecords.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val selectedRecord by viewModel.selectedDateRecord.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val milestones by viewModel.milestones.collectAsState()

    val reminderEnabled by viewModel.reminderEnabled.collectAsState()
    val reminderHour by viewModel.reminderHour.collectAsState()
    val reminderMinute by viewModel.reminderMinute.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedCalendarDate by remember { mutableStateOf<LocalDate?>(null) }
    var showLapseConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.dismissUserMessage()
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Streak Tracker",
                        style = Typography.titleLarge,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Tracker Settings",
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

            // Main Hero Streak Card
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
                    // Badge Circle
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(PrimaryIndigo.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${summary.currentStreak}",
                                style = Typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryIndigoLight
                            )
                            Text(
                                text = if (summary.currentStreak == 1) "DAY" else "DAYS",
                                style = Typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (summary.currentStreak > 0) "Current Clean Streak" else "Start Your Journey",
                        style = Typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    summary.currentStreakStartDate?.let { start ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Started: $start",
                            style = Typography.bodySmall,
                            color = TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Milestone Progress Bar
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Next: ${summary.nextMilestoneTitle}",
                                style = Typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            Text(
                                text = "${summary.currentStreak} / ${summary.nextMilestoneDays} days",
                                style = Typography.bodySmall,
                                color = PrimaryIndigoLight
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { summary.milestoneProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = ShieldGreen,
                            trackColor = DarkSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Daily Check-in / Lapse Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.checkInToday() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (summary.todayStatus == StreakStatus.COMPLETED)
                                    ShieldGreen.copy(alpha = 0.8f)
                                else ShieldGreen
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Icon(
                                imageVector = if (summary.todayStatus == StreakStatus.COMPLETED)
                                    Icons.Default.CheckCircle
                                else Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (summary.todayStatus == StreakStatus.COMPLETED) "Checked In" else "I Completed Today",
                                style = Typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { showLapseConfirmDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRose),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Record Lapse", style = Typography.bodyMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Statistics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Longest Streak",
                    value = "${summary.longestStreak} days",
                    icon = Icons.Default.EmojiEvents,
                    iconTint = WarningAmber
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Clean Days",
                    value = "${summary.totalCompleted}",
                    icon = Icons.Default.CheckCircle,
                    iconTint = ShieldGreen
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Milestones Unlocked",
                    value = "${summary.unlockedMilestonesCount} / ${summary.totalMilestonesCount}",
                    icon = Icons.Default.Star,
                    iconTint = PrimaryIndigoLight
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Lapses",
                    value = "${summary.totalLapses}",
                    icon = Icons.Default.History,
                    iconTint = if (summary.totalLapses > 0) ErrorRose else TextMuted
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Calendar Section
            Text(
                text = "History & Calendar",
                style = Typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            StreakCalendarSection(
                currentMonth = currentMonth,
                records = allRecords,
                onPreviousMonth = { viewModel.previousMonth() },
                onNextMonth = { viewModel.nextMonth() },
                onDateClick = { date ->
                    selectedCalendarDate = date
                    viewModel.loadDateRecord(date)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Milestones Roadmap Section
            StreakMilestonesSection(
                milestones = milestones,
                currentStreak = summary.currentStreak
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Selected Date Details Bottom Sheet
    selectedCalendarDate?.let { date ->
        val dateStr = date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        val record = allRecords.find { it.localDate == dateStr } ?: selectedRecord
        DayDetailBottomSheet(
            date = date,
            record = record,
            onSaveStatus = { status, note ->
                viewModel.recordDateStatus(date, status, note)
            },
            onDelete = {
                viewModel.removeDateRecord(date)
            },
            onDismiss = {
                selectedCalendarDate = null
                viewModel.clearSelectedDateRecord()
            }
        )
    }

    // Lapse Confirmation Dialog
    if (showLapseConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLapseConfirmDialog = false },
            containerColor = DarkSurface,
            title = { Text("Record a Lapse?", color = TextPrimary) },
            text = {
                Text(
                    "Recovery is a journey of continuous resilience. Recording a lapse resets your current streak to 0, but your total clean days and longest record remain protected.",
                    color = TextMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.recordLapseToday()
                        showLapseConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRose)
                ) {
                    Text("Confirm Lapse & Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLapseConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Settings & Data Management Dialog
    if (showSettingsDialog) {
        StreakSettingsDialog(
            reminderEnabled = reminderEnabled,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute,
            onSaveReminder = { enabled, h, m ->
                viewModel.updateReminder(context, enabled, h, m)
            },
            onExportData = { viewModel.exportJsonData() },
            onImportData = { json ->
                viewModel.importJsonData(json) { success, count ->
                    if (success) {
                        android.widget.Toast.makeText(context, "Imported $count streak records", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(context, "Failed to import records", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onResetStreakData = { viewModel.resetStreakTrackerOnly() },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = Typography.bodyMedium, color = TextSecondary)
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = Typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

