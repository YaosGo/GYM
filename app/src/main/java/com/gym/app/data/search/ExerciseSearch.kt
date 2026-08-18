package com.gym.app.data.search

import com.gym.app.data.entity.Exercise

/**
 * 动作搜索评分器：在内存中对动作库打分排序，替代 SQL LIKE 硬匹配。
 *
 * 打分规则（对每个候选动作的 name / name_zh 各算一次，取较高分）：
 * - 名称完全相同：100
 * - 连续子串包含：95
 * - 全字符覆盖 + 双字组重叠：80 ~ 95
 *   （容忍词序颠倒，如搜索"哑铃平板卧推"命中"平板哑铃卧推"）
 * - 未达全字符覆盖：过滤不展示（与旧逐字 AND 相同的严格度）
 *
 * 多词搜索（空格分隔）：每个词都必须达到展示阈值，取平均分排序。
 */
object ExerciseSearch {

    /** 展示阈值：低于此分的候选不返回。部分字符覆盖最多 60 分，永远不会展示。 */
    private const val MIN_SCORE = 80.0

    fun search(all: List<Exercise>, query: String): List<Exercise> =
        searchWithScores(all, query).map { it.first }

    /**
     * 带评分的搜索：返回 (动作, 得分) 列表，按得分降序。
     * 供图片导入等需要「最佳匹配 + 置信度」的场景使用。
     */
    fun searchWithScores(all: List<Exercise>, query: String): List<Pair<Exercise, Double>> {
        val tokens = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return all.sortedBy { it.name }.map { it to 0.0 }

        return all
            .mapNotNull { exercise ->
                val scores = tokens.map { token -> scoreToken(exercise, token) }
                if (scores.any { it < MIN_SCORE }) {
                    null
                } else {
                    exercise to scores.average()
                }
            }
            .sortedWith(
                compareByDescending<Pair<Exercise, Double>> { it.second }
                    .thenBy { it.first.name }
            )
    }

    /** 对单个搜索词评分：中文名与英文名各算一次，取较高分。 */
    fun scoreToken(exercise: Exercise, token: String): Double {
        val zhScore = exercise.nameZh?.let { scoreName(it.lowercase(), token) } ?: 0.0
        val enScore = scoreName(exercise.name.lowercase(), token)
        return maxOf(zhScore, enScore)
    }

    /** 对单个名称打分。 */
    private fun scoreName(target: String, token: String): Double {
        if (target == token) return 100.0
        if (target.contains(token)) return 95.0

        val coverage = charCoverage(target, token)
        if (coverage < token.length) {
            // 部分字符覆盖，得分必然低于阈值，仅用于未来放宽展示阈值时排序
            return coverage * 60.0 / token.length
        }

        // 全字符覆盖但顺序不同：按双字组重叠比例加分
        if (token.length == 1) return 80.0
        val tokenBigrams = bigrams(token)
        val targetBigrams = bigrams(target)
        val overlap = tokenBigrams.count { it in targetBigrams }.toDouble() / tokenBigrams.size
        return 80.0 + 15.0 * overlap
    }

    /** 多重集字符覆盖率：token 中有多少个字符能在 target 中找到（考虑重复次数）。 */
    private fun charCoverage(target: String, token: String): Int {
        val pool = target.groupingBy { it }.eachCount().toMutableMap()
        var matched = 0
        for (ch in token) {
            val remaining = pool[ch] ?: 0
            if (remaining > 0) {
                pool[ch] = remaining - 1
                matched++
            }
        }
        return matched
    }

    /** 相邻双字组（中文词序匹配核心），如 "哑铃平板卧推" → [哑铃, 铃平, 平板, 板卧, 卧推]。 */
    private fun bigrams(text: String): Set<String> =
        text.zipWithNext { a, b -> "$a$b" }.toSet()
}
