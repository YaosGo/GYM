package com.gym.app.data.importer

import java.time.LocalDate

/** 解析出的一组：重量（kg）与次数。 */
data class ParsedSet(
    val weight: Float,
    val reps: Int,
)

/** 解析出的一个动作条目：OCR 原文行 + 组数据。 */
data class ParsedExerciseEntry(
    val rawName: String,
    val sets: List<ParsedSet>,
)

/**
 * 训练记录截图 OCR 文本解析器（纯 Kotlin，可单测）。
 *
 * 输入：ML Kit 逐行识别结果（textBlocks → lines → text）。
 * 规则：
 * - 组数据三种形态：
 *   1. 乘号行：「40kg × 12」「22.5 kg x 10」「12 × 40kg」（次数在前时重量带 kg）
 *      「40×12」（双方无单位时默认 重量×次数）
 *   2. 表格行（无乘号）：「1 40kg 12」「40 kg 12」「40 12」
 *   3. 跨行合并：OCR 把重量与次数拆成两行（「40 kg」+「12」），重量行入队、次数行按序配对
 * - 含「组」字的行视为汇总行忽略（如「4组 × 12次」）
 * - 统计/日期等噪声行忽略
 * - 名称行：含 ≥2 个中文字符、或 ≥3 个字母且无数字 → 新动作条目，其后组数据归入该动作
 * - 名称行之前出现的组数据 → 归入未命名条目（rawName 为空，由 UI 提示用户）
 * - 没有任何组的条目最终丢弃
 */
object ImportParser {

    /** 统计噪声行：包含这些词即忽略。 */
    private val NOISE_WORDS = listOf(
        "总容量", "总重量", "容量", "总组数", "组数", "时长", "用时",
        "训练时间", "备注", "笔记", "日期", "体重",
    )

    private val SET_LINE = Regex(
        """(\d+(?:\.\d+)?)\s*(kg|公斤)?\s*[×xX*]\s*(\d+(?:\.\d+)?)\s*(kg|公斤|次)?"""
    )

    /** 表格行数字片段（含可选单位）。 */
    private val NUM_TOKEN = Regex("""(\d+(?:\.\d+)?)\s*(kg|公斤|次)?""")

    /** 纯重量行：「40 kg」「37.5kg」。 */
    private val WEIGHT_ONLY = Regex(
        """^\s*(\d+(?:\.\d+)?)\s*(?:kg|公斤)\s*$""",
        RegexOption.IGNORE_CASE,
    )

    /** 纯次数行：「12」「12次」。 */
    private val REPS_ONLY = Regex("""^\s*(\d{1,3})\s*次?\s*$""")

    private val DATE_LINE = Regex("""(20\d{2})\s*[-/年.]\s*(\d{1,2})\s*[-/月.]\s*(\d{1,2})""")
    private val DATE_SHORT = Regex("""(\d{1,2})月(\d{1,2})日""")

    fun parse(lines: List<String>): List<ParsedExerciseEntry> {
        val entries = mutableListOf<ParsedExerciseEntry>()
        var current = ParsedExerciseEntry(rawName = "", sets = emptyList())
        // 跨行合并队列：重量行先进队，次数行按顺序取队首配对
        val pendingWeights = mutableListOf<Float>()

        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty()) continue

            if (isNoiseLine(line)) {
                pendingWeights.clear()
                continue
            }

            val set = parseSetLine(line)
            if (set != null) {
                current = current.copy(sets = current.sets + set)
                pendingWeights.clear()
                continue
            }

            val weightOnly = WEIGHT_ONLY.find(line)
            if (weightOnly != null) {
                pendingWeights.add(weightOnly.groupValues[1].toFloat())
                continue
            }

            if (pendingWeights.isNotEmpty()) {
                val repsOnly = REPS_ONLY.find(line)
                if (repsOnly != null) {
                    val reps = repsOnly.groupValues[1].toInt()
                    current = current.copy(
                        sets = current.sets + ParsedSet(pendingWeights.removeAt(0), reps)
                    )
                    continue
                }
            }

            if (isNameLine(line)) {
                if (current.sets.isNotEmpty() || current.rawName.isNotEmpty()) {
                    entries.add(current)
                }
                current = ParsedExerciseEntry(rawName = line, sets = emptyList())
                pendingWeights.clear()
                continue
            }

