package com.gym.app.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gym.app.data.entity.WorkoutPlan
import com.gym.app.data.entity.WorkoutPlanExercise
import com.gym.app.data.planshare.PlanShareCodec
import com.gym.app.data.repository.ExerciseRepository
import com.gym.app.data.repository.WorkoutRepository
import com.gym.app.ui.components.LocaleStrings
import com.gym.app.ui.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlanListUiState(
    val plans: List<WorkoutPlan> = emptyList(),
    val lastDurations: Map<Long, Int> = emptyMap(), // planId → durationSeconds of last session
    val showImageImport: Boolean = true, // 设置开关：是否显示「从图片导入」入口
)

/** 导入分享串的预览条目。 */
data class ImportPreviewItem(
    val name: String,
    val sets: Int,
    val reps: Int,
    val weightMode: Int,
    val willCreateCustom: Boolean, // 接收方动作库没有该动作，导入时将自动创建自定义动作
)

/** 导入分享串的预览。 */
data class ImportPreview(
    val planName: String,
    val items: List<ImportPreviewItem>,
)

/** 导入对话框状态：输入 → 解析预览 → 确认导入。 */
data class ImportUiState(
    val showDialog: Boolean = false,
    val raw: String = "",
    val preview: ImportPreview? = null,
    val error: String? = null,
    val isWorking: Boolean = false,
)

class PlanListViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val settings: AppSettings,
) : ViewModel() {

    private val _importState = MutableStateFlow(ImportUiState())
    val importState: StateFlow<ImportUiState> = _importState

    private val _lastDurations = kotlinx.coroutines.flow.MutableStateFlow<Map<Long, Int>>(emptyMap())

    val uiState: StateFlow<PlanListUiState> = combine(
        workoutRepository.getAllPlans(),
        _lastDurations,
        settings.showImageImport,
    ) { plans, durations, showImageImport ->
        PlanListUiState(plans = plans, lastDurations = durations, showImageImport = showImageImport)
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

    // --- 分享串导入 ---

    /** 打开导入对话框；剪贴板里若是分享串则自动填入。 */
    fun openImportDialog(initialText: String = "") {
        _importState.value = ImportUiState(showDialog = true, raw = initialText)
    }

    fun closeImportDialog() {
        _importState.value = ImportUiState()
    }

    fun onImportTextChanged(text: String) {
        _importState.value = _importState.value.copy(raw = text, preview = null, error = null)
    }

    /** 解析分享串并生成预览（校验动作是否在接收方动作库中）。 */
    fun parseImportText() {
        val plan = PlanShareCodec.decode(_importState.value.raw)
        if (plan == null) {
            _importState.value = _importState.value.copy(error = LocaleStrings.invalidShareText, preview = null)
            return
        }
        viewModelScope.launch {
            _importState.value = _importState.value.copy(isWorking = true)
            val items = plan.exercises.map { ex ->
                val exists = exerciseRepository.getExerciseById(ex.id) != null
                ImportPreviewItem(
                    name = ex.name,
                    sets = ex.sets,
                    reps = ex.reps,
                    weightMode = ex.weightMode,
                    willCreateCustom = !exists,
                )
            }
            _importState.value = _importState.value.copy(
                preview = ImportPreview(plan.name, items),
                error = null,
                isWorking = false,
            )
        }
    }

    /** 从预览返回输入框。 */
    fun backToImportInput() {
        _importState.value = _importState.value.copy(preview = null, error = null)
    }

    /**
     * 确认导入：解析 → 动作逐一定位（缺失自动创建自定义动作 + 同步助力/负重标记）→
     * 创建计划与动作关联，返回新计划 id。
     */
    fun confirmImport(onImported: (Long) -> Unit) {
        val plan = PlanShareCodec.decode(_importState.value.raw) ?: return
        viewModelScope.launch {
            _importState.value = _importState.value.copy(isWorking = true)
            val resolvedIds = plan.exercises.map { ex ->
                val existing = exerciseRepository.getExerciseById(ex.id)
                if (existing != null) {
                    // 助力/负重是动作的全局标记，分享者标了就同步（不覆盖接收方自己的普通标记）
                    if (ex.weightMode != 0 && existing.weightMode != ex.weightMode) {
                        exerciseRepository.updateWeightMode(existing.id, ex.weightMode)
                    }
                    existing.id
                } else {
                    // 接收方没有该动作（自定义动作或动作库版本差异）→ 用分享串里的名称创建自定义动作
                    val created = exerciseRepository.createCustomExercise(ex.name)
                    if (ex.weightMode != 0) {
                        exerciseRepository.updateWeightMode(created.id, ex.weightMode)
                    }
                    created.id
                }
            }
            val newPlan = WorkoutPlan(name = plan.name)
            val planExercises = resolvedIds.mapIndexed { index, exerciseId ->
                WorkoutPlanExercise(
                    planId = 0,
                    exerciseId = exerciseId,
                    targetSets = plan.exercises[index].sets,
                    targetReps = plan.exercises[index].reps,
                    sortOrder = index,
                )
            }
            val planId = workoutRepository.createPlanWithExercises(newPlan, planExercises)
            _importState.value = ImportUiState()
            onImported(planId)
        }
    }

    class Factory(
        private val workoutRepository: WorkoutRepository,
        private val exerciseRepository: ExerciseRepository,
        private val settings: AppSettings,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlanListViewModel(workoutRepository, exerciseRepository, settings) as T
        }
    }
}
