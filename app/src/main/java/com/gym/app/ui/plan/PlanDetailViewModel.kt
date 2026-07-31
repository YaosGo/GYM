package com.gym.app.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gym.app.data.entity.Exercise
import com.gym.app.data.entity.WorkoutPlanExercise
import com.gym.app.data.repository.ExerciseRepository
import com.gym.app.data.repository.WorkoutRepository
import com.gym.app.ui.components.displayName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlanDetailUiState(
    val planName: String = "",
    val createdDate: Long = 0L,
    val exercises: List<PlanExerciseItem> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<Exercise> = emptyList(),
    val isSearching: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val totalSessions: Int = 0,
    val trendPoints: List<com.gym.app.ui.components.TrendPoint> = emptyList(),
    val totalDurationSec: Int = 0,
)

data class PlanExerciseItem(
    val planExercise: WorkoutPlanExercise,
    val exercise: Exercise?,
) {
    val name: String get() = exercise?.displayName ?: planExercise.exerciseId
    val bodyPart: String get() = exercise?.bodyPart ?: ""
}

class PlanDetailViewModel(
    private val planId: Long,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val _planName = MutableStateFlow("")
    private val _createdDate = MutableStateFlow(0L)
    private val _searchQuery = MutableStateFlow("")
    private val _isSaving = MutableStateFlow(false)
    private val _isSaved = MutableStateFlow(false)
    private val _exercises = MutableStateFlow<List<PlanExerciseItem>>(emptyList())
    private val _totalSessions = MutableStateFlow(0)
    private val _trendPoints = MutableStateFlow<List<com.gym.app.ui.components.TrendPoint>>(emptyList())
    private val _totalDurationSec = MutableStateFlow(0)

    val uiState: StateFlow<PlanDetailUiState> = buildState()

    init {
        viewModelScope.launch {
            val plan = workoutRepository.getPlanById(planId)
            _planName.value = plan?.name ?: ""
            _createdDate.value = plan?.createdDate ?: 0L
            loadExercises()
            loadTrainingData()
        }
    }

    fun deletePlan(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val plan = workoutRepository.getPlanById(planId)
            if (plan != null) {
                workoutRepository.deletePlan(plan)
                onDeleted()
            }
        }
    }

    private suspend fun loadTrainingData() {
        val sessions: List<com.gym.app.data.entity.WorkoutSession> =
            workoutRepository.getSessionsByPlanId(planId).let { it.first() }
        var totalDuration = 0
        for (s in sessions) { totalDuration += s.durationSeconds }

        val moshi = com.squareup.moshi.Moshi.Builder()
            .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
        val intListType = com.squareup.moshi.Types.newParameterizedType(
            List::class.java, Int::class.javaObjectType
        )
        val floatListType = com.squareup.moshi.Types.newParameterizedType(
            List::class.java, Float::class.javaObjectType
        )

        val points = mutableListOf<com.gym.app.ui.components.TrendPoint>()
        for (session in sessions) {
            val records = workoutRepository.getRecordsBySessionId(session.sessionId)
            var totalVolume = 0.0
            var maxWeight = 0f
            var maxReps = 0
            for (record in records) {
                val weightList: List<Float> = try {
                    moshi.adapter<List<Float>>(floatListType).fromJson(record.weight) ?: emptyList()
                } catch (_: Exception) { emptyList() }
                val repsList: List<Int> = try {
                    moshi.adapter<List<Int>>(intListType).fromJson(record.actualReps) ?: emptyList()
                } catch (_: Exception) { emptyList() }
                val size = if (weightList.size <= repsList.size) weightList.size else repsList.size
                for (i in 0 until size) {
                    totalVolume += (weightList[i] * repsList[i]).toDouble()
                }
                val wm = weightList.maxOrNull() ?: 0f
                if (wm > maxWeight) maxWeight = wm
                val rm = repsList.maxOrNull() ?: 0
                if (rm > maxReps) maxReps = rm
            }
            points.add(
                com.gym.app.ui.components.TrendPoint(
                    date = session.date,
                    maxWeight = maxWeight,
                    maxReps = maxReps,
                    totalVolume = totalVolume.toFloat(),
                )
            )
        }

        _totalSessions.value = sessions.size
        _trendPoints.value = points
        _totalDurationSec.value = totalDuration
    }

    private fun loadExercises() {
        viewModelScope.launch {
            val pes = workoutRepository.getPlanExercisesOnce(planId)
            val items = pes.map { pe ->
                val ex = exerciseRepository.getExerciseById(pe.exerciseId)
                PlanExerciseItem(planExercise = pe, exercise = ex)
            }
            _exercises.value = items
        }
    }

    fun onPlanNameChanged(name: String) {
        _planName.value = name
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun addExercise(exercise: Exercise) {
        val current = _exercises.value.toMutableList()
        if (current.any { it.planExercise.exerciseId == exercise.id }) return

        val newPe = WorkoutPlanExercise(
            planId = planId,
            exerciseId = exercise.id,
            targetSets = 3,
            targetReps = 10,
            sortOrder = current.size,
        )
        current.add(PlanExerciseItem(planExercise = newPe, exercise = exercise))
        _exercises.value = current
        _searchQuery.value = ""
    }

    fun removeExercise(index: Int) {
        val current = _exercises.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _exercises.value = current.mapIndexed { i, item ->
                item.copy(planExercise = item.planExercise.copy(sortOrder = i))
            }
        }
    }

    fun updateSets(index: Int, sets: Int) {
        val current = _exercises.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(
                planExercise = current[index].planExercise.copy(targetSets = sets)
            )
            _exercises.value = current
        }
    }

    fun updateReps(index: Int, reps: Int) {
        val current = _exercises.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(
                planExercise = current[index].planExercise.copy(targetReps = reps)
            )
            _exercises.value = current
        }
    }

    /** 拖拽重排：fromIndex → toIndex */
    fun moveItem(fromIndex: Int, toIndex: Int) {
        val list = _exercises.value.toMutableList()
        if (fromIndex < 0 || fromIndex >= list.size || toIndex < 0 || toIndex >= list.size) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _exercises.value = list.mapIndexed { i, it ->
            it.copy(planExercise = it.planExercise.copy(sortOrder = i))
        }
    }

    fun savePlan(onSaved: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                // 更新计划名称
                val plan = workoutRepository.getPlanById(planId)
                if (plan != null) {
                    workoutRepository.updatePlan(plan.copy(name = _planName.value))
                }

                // 删除旧的动作关联，重新插入
                val items = _exercises.value.mapIndexed { index, item ->
                    item.planExercise.copy(sortOrder = index)
                }

                // 使用 DAO 删除旧记录
                workoutRepository.deletePlanExercises(planId)
                // 重新插入
                workoutRepository.insertPlanExercises(items)

                _isSaved.value = true
                onSaved()
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun buildState(): StateFlow<PlanDetailUiState> {
        val searchFlow = _searchQuery.flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                exerciseRepository.fuzzySearchExercises(query)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

        @Suppress("UNCHECKED_CAST")
        return combine(
            listOf<Flow<Any?>>(
                _planName, _createdDate, _exercises, _searchQuery, searchFlow,
                _isSaving, _isSaved, _totalSessions, _trendPoints, _totalDurationSec,
            )
        ) { values ->
            PlanDetailUiState(
                planName = values[0] as String,
                createdDate = values[1] as Long,
                exercises = values[2] as List<PlanExerciseItem>,
                searchQuery = values[3] as String,
                searchResults = values[4] as List<Exercise>,
                isSearching = (values[3] as String).isNotBlank(),
                isSaving = values[5] as Boolean,
                isSaved = values[6] as Boolean,
                totalSessions = values[7] as Int,
                trendPoints = values[8] as List<com.gym.app.ui.components.TrendPoint>,
                totalDurationSec = values[9] as Int,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlanDetailUiState(),
        )
    }

    class Factory(
        private val planId: Long,
        private val workoutRepository: WorkoutRepository,
        private val exerciseRepository: ExerciseRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlanDetailViewModel(planId, workoutRepository, exerciseRepository) as T
        }
    }
}
