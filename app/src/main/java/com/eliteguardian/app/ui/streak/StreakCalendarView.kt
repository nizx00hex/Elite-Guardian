package com.eliteguardian.app.ui.streak

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eliteguardian.app.data.database.StreakRecordEntity
import com.eliteguardian.app.domain.streak.StreakStatus
import com.eliteguardian.app.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StreakCalendarSection(
    currentMonth: YearMonth,
    records: List<StreakRecordEntity>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateClick: (LocalDate) -> Unit
) {
    val recordMap = remember(records) {
        records.associateBy { it.localDate }
    }

    val daysInMonth = remember(currentMonth) {
        val daysCount = currentMonth.lengthOfMonth()
        (1..daysCount).map { currentMonth.atDay(it) }
    }

    val leadingEmptyDays = remember(currentMonth) {
        val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value // 1 (Mon) to 7 (Sun)
        firstDayOfWeek - 1
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Month / Year & Nav
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = TextPrimary)
                }

                Text(
                    text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                    style = Typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                IconButton(onClick = onNextMonth) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Day of week labels (Mon -> Sun)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                    Text(
                        text = day,
                        style = Typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Days Grid
            val today = LocalDate.now()

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(leadingEmptyDays) {
                    Spacer(modifier = Modifier.size(36.dp))
                }

                items(daysInMonth.size) { index ->
                    val date = daysInMonth[index]
                    val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val record = recordMap[dateStr]
                    val isToday = date == today
                    val isFuture = date.isAfter(today)

                    val bgColor = when {
                        record?.status == StreakStatus.COMPLETED.name -> ShieldGreen
                        record?.status == StreakStatus.LAPSE.name -> ErrorRose
                        isToday -> PrimaryIndigoLight.copy(alpha = 0.3f)
                        else -> Color.Transparent
                    }

                    val textColor = when {
                        record != null -> Color.White
                        isFuture -> TextMuted.copy(alpha = 0.4f)
                        isToday -> PrimaryIndigoLight
                        else -> TextPrimary
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(bgColor)
                            .then(
                                if (isToday) Modifier.border(1.5.dp, PrimaryIndigoLight, CircleShape)
                                else Modifier
                            )
                            .clickable(enabled = !isFuture) {
                                onDateClick(date)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = Typography.bodySmall,
                            fontWeight = if (isToday || record != null) FontWeight.Bold else FontWeight.Normal,
                            color = textColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = ShieldGreen, label = "Clean Day")
                LegendItem(color = ErrorRose, label = "Lapse")
                LegendItem(color = DarkSurfaceVariant, label = "Untracked")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = Typography.bodySmall, color = TextMuted)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailBottomSheet(
    date: LocalDate,
    record: StreakRecordEntity?,
    onSaveStatus: (StreakStatus, String?) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStatus by remember(date, record) {
        mutableStateOf(
            when (record?.status) {
                StreakStatus.COMPLETED.name -> StreakStatus.COMPLETED
                StreakStatus.LAPSE.name -> StreakStatus.LAPSE
                else -> StreakStatus.COMPLETED
            }
        )
    }

    var noteText by remember(date, record) {
        mutableStateOf(record?.note ?: "")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
                    style = Typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Status for this date:", style = Typography.bodyMedium, color = TextMuted)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { selectedStatus = StreakStatus.COMPLETED },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedStatus == StreakStatus.COMPLETED) ShieldGreen else DarkSurfaceVariant,
                        contentColor = if (selectedStatus == StreakStatus.COMPLETED) GlowWhite else TextSecondary
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Completed Clean",
                        fontWeight = if (selectedStatus == StreakStatus.COMPLETED) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Button(
                    onClick = { selectedStatus = StreakStatus.LAPSE },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedStatus == StreakStatus.LAPSE) ErrorRose else DarkSurfaceVariant,
                        contentColor = if (selectedStatus == StreakStatus.LAPSE) GlowWhite else TextSecondary
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Lapse",
                        fontWeight = if (selectedStatus == StreakStatus.LAPSE) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Private Note (Optional)", color = TextMuted) },
                placeholder = { Text("Triggers, feelings, or positive reflections...", color = TextMuted) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PrimaryIndigoLight,
                    unfocusedBorderColor = DarkSurfaceVariant
                ),
                maxLines = 4,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (record != null) {
                    OutlinedButton(
                        onClick = {
                            onDelete()
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRose),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear Entry")
                    }
                }

                Button(
                    onClick = {
                        onSaveStatus(selectedStatus, noteText.ifBlank { null })
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryIndigo,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(if (record != null) 1f else 2f)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

