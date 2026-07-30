package com.gym.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gym.app.data.entity.Exercise
import com.gym.app.data.entity.ExerciseRecord
import com.gym.app.data.entity.WorkoutPlan
import com.gym.app.data.entity.WorkoutSession
import com.gym.app.data.repository.ExerciseRepository
import com.gym.app.data.repository.WorkoutRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 单组数据 */
data class ActualSet(
    val reps: Int = 0,
    val weight: Float = 2.5f,
    val completed: Boolean = false,
)

/** 上次训练摘要 */
data class PreviousSummary(
    val weight: Float,
    val reps: Int,
)

/** 历史最佳组 */
data class BestSet(
    val weight: Float,
    val reps: Int,
)

/** 训练中单个动作的完整状态 */
data class ExerciseWorkoutState(
    val exercise: Exercise,
    val targetSets: Int,
    val targetReps: Int,
    val actualSets: List<ActualSet>,
    val sortOrder: Int,
    val previous: PreviousSummary? = null,
    val bestSet: BestSet? = null,
    val musclesWorked: List<String> = emptyList(),
    val isExpanded: Boolean = true,
)

data class WorkoutUiState(
    val planName: String = "",
    val exercises: List<ExerciseWorkoutState> = emptyList(),
    val elapsedSeconds: Int = 0,
    val tonsLifted: Float = 0f,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val longPressProgress: Float = 0f, // 0..1, 长按 3 秒进度
    val isCompleted: Boolean = false,
    val savedSessionId: Long? = null,
    val isSaving: Boolean = false,
    val showCompletionSheet: Boolean = false,
)

