package com.eliteguardian.app.domain.streak

import com.eliteguardian.app.data.database.StreakRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {

    private val baseDate = LocalDate.of(2026, 10, 13)

    private fun createRecord(date: String, status: StreakStatus, note: String? = null): StreakRecordEntity {
        return StreakRecordEntity(
            id = 0,
            localDate = date,
            status = status.name,
            note = note
        )
    }

    @Test
    fun testEmptyRecordsReturnsZeroStreak() {
        val streak = StreakCalculator.calculateCurrentStreak(emptyList(), baseDate)
        assertEquals(0, streak)
        val longest = StreakCalculator.calculateLongestStreak(emptyList())
        assertEquals(0, longest)
        val startDate = StreakCalculator.calculateCurrentStreakStartDate(emptyList(), baseDate)
        assertNull(startDate)
    }

    @Test
    fun testSingleDayCompletedToday() {
        val records = listOf(
            createRecord("2026-10-13", StreakStatus.COMPLETED)
        )
        val currentStreak = StreakCalculator.calculateCurrentStreak(records, baseDate)
        assertEquals(1, currentStreak)
        assertEquals("2026-10-13", StreakCalculator.calculateCurrentStreakStartDate(records, baseDate))
        assertEquals(1, StreakCalculator.calculateLongestStreak(records))
    }

    @Test
    fun testYesterdayCompletedTodayNotCheckedInYetKeepsStreak() {
        // Today is 2026-10-13, not yet checked in. Yesterday was 2026-10-12 and completed.
        val records = listOf(
            createRecord("2026-10-11", StreakStatus.COMPLETED),
            createRecord("2026-10-12", StreakStatus.COMPLETED)
        )
        val currentStreak = StreakCalculator.calculateCurrentStreak(records, baseDate)
        assertEquals("Streak must remain active if today hasn't passed/checked in yet", 2, currentStreak)
        assertEquals("2026-10-11", StreakCalculator.calculateCurrentStreakStartDate(records, baseDate))
    }

    @Test
    fun testLapseTodayResetsCurrentStreakToZero() {
        val records = listOf(
            createRecord("2026-10-11", StreakStatus.COMPLETED),
            createRecord("2026-10-12", StreakStatus.COMPLETED),
            createRecord("2026-10-13", StreakStatus.LAPSE)
        )
        val currentStreak = StreakCalculator.calculateCurrentStreak(records, baseDate)
        assertEquals(0, currentStreak)
        assertNull(StreakCalculator.calculateCurrentStreakStartDate(records, baseDate))

        // Longest streak must still preserve the 2-day record
        val longestStreak = StreakCalculator.calculateLongestStreak(records)
        assertEquals(2, longestStreak)
    }

    @Test
    fun testHistoricalLapseAndRestart() {
        val records = listOf(
            createRecord("2026-10-01", StreakStatus.COMPLETED),
            createRecord("2026-10-02", StreakStatus.COMPLETED),
            createRecord("2026-10-03", StreakStatus.COMPLETED),
            createRecord("2026-10-04", StreakStatus.LAPSE),
            createRecord("2026-10-05", StreakStatus.COMPLETED),
            createRecord("2026-10-06", StreakStatus.COMPLETED),
            createRecord("2026-10-07", StreakStatus.COMPLETED),
            createRecord("2026-10-08", StreakStatus.COMPLETED),
            createRecord("2026-10-09", StreakStatus.COMPLETED),
            createRecord("2026-10-10", StreakStatus.COMPLETED),
            createRecord("2026-10-11", StreakStatus.COMPLETED),
            createRecord("2026-10-12", StreakStatus.COMPLETED),
            createRecord("2026-10-13", StreakStatus.COMPLETED)
        )
        val currentStreak = StreakCalculator.calculateCurrentStreak(records, baseDate)
        assertEquals(9, currentStreak) // Oct 5 through Oct 13 = 9 days
        assertEquals("2026-10-05", StreakCalculator.calculateCurrentStreakStartDate(records, baseDate))

        val longestStreak = StreakCalculator.calculateLongestStreak(records)
        assertEquals(9, longestStreak)
    }

    @Test
    fun testMonthBoundaryTransition() {
        val nov1 = LocalDate.of(2026, 11, 1)
        val records = listOf(
            createRecord("2026-10-30", StreakStatus.COMPLETED),
            createRecord("2026-10-31", StreakStatus.COMPLETED),
            createRecord("2026-11-01", StreakStatus.COMPLETED)
        )
        val currentStreak = StreakCalculator.calculateCurrentStreak(records, nov1)
        assertEquals(3, currentStreak)
        assertEquals("2026-10-30", StreakCalculator.calculateCurrentStreakStartDate(records, nov1))
    }

    @Test
    fun testLeapYearBoundaryTransition() {
        // 2028 is a leap year (Feb 29 exists)
        val march1 = LocalDate.of(2028, 3, 1)
        val records = listOf(
            createRecord("2028-02-28", StreakStatus.COMPLETED),
            createRecord("2028-02-29", StreakStatus.COMPLETED),
            createRecord("2028-03-01", StreakStatus.COMPLETED)
        )
        val currentStreak = StreakCalculator.calculateCurrentStreak(records, march1)
        assertEquals(3, currentStreak)
        assertEquals("2028-02-28", StreakCalculator.calculateCurrentStreakStartDate(records, march1))
    }

    @Test
    fun testMissedHistoricalDayBreaksConsecutiveStreak() {
        // Missing Oct 11 breaks current streak from Oct 10
        val records = listOf(
            createRecord("2026-10-10", StreakStatus.COMPLETED),
            // 2026-10-11 unrecorded
            createRecord("2026-10-12", StreakStatus.COMPLETED),
            createRecord("2026-10-13", StreakStatus.COMPLETED)
        )
        val currentStreak = StreakCalculator.calculateCurrentStreak(records, baseDate)
        assertEquals(2, currentStreak) // Oct 12 + Oct 13
        assertEquals("2026-10-12", StreakCalculator.calculateCurrentStreakStartDate(records, baseDate))
    }

    @Test
    fun testMilestoneEvaluation() {
        val milestones = StreakCalculator.evaluateMilestones(currentStreak = 8, longestStreak = 10)
        // 3 days should be achieved
        val m3 = milestones.find { it.days == 3 }
        assertNotNull(m3)
        assertTrue(m3!!.isAchieved)

        // 7 days should be achieved
        val m7 = milestones.find { it.days == 7 }
        assertNotNull(m7)
        assertTrue(m7!!.isAchieved)

        // 14 days should NOT be achieved yet, and should be the current target
        val m14 = milestones.find { it.days == 14 }
        assertNotNull(m14)
        assertFalse(m14!!.isAchieved)
        assertTrue(m14.isCurrentTarget)

        // 30 days should NOT be achieved and NOT current target
        val m30 = milestones.find { it.days == 30 }
        assertNotNull(m30)
        assertFalse(m30!!.isAchieved)
        assertFalse(m30.isCurrentTarget)
    }

    @Test
    fun testSummaryGeneration() {
        val records = listOf(
            createRecord("2026-10-11", StreakStatus.COMPLETED),
            createRecord("2026-10-12", StreakStatus.COMPLETED),
            createRecord("2026-10-13", StreakStatus.COMPLETED, "Felt energetic today")
        )

        val summary = StreakCalculator.generateSummary(records, baseDate)
        assertEquals(3, summary.currentStreak)
        assertEquals(3, summary.longestStreak)
        assertEquals(3, summary.totalCompleted)
        assertEquals(0, summary.totalLapses)
        assertEquals("2026-10-11", summary.currentStreakStartDate)
        assertEquals(StreakStatus.COMPLETED, summary.todayStatus)
        assertTrue(summary.isCheckedInToday)
        assertEquals(1, summary.unlockedMilestonesCount) // 3-day milestone unlocked
        assertEquals(7, summary.nextMilestoneDays) // Next is 7 days
    }
}

