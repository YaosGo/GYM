package com.gym.app.ui.workout

import android.os.SystemClock
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
    /** 历史最佳（训练开始前从历史记录算出，本场训练内不变），取消勾选后最佳可还原 */
    val historyBest: BestSet? = null,
    /** 展示用最佳 = 历史最佳与本次已完成组的最大值；取消勾选后自动还原 */
    val bestSet: BestSet? = null,
    /** 本次训练已打破历史最佳（勾选完成后实时对比），「最佳」栏打星 */
    val isNewRecord: Boolean = false,
    val musclesWorked: List<String> = emptyList(),
    /** 进训练页默认全部折叠，练到哪个展开哪个 */
    val isExpanded: Boolean = false,
    /** 计划目标重量（kg），无上次数据时预填每组重量；0 表示计划未设定 */
    val targetWeight: Float = 0f,
)

data class WorkoutUiState(
    val planName: String = "",
    val exercises: List<ExerciseWorkoutState> = emptyList(),
    val elapsedSeconds: Int = 0,
    val completedTons: Float = 0f,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val longPressProgress: Float = 0f, // 0..1, 长按 3 秒进度
    val isCompleted: Boolean = false,
    val savedSessionId: Long? = null,
    val isSaving: Boolean = false,
    val showCompletionSheet: Boolean = false,
    /** 训练开始时的体重快照（kg），用于助力/负重动作的有效重量换算 */
    val bodyWeightKg: Float = 0f,
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

    private val timer = WorkoutTimer { SystemClock.elapsedRealtime() }

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
                            // 预填重量：上次数据优先，没有则用计划目标重量
                            weight = lastWeights.getOrElse(setIndex) {
                                if (pe.targetWeight > 0f) pe.targetWeight else 2.5f
                            },
                        )
                    },
                    sortOrder = pe.sortOrder,
                    previous = previous,
                    historyBest = bestSet,
                    bestSet = bestSet,
                    musclesWorked = muscles,
                    targetWeight = pe.targetWeight,
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
                _uiState.value = state.copy(
                    elapsedSeconds = currentElapsedSeconds(),
                    completedTons = calculateCompletedTons(state.exercises),
                )
            }
        }
    }

    /** 当前训练时长（秒）：时间戳差值，后台/熄屏后回前台自动校正；暂停中冻结。 */
    private fun currentElapsedSeconds(): Int = timer.currentSeconds()

    /** 开始训练（状态一→状态二），记录本次训练的体重快照。 */
    fun startWorkout(bodyWeightKg: Float) {
        timer.start()
        _uiState.value = _uiState.value.copy(
            isRunning = true,
            isPaused = false,
            elapsedSeconds = 0,
            bodyWeightKg = bodyWeightKg.coerceAtLeast(0f),
        )
    }

    /** 暂停训练（状态二→状态三）：冻结当前时长 */
    fun pauseWorkout() {
        timer.pause()
        _uiState.value = _uiState.value.copy(
            isRunning = false,
            isPaused = true,
            elapsedSeconds = timer.currentSeconds(),
        )
    }

    /** 暂停后继续（状态三→状态二）：结算暂停时长，恢复计时 */
    fun resumeWorkout() {
        timer.resume()
        _uiState.value = _uiState.value.copy(isRunning = true, isPaused = false)
    }

    /** 长按进度更新 */
    fun updateLongPressProgress(progress: Float) {
        _uiState.value = _uiState.value.copy(longPressProgress = progress.coerceIn(0f, 1f))
    }

    /** 长按完成触发 */
    fun triggerCompletion() {
        if (_uiState.value.isSaving) return
        // 结算最终时长并停表（elapsedRealtime 时间戳差值）
        val finalElapsedSeconds = timer.finalSeconds()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, isRunning = false)
            timerJob?.cancel()

            val today = dateFormat.format(Date())
            val bodyWeightKg = _uiState.value.bodyWeightKg
            val session = WorkoutSession(
                planId = planId,
                date = today,
                durationSeconds = finalElapsedSeconds,
                bodyWeightKg = bodyWeightKg,
            )

            // 仅记录用户实际勾选完成的组；未完成任何组的动作不写入记录
            val records = _uiState.value.exercises.mapNotNull { state ->
                val completedSets = state.actualSets.filter { it.completed }
                if (completedSets.isEmpty()) return@mapNotNull null
                val repsList = completedSets.map { it.reps }
                val weightList = completedSets.map { it.weight }
                // 有效对抗重量与 weight 一一对应：助力=体重−助力值，负重=体重+附加重
                val effectiveList = completedSets.map { effectiveWeight(state.exercise, it.weight, bodyWeightKg) }
                ExerciseRecord(
                    sessionId = 0,
                    exerciseId = state.exercise.id,
                    targetSets = state.targetSets,
                    targetReps = state.targetReps,
                    actualSets = completedSets.size,
                    actualReps = intListAdapter.toJson(repsList),
                    weight = floatListAdapter.toJson(weightList),
                    effectiveWeight = floatListAdapter.toJson(effectiveList),
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
        exercises[exerciseIndex] = withBest(state, newSets)
        _uiState.value = _uiState.value.copy(
            exercises = exercises,
            completedTons = calculateCompletedTons(exercises),
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
                weight = last?.weight ?: (if (state.targetWeight > 0f) state.targetWeight else 2.5f),
            )
        )
        _uiState.value = _uiState.value.copy(
            exercises = exercises,
            completedTons = calculateCompletedTons(exercises),
        )
    }

    fun removeSet(exerciseIndex: Int, setIndex: Int) {
        val exercises = _uiState.value.exercises.toMutableList()
        if (exerciseIndex !in exercises.indices) return
        val state = exercises[exerciseIndex]
        if (state.actualSets.size <= 1) return
        exercises[exerciseIndex] = withBest(
            state,
            state.actualSets.filterIndexed { i, _ -> i != setIndex },
        )
        _uiState.value = _uiState.value.copy(
            exercises = exercises,
            completedTons = calculateCompletedTons(exercises),
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
        exercises[exerciseIndex] = withBest(state, newSets)
        _uiState.value = _uiState.value.copy(
            exercises = exercises,
            completedTons = calculateCompletedTons(exercises),
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    // --- 辅助方法 ---

    /** 完成重量：仅统计已勾选完成的组（有效重量 × 次数）。 */
    private fun calculateCompletedTons(exercises: List<ExerciseWorkoutState>): Float {
        val bodyWeightKg = _uiState.value.bodyWeightKg
        return exercises.sumOf { state ->
            state.actualSets.filter { it.completed }.sumOf { set ->
                (effectiveWeight(state.exercise, set.weight, bodyWeightKg).toDouble() * set.reps).toDouble()
            }
        }.toFloat()
    }

    private fun withBest(state: ExerciseWorkoutState, sets: List<ActualSet>): ExerciseWorkoutState =
        recomputeBest(state, sets)

    /** 有效对抗重量：助力=体重−助力值，负重=体重+附加重；无体重快照时回退到原始重量。 */
    private fun effectiveWeight(exercise: Exercise, recordedWeight: Float, bodyWeightKg: Float): Float =
        if (bodyWeightKg > 0f) exercise.effectiveWeight(bodyWeightKg, recordedWeight) else recordedWeight

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

/**
 * 勾选完成/取消勾选/修改次数重量后重算「最佳」。
 * - 基准取历史最佳 historyBest（本场训练不变），再与当前已完成的组对比，
 *   打破纪录才更新 bestSet 并打星；取消勾选纪录组后自动还原历史最佳。
 * - 必须把最新的 sets 一并写回 state（否则勾选/输入/删组的修改全部丢失）。
 */
internal fun recomputeBest(state: ExerciseWorkoutState, sets: List<ActualSet>): ExerciseWorkoutState {
    var best = state.historyBest
    var isNewRecord = false
    sets.filter { it.completed }.forEach { s ->
        val w = s.weight
        val r = s.reps
        if (w <= 0f) return@forEach
        if (best == null) {
            best = BestSet(w, r)
            isNewRecord = true
        } else if (w > best.weight || (w == best.weight && r > best.reps)) {
            best = BestSet(w, r)
            isNewRecord = true
        }
    }
    return state.copy(actualSets = sets, bestSet = best, isNewRecord = isNewRecord)
}
