package com.eliteguardian.app.domain.streak

import com.eliteguardian.app.data.database.StreakRecordEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object StreakCalculator {

    private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE

    val MILESTONE_DEFINITIONS = listOf(
        3 to Pair("First Step", "3 days of mental clarity and initial reset"),
        7 to Pair("One Week Strong", "7 days! Dopamine receptors begin recalibrating"),
        14 to Pair("Two Weeks Clean", "14 days of sustained discipline and control"),
        30 to Pair("One Month Reset", "30 days of rewiring pathways and building resilience"),
        60 to Pair("Two Months Mastered", "60 days! Significant surge in baseline energy and focus"),
        90 to Pair("The 90-Day Reboot", "90 days complete reboot! Freedom and self-mastery"),
        180 to Pair("Half Year Hero", "180 days of unshakable self-command"),
        365 to Pair("One Full Year", "365 days of profound transformation")
    )

    /**
     * Calculates the current active streak ending at [referenceDate] (or yesterday if today is not checked in yet).
     * If yesterday was completed, the streak remains intact even if today is not checked in yet.
     */
    fun calculateCurrentStreak(
        records: List<StreakRecordEntity>,
        referenceDate: LocalDate = LocalDate.now()
    ): Int {
        if (records.isEmpty()) return 0

        val recordMap = records.associateBy { it.localDate }
        val todayStr = referenceDate.format(DATE_FORMATTER)
        val todayRecord = recordMap[todayStr]

        // If today has a lapse, streak is 0
        if (todayRecord?.status == StreakStatus.LAPSE.name) {
            return 0
        }

        var streak = 0
        var checkDate = if (todayRecord?.status == StreakStatus.COMPLETED.name) {
            referenceDate
        } else {
            // Today is not yet checked in, check if yesterday was completed
            referenceDate.minusDays(1)
        }

        while (true) {
            val dateStr = checkDate.format(DATE_FORMATTER)
            val record = recordMap[dateStr]
            if (record != null && record.status == StreakStatus.COMPLETED.name) {
                streak++
                checkDate = checkDate.minusDays(1)
            } else {
                // Streak breaks
                break
            }
        }

        return streak
    }

    /**
     * Determines the start date string (YYYY-MM-DD) of the current active streak.
     * Returns null if current streak is 0.
     */
    fun calculateCurrentStreakStartDate(
        records: List<StreakRecordEntity>,
        referenceDate: LocalDate = LocalDate.now()
    ): String? {
        val currentStreak = calculateCurrentStreak(records, referenceDate)
        if (currentStreak == 0) return null

        val recordMap = records.associateBy { it.localDate }
        val todayStr = referenceDate.format(DATE_FORMATTER)
        val todayRecord = recordMap[todayStr]

        val endDate = if (todayRecord?.status == StreakStatus.COMPLETED.name) {
            referenceDate
        } else {
            referenceDate.minusDays(1)
        }

        // Start date is endDate minus (currentStreak - 1) days
        val startDate = endDate.minusDays((currentStreak - 1).toLong())
        return startDate.format(DATE_FORMATTER)
    }

    /**
     * Calculates the longest consecutive streak of COMPLETED days across all records.
     */
    fun calculateLongestStreak(records: List<StreakRecordEntity>): Int {
        if (records.isEmpty()) return 0

        val completedDates = records
            .filter { it.status == StreakStatus.COMPLETED.name }
            .map { LocalDate.parse(it.localDate, DATE_FORMATTER) }
            .distinct()
            .sorted()

        if (completedDates.isEmpty()) return 0

        var maxStreak = 1
        var currentStreak = 1

        for (i in 1 until completedDates.size) {
            val prev = completedDates[i - 1]
            val curr = completedDates[i]

            if (curr == prev.plusDays(1)) {
                currentStreak++
                if (currentStreak > maxStreak) {
                    maxStreak = currentStreak
                }
            } else {
                currentStreak = 1
            }
        }

        return maxStreak
    }

    /**
     * Evaluates all milestones based on current and longest streak.
     */
    fun evaluateMilestones(
        currentStreak: Int,
        longestStreak: Int
    ): List<Milestone> {
        val maxAchieved = maxOf(currentStreak, longestStreak)
        var firstUnachievedFound = false

        return MILESTONE_DEFINITIONS.map { (days, info) ->
            val isAchieved = maxAchieved >= days
            val isCurrentTarget = !isAchieved && !firstUnachievedFound
            if (isCurrentTarget) {
                firstUnachievedFound = true
            }

            Milestone(
                days = days,
                title = info.first,
                description = info.second,
                isAchieved = isAchieved,
                isCurrentTarget = isCurrentTarget
            )
        }
    }

    /**
     * Generates a complete StreakSummary snapshot.
     */
    fun generateSummary(
        records: List<StreakRecordEntity>,
        referenceDate: LocalDate = LocalDate.now()
    ): StreakSummary {
        val currentStreak = calculateCurrentStreak(records, referenceDate)
        val longestStreak = calculateLongestStreak(records)
        val totalCompleted = records.count { it.status == StreakStatus.COMPLETED.name }
        val totalLapses = records.count { it.status == StreakStatus.LAPSE.name }
        val startDate = calculateCurrentStreakStartDate(records, referenceDate)

        val milestones = evaluateMilestones(currentStreak, longestStreak)
        val nextMilestone = milestones.firstOrNull { !it.isAchieved } ?: milestones.last()
        val unlockedCount = milestones.count { it.isAchieved }

        val previousMilestoneDays = milestones
            .lastOrNull { it.isAchieved }?.days ?: 0

        val progressRatio = if (currentStreak >= nextMilestone.days) {
            1.0f
        } else {
            val range = (nextMilestone.days - previousMilestoneDays).coerceAtLeast(1)
            val currentInRange = (currentStreak - previousMilestoneDays).coerceAtLeast(0)
            (currentInRange.toFloat() / range.toFloat()).coerceIn(0.0f, 1.0f)
        }

        val todayStr = referenceDate.format(DATE_FORMATTER)
        val todayRecord = records.find { it.localDate == todayStr }
        val todayStatus = when (todayRecord?.status) {
            StreakStatus.COMPLETED.name -> StreakStatus.COMPLETED
            StreakStatus.LAPSE.name -> StreakStatus.LAPSE
            else -> null
        }

        return StreakSummary(
            currentStreak = currentStreak,
            longestStreak = maxOf(longestStreak, currentStreak),
            totalCompleted = totalCompleted,
            totalLapses = totalLapses,
            currentStreakStartDate = startDate,
            nextMilestoneDays = nextMilestone.days,
            nextMilestoneTitle = nextMilestone.title,
            milestoneProgress = progressRatio,
            todayStatus = todayStatus,
            isCheckedInToday = todayStatus != null,
            unlockedMilestonesCount = unlockedCount,
            totalMilestonesCount = milestones.size
        )
    }
}

