package com.eliteguardian.app.domain.streak

import java.time.LocalDate

enum class StreakStatus {
    COMPLETED,
    LAPSE
}

data class Milestone(
    val days: Int,
    val title: String,
    val description: String,
    val isAchieved: Boolean,
    val isCurrentTarget: Boolean
)

data class StreakSummary(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalCompleted: Int,
    val totalLapses: Int,
    val currentStreakStartDate: String?,
    val nextMilestoneDays: Int,
    val nextMilestoneTitle: String,
    val milestoneProgress: Float, // 0.0f to 1.0f
    val todayStatus: StreakStatus?,
    val isCheckedInToday: Boolean,
    val unlockedMilestonesCount: Int,
    val totalMilestonesCount: Int
)

data class DayDetail(
    val date: LocalDate,
    val dateString: String, // YYYY-MM-DD
    val status: StreakStatus?,
    val note: String?,
    val isToday: Boolean,
    val isFuture: Boolean
)

