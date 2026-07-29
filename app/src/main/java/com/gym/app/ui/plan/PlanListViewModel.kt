package com.gym.app.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gym.app.data.entity.WorkoutPlan
import com.gym.app.data.entity.WorkoutPlanExercise
import com.gym.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlanListUiState(
    val plans: List<WorkoutPlan> = emptyList(),
    val exerciseCounts: Map<Long, Int> = emptyMap(),
)

class PlanListViewModel(
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    val uiState: StateFlow<PlanListUiState> = workoutRepository.getAllPlans()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlanListUiState(),
        )
        // Note: exerciseCounts can be added later with per-plan query

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
