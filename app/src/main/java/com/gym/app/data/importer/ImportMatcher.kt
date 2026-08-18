package com.gym.app.data.importer

import com.gym.app.data.entity.Exercise
import com.gym.app.data.search.ExerciseSearch

/** 匹配结果：OCR 条目 + 动作库动作（null = 未匹配，导入时跳过）。 */
data class MatchedImportEntry(
    val rawName: String,
    val sets: List<ParsedSet>,
    val exercise: Exercise?,
    val score: Double,
)

/** 把解析出的动作名匹配到动作库（复用 ExerciseSearch 评分器）。 */
object ImportMatcher {

    fun match(
        entries: List<ParsedExerciseEntry>,
        allExercises: List<Exercise>,
    ): List<MatchedImportEntry> = entries.map { entry ->
        // 先整行匹配；失败则按括号/标点拆段逐段尝试（如「平板卧推（宽握）」）
        val candidates = buildList {
            if (entry.rawName.isNotBlank()) add(entry.rawName)
            addAll(entry.rawName.split(Regex("[^一-鿿A-Za-z]+")).filter { it.length >= 2 })
        }
        var best: Pair<Exercise, Double>? = null
        for (query in candidates) {
            val top = ExerciseSearch.searchWithScores(allExercises, query).firstOrNull() ?: continue
            if (best == null || top.second > best.second) best = top
        }
        if (best != null) {
            MatchedImportEntry(entry.rawName, entry.sets, best.first, best.second)
        } else {
            MatchedImportEntry(entry.rawName, entry.sets, null, 0.0)
        }
    }
}
