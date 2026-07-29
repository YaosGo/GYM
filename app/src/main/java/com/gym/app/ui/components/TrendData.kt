package com.gym.app.ui.components

import com.gym.app.data.entity.ExerciseRecord
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/** 单个训练日的数据点 */
data class TrendPoint(
    val date: String,
    val maxWeight: Float,
    val maxReps: Int,
    val totalVolume: Float, // Σ(weight × reps)
)

/** 进步/退步对比结果 */
data class ProgressResult(
    val exerciseId: String,
    val exerciseName: String,
    val currentMaxWeight: Float,
    val previousMaxWeight: Float,
    val currentMaxReps: Int,
    val previousMaxReps: Int,
    val currentTotalVolume: Float,
    val previousTotalVolume: Float,
) {
    val weightTrend: Int get() = if (currentMaxWeight > previousMaxWeight) 1 else if (currentMaxWeight < previousMaxWeight) -1 else 0
    val repsTrend: Int get() = if (currentMaxReps > previousMaxReps) 1 else if (currentMaxReps < previousMaxReps) -1 else 0
    val volumeTrend: Int get() = if (currentTotalVolume > previousTotalVolume) 1 else if (currentTotalVolume < previousTotalVolume) -1 else 0

    val hasProgress: Boolean get() = weightTrend > 0 || repsTrend > 0 || volumeTrend > 0
    val hasRegression: Boolean get() = weightTrend < 0 || repsTrend < 0 || volumeTrend < 0
    val isFirstTime: Boolean get() = previousMaxWeight == 0f && previousMaxReps == 0
}

/** 从 ExerciseRecord 列表提取趋势数据点 */
fun extractTrendPoints(records: List<ExerciseRecord>, dates: List<String>): List<TrendPoint> {
    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    val intListType = Types.newParameterizedType(List::class.java, Int::class.java)
    val floatListType = Types.newParameterizedType(List::class.java, Float::class.java)

    return records.mapIndexed { index, record ->
        val repsList: List<Int> = try {
            moshi.adapter<List<Int>>(intListType).fromJson(record.actualReps) ?: emptyList()
        } catch (_: Exception) { emptyList() }
        val weightList: List<Float> = try {
            moshi.adapter<List<Float>>(floatListType).fromJson(record.weight) ?: emptyList()
        } catch (_: Exception) { emptyList() }

        val maxWeight = weightList.maxOrNull() ?: 0f
        val maxReps = repsList.maxOrNull() ?: 0
        val totalVolume = repsList.zip(weightList).sumOf { (r, w) -> (r * w).toDouble() }.toFloat()

        TrendPoint(
            date = dates.getOrElse(index) { record.sessionId.toString() },
            maxWeight = maxWeight,
            maxReps = maxReps,
            totalVolume = totalVolume,
        )
    }
}

/** 计算进步对比 */
fun calculateProgress(
    currentRecords: List<ExerciseRecord>,
    previousRecords: List<ExerciseRecord>,
    exerciseNames: Map<String, String>,
): List<ProgressResult> {
    val results = mutableListOf<ProgressResult>()

    for (current in currentRecords) {
        val prev = previousRecords.find { it.exerciseId == current.exerciseId }
        val name = exerciseNames[current.exerciseId] ?: current.exerciseId

        val currMaxWeight = parseMaxWeight(current)
        val currMaxReps = parseMaxReps(current)
        val currVolume = parseTotalVolume(current)

        val prevMaxWeight = if (prev != null) parseMaxWeight(prev) else 0f
        val prevMaxReps = if (prev != null) parseMaxReps(prev) else 0
        val prevVolume = if (prev != null) parseTotalVolume(prev) else 0f

        results.add(
            ProgressResult(
                exerciseId = current.exerciseId,
                exerciseName = name,
                currentMaxWeight = currMaxWeight,
                previousMaxWeight = prevMaxWeight,
                currentMaxReps = currMaxReps,
                previousMaxReps = prevMaxReps,
                currentTotalVolume = currVolume,
                previousTotalVolume = prevVolume,
            )
        )
    }
    return results
}

private fun parseMaxWeight(record: ExerciseRecord): Float {
    return try {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val type = Types.newParameterizedType(List::class.java, Float::class.java)
        val list = moshi.adapter<List<Float>>(type).fromJson(record.weight) ?: emptyList()
        list.maxOrNull() ?: 0f
    } catch (_: Exception) { 0f }
}

private fun parseMaxReps(record: ExerciseRecord): Int {
    return try {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val type = Types.newParameterizedType(List::class.java, Int::class.java)
        val list = moshi.adapter<List<Int>>(type).fromJson(record.actualReps) ?: emptyList()
        list.maxOrNull() ?: 0
    } catch (_: Exception) { 0 }
}

private fun parseTotalVolume(record: ExerciseRecord): Float {
    return try {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val intType = Types.newParameterizedType(List::class.java, Int::class.java)
        val floatType = Types.newParameterizedType(List::class.java, Float::class.java)
        val repsList = moshi.adapter<List<Int>>(intType).fromJson(record.actualReps) ?: emptyList()
        val weightList = moshi.adapter<List<Float>>(floatType).fromJson(record.weight) ?: emptyList()
        repsList.zip(weightList).sumOf { (r, w) -> (r * w).toDouble() }.toFloat()
    } catch (_: Exception) { 0f }
}
