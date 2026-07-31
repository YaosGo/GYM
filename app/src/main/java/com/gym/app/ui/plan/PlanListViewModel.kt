package com.gym.app.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gym.app.data.entity.WorkoutPlan
import com.gym.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlanListUiState(
    val plans: List<WorkoutPlan> = emptyList(),
    val lastDurations: Map<Long, Int> = emptyMap(), // planId → durationSeconds of last session
)

class PlanListViewModel(
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val _lastDurations = kotlinx.coroutines.flow.MutableStateFlow<Map<Long, Int>>(emptyMap())

    val uiState: StateFlow<PlanListUiState> = combine(
        workoutRepository.getAllPlans(),
        _lastDurations,
    ) { plans, durations ->
        PlanListUiState(plans = plans, lastDurations = durations)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlanListUiState(),
    )

    init {
        loadLastDurations()
    }

    private fun loadLastDurations() {
        viewModelScope.launch {
            // 延迟一帧等 plans 加载完
            kotlinx.coroutines.delay(200)
            val plans = workoutRepository.getAllPlans()
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
                .value
            val durations = mutableMapOf<Long, Int>()
            for (plan in plans) {
                val session = workoutRepository.getLatestSession(plan.planId)
                if (session != null) {
                    durations[plan.planId] = session.durationSeconds
                }
            }
            _lastDurations.value = durations
        }
    }

    fun deletePlan(plan: WorkoutPlan) {
        viewModelScope.launch {
            workoutRepository.deletePlan(plan)
        }
    }

    class Factory(private val repository: WorkoutRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlanListViewModel(repository) as T
        }
    }
}
