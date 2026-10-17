package com.eliteguardian.app.ui.streak

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eliteguardian.app.domain.streak.Milestone
import com.eliteguardian.app.ui.theme.*

@Composable
fun StreakMilestonesSection(
    milestones: List<Milestone>,
    currentStreak: Int
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Milestones & Roadmaps",
                style = Typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Key brain rewiring benchmarks achieved through discipline",
                style = Typography.bodySmall,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(16.dp))

            milestones.forEachIndexed { index, milestone ->
                MilestoneItemRow(
                    milestone = milestone,
                    isLast = index == milestones.size - 1
                )
            }
        }
    }
}

@Composable
private fun MilestoneItemRow(
    milestone: Milestone,
    isLast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            milestone.isAchieved -> ShieldGreen
                            milestone.isCurrentTarget -> PrimaryIndigoLight
                            else -> DarkSurfaceVariant
                        }
                    )
                    .then(
                        if (milestone.isCurrentTarget)
                            Modifier.border(2.dp, PrimaryIndigoLight, CircleShape)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (milestone.isAchieved) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Achieved",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = if (milestone.isCurrentTarget) Color.White else TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(38.dp)
                        .background(
                            if (milestone.isAchieved) ShieldGreen.copy(alpha = 0.5f)
                            else DarkSurfaceVariant
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${milestone.days} Days — ${milestone.title}",
                    style = Typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (milestone.isAchieved || milestone.isCurrentTarget) TextPrimary else TextMuted
                )

                if (milestone.isAchieved) {
                    Text(
                        text = "UNLOCKED",
                        style = Typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ShieldGreen
                    )
                } else if (milestone.isCurrentTarget) {
                    Text(
                        text = "NEXT GOAL",
                        style = Typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryIndigoLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = milestone.description,
                style = Typography.bodySmall,
                color = TextMuted
            )
        }
    }
}

