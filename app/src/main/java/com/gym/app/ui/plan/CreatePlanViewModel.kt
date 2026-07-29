package com.gym.app.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gym.app.data.entity.Exercise
import com.gym.app.data.entity.WorkoutPlan
import com.gym.app.data.entity.WorkoutPlanExercise
import com.gym.app.data.repository.ExerciseRepository
import com.gym.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 计划中单个选中动作的 UI 数据。
 */
data class SelectedExercise(
    val exercise: Exercise,
    val targetSets: Int = 3,
    val targetReps: Int = 10,
)

data class CreatePlanUiState(
    val planName: String = "",
    val selectedExercises: List<SelectedExercise> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<Exercise> = emptyList(),
    val isSearching: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
)

class CreatePlanViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val editPlanId: Long? = null, // null = 新建，非 null = 编辑
) : ViewModel() {

    private val _planName = MutableStateFlow("")
    private val _selectedExercises = MutableStateFlow<List<SelectedExercise>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _isSaving = MutableStateFlow(false)
    private val _isSaved = MutableStateFlow(false)

    val uiState: StateFlow<CreatePlanUiState> = combineStates()

    init {
        // 如果是编辑模式，加载已有计划数据
        if (editPlanId != null) {
            viewModelScope.launch {
                val plan = workoutRepository.getPlanById(editPlanId) ?: return@launch
                _planName.value = plan.name
                val planExercises = workoutRepository.getPlanExercisesOnce(editPlanId)
                val selected = planExercises.map { pe ->
                    val exercise = exerciseRepository.getExerciseById(pe.exerciseId)
                    if (exercise != null) {
                        SelectedExercise(exercise, pe.targetSets, pe.targetReps)
                    } else null
                }.filterNotNull()
                _selectedExercises.value = selected
            }
        }
    }

    fun onPlanNameChanged(name: String) {
        _planName.value = name
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun addExercise(exercise: Exercise) {
        val current = _selectedExercises.value.toMutableList()
        // 防止重复添加
        if (current.any { it.exercise.id == exercise.id }) return
        current.add(SelectedExercise(exercise = exercise))
        _selectedExercises.value = current
        _searchQuery.value = ""
    }

    fun removeExercise(exerciseId: String) {
        _selectedExercises.value = _selectedExercises.value.filter { it.exercise.id != exerciseId }
    }

    fun updateExerciseSets(exerciseId: String, sets: Int) {
        _selectedExercises.value = _selectedExercises.value.map {
            if (it.exercise.id == exerciseId) it.copy(targetSets = sets) else it
        }
    }

    fun updateExerciseReps(exerciseId: String, reps: Int) {
        _selectedExercises.value = _selectedExercises.value.map {
            if (it.exercise.id == exerciseId) it.copy(targetReps = reps) else it
        }
    }

    fun moveExercise(fromIndex: Int, toIndex: Int) {
        val list = _selectedExercises.value.toMutableList()
        if (fromIndex < 0 || fromIndex >= list.size || toIndex < 0 || toIndex >= list.size) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _selectedExercises.value = list
    }

    fun savePlan(onSaved: (Long) -> Unit) {
        val name = _planName.value.trim()
        if (name.isBlank() || _selectedExercises.value.isEmpty()) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val plan = WorkoutPlan(
                    planId = editPlanId ?: 0,
                    name = name,
                )
                val exercises = _selectedExercises.value.mapIndexed { index, se ->
                    WorkoutPlanExercise(
                        planId = 0, // 会在 DAO 事务中被替换
                        exerciseId = se.exercise.id,
                        targetSets = se.targetSets,
                        targetReps = se.targetReps,
                        sortOrder = index,
                    )
                }

                val planId = if (editPlanId != null) {
                    // 编辑模式：删除旧关联再插入新的
                    workoutRepository.updatePlan(plan)
                    // TODO: 更好的编辑流程
                    editPlanId
                } else {
                    workoutRepository.createPlanWithExercises(plan, exercises)
                }

                _isSaved.value = true
                onSaved(planId)
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun combineStates(): StateFlow<CreatePlanUiState> {
        // 手动组合状态流
        return _searchQuery.flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                exerciseRepository.fuzzySearchExercises(query)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        ).let { searchFlow ->
            // Combine with other state
            object : StateFlow<CreatePlanUiState> {
                override val value: CreatePlanUiState
                    get() = CreatePlanUiState(
                        planName = _planName.value,
                        selectedExercises = _selectedExercises.value,
                        searchQuery = _searchQuery.value,
                        searchResults = searchFlow.value,
                        isSearching = _searchQuery.value.isNotBlank(),
                        isSaving = _isSaving.value,
                        isSaved = _isSaved.value,
                    )

                override val replayCache: List<CreatePlanUiState>
                    get() = listOf(value)

                override suspend fun collect(collector: kotlinx.coroutines.flow.FlowCollector<CreatePlanUiState>) {
                    kotlinx.coroutines.flow.combine(
                        _planName,
                        _selectedExercises,
                        _searchQuery,
                        searchFlow,
                        _isSaving,
                        _isSaved,
                    ) { name, exercises, query, results, saving, saved ->
                        CreatePlanUiState(
                            planName = name,
                            selectedExercises = exercises,
                            searchQuery = query,
                            searchResults = results,
                            isSearching = query.isNotBlank(),
                            isSaving = saving,
                            isSaved = saved,
                        )
                    }.collect(collector)
                }
            }
        }
    }

    class Factory(
        private val workoutRepository: WorkoutRepository,
        private val exerciseRepository: ExerciseRepository,
        private val editPlanId: Long? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CreatePlanViewModel(workoutRepository, exerciseRepository, editPlanId) as T
        }
    }
}