            // 其余行（纯数字、符号等）忽略
            pendingWeights.clear()
        }
        entries.add(current)

        // 丢弃没有组的条目（名称识别成功但组数据识别失败）
        return entries.filter { it.sets.isNotEmpty() }
    }

    /** 解析组数据行（乘号格式 + 表格格式）；非组数据行返回 null。 */
    fun parseSetLine(line: String): ParsedSet? {
        // 「4组 × 12次」这类行是计划汇总而非实际组数据
        if (line.contains('组')) return null
        if (isNoiseLine(line)) return null
        // 统一小写，兼容 OCR 输出的 "40KG" / "40 Kg" 等变体
        val normalized = line.lowercase()

        val match = SET_LINE.find(normalized)
        if (match != null) {
            val first = match.groupValues[1].toFloat()
            val second = match.groupValues[3].toFloat()
            val secondUnit = match.groupValues[4]

            // 次数在前时重量必带 kg；否则默认 重量×次数
            val (weight, reps) = if (secondUnit == "kg" || secondUnit == "公斤") {
                second to first.toInt()
            } else {
                first to second.toInt()
            }
            if (weight <= 0f || reps <= 0) return null
            return ParsedSet(weight = weight, reps = reps)
        }

        // 无乘号的表格行：「1 40kg 12」「40 kg 12」「40 12」
        return parseTableLine(normalized)
    }

    /** 表格行：数字以空格/单位分隔、无乘号。 */
    private fun parseTableLine(line: String): ParsedSet? {
        val tokens = NUM_TOKEN.findAll(line).toList()
        if (tokens.size < 2) return null
        val nums = tokens.map { it.groupValues[1].toFloat() }
        val units = tokens.map { it.groupValues[2] }
        val kgIndex = units.indexOfFirst { it == "kg" || it == "公斤" }

        val weight: Float
        val reps: Int
        if (kgIndex >= 0) {
            // 重量带 kg：次数取其后第一个数字，没有则取前一个（「12 40kg」）
            weight = nums[kgIndex]
            val after = nums.getOrNull(kgIndex + 1)
            val before = nums.getOrNull(kgIndex - 1)
            reps = (after ?: before ?: return null).toInt()
        } else {
            // 无单位：≥3 个数字时跳过组号（第 2 个是重量），否则按 重量 次数 解读
            if (nums.size >= 3) {
                weight = nums[1]
                reps = nums.last().toInt()
            } else {
                weight = nums[0]
                reps = nums[1].toInt()
            }
            // 无单位时重量需 ≥2.5，避免把「2 12」这类残行误判为组
            if (weight < 2.5f) return null
        }
        if (reps !in 1..100) return null
        return ParsedSet(weight = weight, reps = reps)
    }

    /**
     * 解析训练日期：完整日期（2026-08-15 / 08月15日）优先，
     * 其次「今天/昨天」相对日期，都没有则回退 today。
     */
    fun parseDate(lines: List<String>, today: LocalDate): String {
        for (raw in lines) {
            val line = raw.trim()
            DATE_LINE.find(line)?.let { m ->
                val date = try {
                    LocalDate.of(
                        m.groupValues[1].toInt(),
                        m.groupValues[2].toInt(),
                        m.groupValues[3].toInt(),
                    )
                } catch (_: Exception) {
                    null
                }
                if (date != null) return date.toString()
            }
            if (line.contains("今天")) return today.toString()
            if (line.contains("昨天")) return today.minusDays(1).toString()
            DATE_SHORT.find(line)?.let { m ->
                val date = try {
                    LocalDate.of(today.year, m.groupValues[1].toInt(), m.groupValues[2].toInt())
                } catch (_: Exception) {
                    null
                }
                if (date != null) return date.toString()
            }
        }
        return today.toString()
    }

    private fun isNoiseLine(line: String): Boolean {
        if (NOISE_WORDS.any { line.contains(it) }) return true
        if (DATE_LINE.containsMatchIn(line) || DATE_SHORT.containsMatchIn(line)) return true
        if (line.contains("今天") || line.contains("昨天")) return true
        return false
    }

    private fun isNameLine(line: String): Boolean {
        if (line.length > 40) return false
        val cjk = line.count { it in '一'..'鿿' }
        if (cjk >= 2) return true
        // 英文名：≥3 个字母且不含数字（如 "Push Up"）
        return line.count { it.isLetter() } >= 3 && !line.any { it.isDigit() }
    }
}
