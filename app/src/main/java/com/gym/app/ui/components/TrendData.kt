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
    val sets: Int = 0,      // 总组数
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

/**
 * 解析记录的有效对抗重量列表（与组一一对应）。
 * 助力=体重−助力值，负重=体重+附加重；无有效重量数据时回退到原始 weight（兼容旧记录）。
 */
fun parseEffectiveWeights(record: ExerciseRecord): List<Float> {
    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    val type = Types.newParameterizedType(List::class.java, Float::class.javaObjectType)
    val effective = try {
        moshi.adapter<List<Float>>(type).fromJson(record.effectiveWeight) ?: emptyList()
    } catch (_: Exception) { emptyList() }
    if (effective.isNotEmpty()) return effective
    return try {
        moshi.adapter<List<Float>>(type).fromJson(record.weight) ?: emptyList()
    } catch (_: Exception) { emptyList() }
}

/** 从 ExerciseRecord 列表提取趋势数据点 */
fun extractTrendPoints(records: List<ExerciseRecord>, dates: List<String>): List<TrendPoint> {
    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    val intListType = Types.newParameterizedType(List::class.java, Int::class.javaObjectType)

    return records.mapIndexed { index, record ->
        val repsList: List<Int> = try {
            moshi.adapter<List<Int>>(intListType).fromJson(record.actualReps) ?: emptyList()
        } catch (_: Exception) { emptyList() }
        val weightList = parseEffectiveWeights(record)

        val maxWeight = weightList.maxOrNull() ?: 0f
        val maxReps = repsList.maxOrNull() ?: 0
        val totalVolume = repsList.zip(weightList).sumOf { (r, w) -> (r * w).toDouble() }.toFloat()
        val sets = record.actualSets

        TrendPoint(
            date = dates.getOrElse(index) { record.sessionId.toString() },
            maxWeight = maxWeight,
            maxReps = maxReps,
            totalVolume = totalVolume,
            sets = sets,
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
    return parseEffectiveWeights(record).maxOrNull() ?: 0f
}

private fun parseMaxReps(record: ExerciseRecord): Int {
    return try {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val type = Types.newParameterizedType(List::class.java, Int::class.javaObjectType)
        val list = moshi.adapter<List<Int>>(type).fromJson(record.actualReps) ?: emptyList()
        list.maxOrNull() ?: 0
    } catch (_: Exception) { 0 }
}

private fun parseTotalVolume(record: ExerciseRecord): Float {
    return try {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val intType = Types.newParameterizedType(List::class.java, Int::class.javaObjectType)
        val repsList = moshi.adapter<List<Int>>(intType).fromJson(record.actualReps) ?: emptyList()
        val weightList = parseEffectiveWeights(record)
        repsList.zip(weightList).sumOf { (r, w) -> (r * w).toDouble() }.toFloat()
    } catch (_: Exception) { 0f }
}
