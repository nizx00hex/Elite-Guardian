package com.eliteguardian.app.ui.streak

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteguardian.app.EliteGuardianApp
import com.eliteguardian.app.data.database.StreakRecordEntity
import com.eliteguardian.app.data.repository.StreakRepository
import com.eliteguardian.app.domain.streak.Milestone
import com.eliteguardian.app.domain.streak.StreakCalculator
import com.eliteguardian.app.domain.streak.StreakStatus
import com.eliteguardian.app.domain.streak.StreakSummary
import com.eliteguardian.app.notifications.streak.StreakReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class StreakViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StreakRepository.getInstance(application)
    private val appPreferences = (application as EliteGuardianApp).appPreferences

    val allRecords: StateFlow<List<StreakRecordEntity>> = repository.getAllRecordsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summary: StateFlow<StreakSummary> = repository.getStreakSummaryFlow()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            StreakCalculator.generateSummary(emptyList(), LocalDate.now())
        )

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    private val _selectedDateRecord = MutableStateFlow<StreakRecordEntity?>(null)
    val selectedDateRecord: StateFlow<StreakRecordEntity?> = _selectedDateRecord.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    val reminderEnabled: StateFlow<Boolean> = appPreferences.streakReminderEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val reminderHour: StateFlow<Int> = appPreferences.streakReminderHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 21)

    val reminderMinute: StateFlow<Int> = appPreferences.streakReminderMinute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val milestones: StateFlow<List<Milestone>> = allRecords.combine(summary) { records, summ ->
        StreakCalculator.evaluateMilestones(summ.currentStreak, summ.longestStreak)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun checkInToday(note: String? = null) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        viewModelScope.launch {
            repository.recordCheckIn(today, note)
            _userMessage.value = "Great job! Day completed."
        }
    }

    fun recordLapseToday(note: String? = null) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        viewModelScope.launch {
            repository.recordLapse(today, note)
            _userMessage.value = "Lapse recorded. Resilience begins today."
        }
    }

    fun recordDateStatus(date: LocalDate, status: StreakStatus, note: String? = null) {
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        viewModelScope.launch {
            if (status == StreakStatus.COMPLETED) {
                repository.recordCheckIn(dateStr, note)
            } else {
                repository.recordLapse(dateStr, note)
            }
            loadDateRecord(date)
            _userMessage.value = "Updated $dateStr"
        }
    }

    fun removeDateRecord(date: LocalDate) {
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        viewModelScope.launch {
            repository.deleteRecord(dateStr)
            loadDateRecord(date)
            _userMessage.value = "Cleared entry for $dateStr"
        }
    }

    fun updateNoteForDate(date: LocalDate, note: String?) {
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        viewModelScope.launch {
            repository.updateNote(dateStr, note)
            loadDateRecord(date)
            _userMessage.value = "Note saved."
        }
    }

    fun loadDateRecord(date: LocalDate) {
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        viewModelScope.launch {
            _selectedDateRecord.value = repository.getRecordForDate(dateStr)
        }
    }

    fun clearSelectedDateRecord() {
        _selectedDateRecord.value = null
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun updateReminder(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            appPreferences.setStreakReminder(enabled, hour, minute)
            if (enabled) {
                StreakReminderScheduler.scheduleDailyReminder(context, hour, minute)
                _userMessage.value = "Daily reminder set for %02d:%02d".format(hour, minute)
            } else {
                StreakReminderScheduler.cancelReminder(context)
                _userMessage.value = "Reminder turned off"
            }
        }
    }

    fun resetStreakTrackerOnly() {
        viewModelScope.launch {
            repository.resetAllStreakData()
            _userMessage.value = "Streak history reset cleanly."
        }
    }

    suspend fun exportJsonData(): String {
        return repository.exportDataAsJson()
    }

    fun importJsonData(json: String, onComplete: (Boolean, Int) -> Unit) {
        viewModelScope.launch {
            try {
                val count = repository.importDataFromJson(json)
                _userMessage.value = "Imported $count records successfully."
                onComplete(true, count)
            } catch (e: Exception) {
                _userMessage.value = "Failed to import: ${e.message}"
                onComplete(false, 0)
            }
        }
    }

    fun dismissUserMessage() {
        _userMessage.value = null
    }
}

