package com.gym.app.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gym.app.data.entity.Exercise
import com.gym.app.data.repository.ExerciseRepository
import com.gym.app.ui.components.LocaleStrings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

data class ExerciseListUiState(
    val exercises: List<Exercise> = emptyList(),
    val bodyParts: List<String> = emptyList(),
    val selectedBodyPart: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
)

class ExerciseListViewModel(
    private val repository: ExerciseRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedBodyPart = MutableStateFlow<String?>(null)
    private var searchJob: Job? = null

    val uiState: StateFlow<ExerciseListUiState> = combine(
        _searchQuery,
        _selectedBodyPart,
    ) { query, bodyPart ->
        Pair(query, bodyPart)
    }.flatMapLatest { (query, bodyPart) ->
        when {
            query.isNotBlank() -> repository.fuzzySearchExercises(query)
            bodyPart != null -> repository.getExercisesByBodyPart(bodyPart)
            else -> repository.getAllExercises()
        }
    }.combine(
        combine(
            repository.getAllBodyParts(),
            _searchQuery,
            _selectedBodyPart,
        ) { parts, query, bodyPart ->
            Triple(parts, query, bodyPart)
        }
    ) { exercises, (bodyParts, query, bodyPart) ->
        ExerciseListUiState(
            exercises = exercises,
            bodyParts = bodyParts,
            selectedBodyPart = bodyPart,
            searchQuery = query,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExerciseListUiState(isLoading = true),
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onBodyPartSelected(bodyPart: String?) {
        _selectedBodyPart.value = bodyPart
        // 选择筛选条件时清空搜索
        if (bodyPart != null) {
            _searchQuery.value = ""
        }
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedBodyPart.value = null
    }

    /**
     * 根据部位名称返回中文标签。
     */
    companion object {
        val BODY_PART_LABELS = mapOf(
            "back" to "背部",
            "cardio" to "有氧",
            "chest" to "胸部",
            "lower arms" to "前臂",
            "lower legs" to "小腿",
            "neck" to "颈部",
            "shoulders" to "肩部",
            "upper arms" to "上臂",
            "upper legs" to "大腿",
            "waist" to "腰部",
        )

        private val TARGET_LABELS = mapOf(
            "abductors" to "外展肌",
            "abs" to "腹肌",
            "adductors" to "内收肌",
            "biceps" to "肱二头肌",
            "calves" to "小腿",
            "cardiovascular system" to "心血管系统",
            "delts" to "三角肌",
            "forearms" to "前臂",
            "glutes" to "臀肌",
            "hamstrings" to "腘绳肌",
            "lats" to "背阔肌",
            "levator scapulae" to "肩胛提肌",
            "pectorals" to "胸肌",
            "quads" to "股四头肌",
            "serratus anterior" to "前锯肌",
            "spine" to "脊柱",
            "traps" to "斜方肌",
            "triceps" to "肱三头肌",
            "upper back" to "上背",
        )

        private val EQUIPMENT_LABELS = mapOf(
            "assisted" to "辅助",
            "band" to "弹力带",
            "barbell" to "杠铃",
            "body weight" to "自重",
            "bosu ball" to "波速球",
            "cable" to "绳索",
            "dumbbell" to "哑铃",
            "elliptical machine" to "椭圆机",
            "ez barbell" to "EZ杠铃",
            "hammer" to "锤式",
            "kettlebell" to "壶铃",
            "leverage machine" to "杠杆器械",
            "medicine ball" to "药球",
            "olympic barbell" to "奥林匹克杠铃",
            "resistance band" to "弹力带",
            "roller" to "滚轴",
            "rope" to "绳索",
            "skierg machine" to "滑雪测功仪",
            "sled machine" to "雪橇机",
            "smith machine" to "史密斯机",
            "stability ball" to "瑜伽球",
            "stationary bike" to "固定单车",
            "stepmill machine" to "台阶机",
            "tire" to "轮胎",
            "trap bar" to "六角杠",
            "upper body ergometer" to "上身测功仪",
            "weighted" to "负重",
            "wheel roller" to "健腹轮",
        )

        fun bodyPartLabel(en: String): String = LocaleStrings.bodyPartLabel(en)
        fun targetLabel(en: String): String = TARGET_LABELS[en] ?: en
        fun equipmentLabel(en: String): String = EQUIPMENT_LABELS[en] ?: en
    }

    class Factory(private val repository: ExerciseRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ExerciseListViewModel(repository) as T
        }
    }
}
