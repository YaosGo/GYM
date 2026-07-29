package com.gym.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gym.app.data.entity.WorkoutSession
import com.gym.app.data.repository.ExerciseRepository
import com.gym.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class HistoryUiState(
    val totalWorkouts: Int = 0,
    val monthlyWorkouts: Int = 0,
    val currentYear: Int = 2026,
    val currentMonth: Int = 7, // 0-based for Calendar (6 = July)
    val workoutDates: Set<String> = emptySet(),
    val selectedDate: String? = null,
    val selectedDateSessions: List<WorkoutSession> = emptyList(),
    val isLoading: Boolean = true,
)

class HistoryViewModel(
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val cal = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        _uiState.value = _uiState.value.copy(
            currentYear = cal.get(Calendar.YEAR),
            currentMonth = cal.get(Calendar.MONTH),
        )
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val state = _uiState.value
            val yearMonth = "${state.currentYear}-${(state.currentMonth + 1).toString().padStart(2, '0')}"

            val total = workoutRepository.getTotalWorkoutCount()
            val monthly = workoutRepository.getMonthlyWorkoutCount(yearMonth)
            val dates = workoutRepository.getWorkoutDatesByMonth(yearMonth).toSet()

            _uiState.value = state.copy(
                totalWorkouts = total,
                monthlyWorkouts = monthly,
                workoutDates = dates,
                isLoading = false,
            )
        }
    }

    fun previousMonth() {
        val state = _uiState.value
        var month = state.currentMonth - 1
        var year = state.currentYear
        if (month < 0) { month = 11; year-- }
        _uiState.value = state.copy(
            currentYear = year,
            currentMonth = month,
            selectedDate = null,
            selectedDateSessions = emptyList(),
            isLoading = true,
        )
        loadData()
    }

    fun nextMonth() {
        val state = _uiState.value
        var month = state.currentMonth + 1
        var year = state.currentYear
        if (month > 11) { month = 0; year++ }
        _uiState.value = state.copy(
            currentYear = year,
            currentMonth = month,
            selectedDate = null,
            selectedDateSessions = emptyList(),
            isLoading = true,
        )
        loadData()
    }

    fun selectDate(date: String) {
        val state = _uiState.value
        if (state.selectedDate == date) {
            // 取消选择
            _uiState.value = state.copy(selectedDate = null, selectedDateSessions = emptyList())
        } else {
            _uiState.value = state.copy(selectedDate = date)
            viewModelScope.launch {
                val sessions = workoutRepository.getSessionsByDate(date)
                _uiState.value = _uiState.value.copy(selectedDateSessions = sessions)
            }
        }
    }

    class Factory(
        private val workoutRepository: WorkoutRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(workoutRepository) as T
        }
    }
}