class WorkoutViewModel(
    private val planId: Long,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val intListAdapter = moshi.adapter<List<Int>>(
        Types.newParameterizedType(List::class.java, Int::class.javaObjectType)
    )
    private val floatListAdapter = moshi.adapter<List<Float>>(
        Types.newParameterizedType(List::class.java, Float::class.javaObjectType)
    )
    private val stringListAdapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
    )

    private var timerJob: Job? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        viewModelScope.launch {
            val plan = workoutRepository.getPlanById(planId)
            val planExercises = workoutRepository.getPlanExercisesOnce(planId)

            val exerciseStates = planExercises.mapNotNull { pe ->
                val exercise = exerciseRepository.getExerciseById(pe.exerciseId) ?: return@mapNotNull null

                // 查询上次记录
                val lastRecord = workoutRepository.getLatestRecordForExercise(planId, pe.exerciseId)
                val previous = lastRecord?.let { record ->
                    val repsList = parseIntArray(record.actualReps)
                    val weightList = parseFloatArray(record.weight)
                    val lastSetIndex = repsList.lastIndex
                    PreviousSummary(
                        weight = weightList.getOrElse(lastSetIndex) { 0f },
                        reps = repsList.getOrElse(lastSetIndex) { 0 },
                    )
                }

                // 计算历史最佳组
                val allRecords = workoutRepository.getRecordsForExerciseHistory(planId, pe.exerciseId)
                val bestSet = calculateBestSet(allRecords)

                // 解析肌群
                val muscles = parseMuscles(exercise.secondaryMuscles, exercise.target)

                // 自动回填上次训练的每组重量和次数（方案C）
                val lastReps = lastRecord?.let { parseIntArray(it.actualReps) } ?: emptyList()
                val lastWeights = lastRecord?.let { parseFloatArray(it.weight) } ?: emptyList()

                ExerciseWorkoutState(
                    exercise = exercise,
                    targetSets = pe.targetSets,
                    targetReps = pe.targetReps,
                    actualSets = List(pe.targetSets) { setIndex ->
                        ActualSet(
                            reps = lastReps.getOrElse(setIndex) { pe.targetReps },
                            weight = lastWeights.getOrElse(setIndex) { 2.5f },
                        )
                    },
                    sortOrder = pe.sortOrder,
                    previous = previous,
                    bestSet = bestSet,
                    musclesWorked = muscles,
                )
            }

            _uiState.value = _uiState.value.copy(
                planName = plan?.name ?: "",
                exercises = exerciseStates,
            )
        }
        startTimer()
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val state = _uiState.value
                if (state.isRunning) {
                    _uiState.value = state.copy(
                        elapsedSeconds = state.elapsedSeconds + 1,
                        tonsLifted = calculateTonsLifted(state.exercises),
                    )
                } else {
                    _uiState.value = state.copy(
                        tonsLifted = calculateTonsLifted(state.exercises),
                    )
                }
            }
        }
    }

    /** 开始训练（状态一→状态二） */
    fun startWorkout() {
        _uiState.value = _uiState.value.copy(isRunning = true, isPaused = false)
    }

    /** 暂停训练（状态二→状态三） */
    fun pauseWorkout() {
        _uiState.value = _uiState.value.copy(isRunning = false, isPaused = true)
    }

    /** 暂停后继续（状态三→状态二） */
    fun resumeWorkout() {
        _uiState.value = _uiState.value.copy(isRunning = true, isPaused = false)
    }

    /** 长按进度更新 */
    fun updateLongPressProgress(progress: Float) {
        _uiState.value = _uiState.value.copy(longPressProgress = progress.coerceIn(0f, 1f))
    }

    /** 长按完成触发 */
    fun triggerCompletion() {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, isRunning = false)
            timerJob?.cancel()

            val today = dateFormat.format(Date())
            val session = WorkoutSession(
                planId = planId,
                date = today,
                durationSeconds = _uiState.value.elapsedSeconds,
            )

            val records = _uiState.value.exercises.map { state ->
                val repsList = state.actualSets.map { it.reps }
                val weightList = state.actualSets.map { it.weight }
                ExerciseRecord(
                    sessionId = 0,
                    exerciseId = state.exercise.id,
                    targetSets = state.targetSets,
                    targetReps = state.targetReps,
                    actualSets = state.actualSets.size,
                    actualReps = intListAdapter.toJson(repsList),
                    weight = floatListAdapter.toJson(weightList),
                    sortOrder = state.sortOrder,
                )
            }

            val sessionId = workoutRepository.saveSessionWithRecords(session, records)
            _uiState.value = _uiState.value.copy(
                isCompleted = true,
                isSaving = false,
                savedSessionId = sessionId,
                showCompletionSheet = true,
                longPressProgress = 0f,
            )
        }
    }

    /** 关闭完成弹窗 */
    fun dismissCompletionSheet() {
        _uiState.value = _uiState.value.copy(showCompletionSheet = false)
    }

    fun toggleExpanded(exerciseIndex: Int) {
        val exercises = _uiState.value.exercises.toMutableList()
        if (exerciseIndex !in exercises.indices) return
        exercises[exerciseIndex] = exercises[exerciseIndex].copy(
            isExpanded = !exercises[exerciseIndex].isExpanded
        )
        _uiState.value = _uiState.value.copy(exercises = exercises)
    }

    fun toggleSetCompleted(exerciseIndex: Int, setIndex: Int) {
        val exercises = _uiState.value.exercises.toMutableList()
        if (exerciseIndex !in exercises.indices) return
        val state = exercises[exerciseIndex]
        if (setIndex !in state.actualSets.indices) return
        val newSets = state.actualSets.toMutableList()
        newSets[setIndex] = newSets[setIndex].copy(completed = !newSets[setIndex].completed)
        exercises[exerciseIndex] = state.copy(actualSets = newSets)
        _uiState.value = _uiState.value.copy(
            exercises = exercises,
            tonsLifted = calculateTonsLifted(exercises),
        )
    }

    fun addSet(exerciseIndex: Int) {
        val exercises = _uiState.value.exercises.toMutableList()
        if (exerciseIndex !in exercises.indices) return
        val state = exercises[exerciseIndex]
        val last = state.actualSets.lastOrNull()
        exercises[exerciseIndex] = state.copy(
            actualSets = state.actualSets + ActualSet(
                reps = last?.reps ?: state.targetReps,
                weight = last?.weight ?: 2.5f,
            )
        )
        _uiState.value = _uiState.value.copy(
            exercises = exercises,
            tonsLifted = calculateTonsLifted(exercises),
        )
    }

    fun removeSet(exerciseIndex: Int, setIndex: Int) {
        val exercises = _uiState.value.exercises.toMutableList()
        if (exerciseIndex !in exercises.indices) return
        val state = exercises[exerciseIndex]
        if (state.actualSets.size <= 1) return
        exercises[exerciseIndex] = state.copy(
            actualSets = state.actualSets.filterIndexed { i, _ -> i != setIndex }
        )
        _uiState.value = _uiState.value.copy(
            exercises = exercises,
            tonsLifted = calculateTonsLifted(exercises),
        )
    }

    fun updateSetReps(exerciseIndex: Int, setIndex: Int, reps: Int) {
        updateSet(exerciseIndex, setIndex) { it.copy(reps = reps) }
    }

    fun updateSetWeight(exerciseIndex: Int, setIndex: Int, weight: Float) {
        updateSet(exerciseIndex, setIndex) { it.copy(weight = weight) }
    }

    private fun updateSet(exerciseIndex: Int, setIndex: Int, transform: (ActualSet) -> ActualSet) {
        val exercises = _uiState.value.exercises.toMutableList()
        if (exerciseIndex !in exercises.indices) return
        val state = exercises[exerciseIndex]
        if (setIndex !in state.actualSets.indices) return
        val newSets = state.actualSets.toMutableList()
        newSets[setIndex] = transform(newSets[setIndex])
        exercises[exerciseIndex] = state.copy(actualSets = newSets)
        _uiState.value = _uiState.value.copy(
            exercises = exercises,
            tonsLifted = calculateTonsLifted(exercises),
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    // --- 辅助方法 ---

    private fun calculateTonsLifted(exercises: List<ExerciseWorkoutState>): Float {
        return exercises.sumOf { state ->
            state.actualSets.sumOf { set ->
                (set.weight.toDouble() * set.reps).toDouble()
            }
        }.toFloat()
    }

    private fun calculateBestSet(allRecords: List<ExerciseRecord>): BestSet? {
        if (allRecords.isEmpty()) return null
        var bestWeight = 0f
        var bestReps = 0
        allRecords.forEach { record ->
            val weightList = parseFloatArray(record.weight)
            val repsList = parseIntArray(record.actualReps)
            weightList.forEachIndexed { i, w ->
                val r = repsList.getOrElse(i) { 0 }
                if (w > bestWeight || (w == bestWeight && r > bestReps)) {
                    bestWeight = w
                    bestReps = r
                }
            }
        }
        return if (bestWeight > 0f || bestReps > 0) BestSet(bestWeight, bestReps) else null
    }

    private fun parseMuscles(secondaryMusclesJson: String, target: String): List<String> {
        val secondary = try {
            stringListAdapter.fromJson(secondaryMusclesJson) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        return listOf(target) + secondary
    }

    private fun parseIntArray(json: String): List<Int> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try { intListAdapter.fromJson(json) ?: emptyList() } catch (_: Exception) { emptyList() }
    }

    private fun parseFloatArray(json: String): List<Float> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try { floatListAdapter.fromJson(json) ?: emptyList() } catch (_: Exception) { emptyList() }
    }

    class Factory(
        private val planId: Long,
        private val workoutRepository: WorkoutRepository,
        private val exerciseRepository: ExerciseRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WorkoutViewModel(planId, workoutRepository, exerciseRepository) as T
        }
    }
}
