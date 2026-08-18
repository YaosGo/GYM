package com.gym.app.ui.importimage

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.common.InputImage
import com.gym.app.data.entity.Exercise
import com.gym.app.data.entity.ExerciseRecord
import com.gym.app.data.entity.WorkoutPlan
import com.gym.app.data.entity.WorkoutPlanExercise
import com.gym.app.data.entity.WorkoutSession
import com.gym.app.data.importer.ImportMatcher
import com.gym.app.data.importer.ImportParser
import com.gym.app.data.repository.ExerciseRepository
import com.gym.app.data.repository.WorkoutRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** 导入流程阶段。 */
enum class ImportPhase { EMPTY, RECOGNIZING, REVIEW, SAVING, ERROR }

/** 导入确认页中一组的可编辑状态。 */
data class ImportSetState(
    val weight: Float = 0f,
    val reps: Int = 0,
)

/** 导入确认页中一个动作的可编辑状态。 */
data class ImportExerciseState(
    val rawName: String,
    val exercise: Exercise?, // null = 未匹配到动作库，导入时跳过
    val sets: List<ImportSetState>,
)

data class ImportUiState(
    val phase: ImportPhase = ImportPhase.EMPTY,
    val imageUri: String? = null,
    val planName: String = "",
    val date: String = "",
    val exercises: List<ImportExerciseState> = emptyList(),
    /** OCR 原始逐行文本（诊断用，确认页可展开查看）。 */
    val rawLines: List<String> = emptyList(),
    val errorMessage: String? = null,
) {
    /** 能导入的动作数（已匹配动作库且有组）。 */
    val matchedCount: Int get() = exercises.count { it.exercise != null && it.sets.isNotEmpty() }

    /** 能导入的总组数。 */
    val matchedSetCount: Int get() = exercises
        .filter { it.exercise != null }
        .sumOf { it.sets.size }
}

/**
 * 图片导入 ViewModel：选图 → OCR 识别 → 解析匹配 → 确认编辑 → 写入计划 + 训练记录。
 * 训练时长等截图缺失信息：时长记 0，备注标注「从图片导入」。
 */
class ImportViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val intListAdapter = moshi.adapter<List<Int>>(
        Types.newParameterizedType(List::class.java, Int::class.javaObjectType)
    )
    private val floatListAdapter = moshi.adapter<List<Float>>(
        Types.newParameterizedType(List::class.java, Float::class.javaObjectType)
    )

    fun onImagePicked(context: Context, uri: Uri) {
        if (_uiState.value.phase == ImportPhase.RECOGNIZING) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                phase = ImportPhase.RECOGNIZING,
                imageUri = uri.toString(),
                errorMessage = null,
            )
            try {
                val lines = recognizeText(context, uri)
                val allExercises = exerciseRepository.getAllExercises().first()
                val parsed = ImportParser.parse(lines)
                val matched = ImportMatcher.match(parsed, allExercises)
                val date = ImportParser.parseDate(lines, LocalDate.now())
                val exercises = matched.map { entry ->
                    ImportExerciseState(
                        rawName = entry.rawName,
                        exercise = entry.exercise,
                        sets = entry.sets.map { ImportSetState(it.weight, it.reps) },
                    )
                }
                _uiState.value = _uiState.value.copy(
                    phase = ImportPhase.REVIEW,
                    date = date,
                    planName = defaultPlanName(date),
                    exercises = exercises,
                    rawLines = lines,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    phase = ImportPhase.ERROR,
                    errorMessage = e.message ?: "识别失败",
                )
            }
        }
    }

    /** 重新开始（清空状态回到选图页）。 */
    fun reset() {
        _uiState.value = ImportUiState()
    }

    fun updatePlanName(name: String) {
        _uiState.value = _uiState.value.copy(planName = name)
    }

    fun updateDate(date: String) {
        _uiState.value = _uiState.value.copy(date = date)
    }

    fun updateSetWeight(exerciseIndex: Int, setIndex: Int, weight: Float) {
        updateSet(exerciseIndex, setIndex) { it.copy(weight = weight.coerceAtLeast(0f)) }
    }

    fun updateSetReps(exerciseIndex: Int, setIndex: Int, reps: Int) {
        updateSet(exerciseIndex, setIndex) { it.copy(reps = reps.coerceAtLeast(1)) }
    }

    fun removeExercise(exerciseIndex: Int) {
        val exercises = _uiState.value.exercises.toMutableList()
        if (exerciseIndex !in exercises.indices) return
        exercises.removeAt(exerciseIndex)
        _uiState.value = _uiState.value.copy(exercises = exercises)
    }

    /** 写入：创建计划 + 一次训练记录（图片即已完成的训练，全部组视为完成）。 */
    fun importNow(onDone: () -> Unit) {
        val state = _uiState.value
        if (state.phase == ImportPhase.SAVING) return
        val valid = state.exercises.filter { it.exercise != null && it.sets.isNotEmpty() }
        if (valid.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(phase = ImportPhase.SAVING, errorMessage = null)
            try {
                val name = state.planName.trim().ifBlank { defaultPlanName(state.date) }
                val planId = workoutRepository.createPlanWithExercises(
                    plan = WorkoutPlan(planId = 0, name = name),
                    exercises = valid.mapIndexed { index, item ->
                        WorkoutPlanExercise(
                            planId = 0, // DAO 事务内替换为真实 planId
                            exerciseId = item.exercise!!.id,
                            targetSets = item.sets.size,
                            targetReps = item.sets.maxOfOrNull { it.reps } ?: 0,
                            sortOrder = index,
                        )
                    },
                )
                val records = valid.mapIndexed { index, item ->
                    ExerciseRecord(
                        sessionId = 0,
                        exerciseId = item.exercise!!.id,
                        targetSets = item.sets.size,
                        targetReps = item.sets.maxOfOrNull { it.reps } ?: 0,
                        actualSets = item.sets.size,
                        actualReps = intListAdapter.toJson(item.sets.map { it.reps }),
                        weight = floatListAdapter.toJson(item.sets.map { it.weight }),
                        sortOrder = index,
                    )
                }
                // 截图无法提供时长等元信息：时长记 0，备注标注来源
                workoutRepository.saveSessionWithRecords(
                    session = WorkoutSession(
                        planId = planId,
                        date = state.date,
                        durationSeconds = 0,
                        note = "从图片导入",
                    ),
                    records = records,
                )
                onDone()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    phase = ImportPhase.REVIEW,
                    errorMessage = e.message ?: "导入失败",
                )
            }
        }
    }

    private fun updateSet(
        exerciseIndex: Int,
        setIndex: Int,
        transform: (ImportSetState) -> ImportSetState,
    ) {
        val exercises = _uiState.value.exercises.toMutableList()
        if (exerciseIndex !in exercises.indices) return
        val item = exercises[exerciseIndex]
        if (setIndex !in item.sets.indices) return
        val sets = item.sets.toMutableList()
        sets[setIndex] = transform(sets[setIndex])
        exercises[exerciseIndex] = item.copy(sets = sets)
        _uiState.value = _uiState.value.copy(exercises = exercises)
    }

    private fun defaultPlanName(date: String): String =
        "导入训练" + date.takeLast(5).replace("-", "")

    /** 在 IO 线程执行中文 OCR，返回逐行文本。 */
    private suspend fun recognizeText(context: Context, uri: Uri): List<String> =
        withContext(Dispatchers.IO) {
            val recognizer = TextRecognition.getClient(
                ChineseTextRecognizerOptions.Builder().build()
            )
            try {
                val image = InputImage.fromFilePath(context, uri)
                val result = suspendCancellableCoroutine<Text> { cont ->
                    recognizer.process(image)
                        .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                        .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
                }
                result.textBlocks.flatMap { block -> block.lines.map { it.text } }
            } finally {
                recognizer.close()
            }
        }

    class Factory(
        private val workoutRepository: WorkoutRepository,
        private val exerciseRepository: ExerciseRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ImportViewModel(workoutRepository, exerciseRepository) as T
        }
    }
}
