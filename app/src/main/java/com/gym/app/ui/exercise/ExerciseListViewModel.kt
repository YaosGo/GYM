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

        fun bodyPartLabel(en: String): String = LocaleStrings.bodyPartLabel(en)
    }

    class Factory(private val repository: ExerciseRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ExerciseListViewModel(repository) as T
        }
    }
}
