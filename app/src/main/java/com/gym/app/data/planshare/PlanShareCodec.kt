package com.gym.app.data.planshare

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.Base64

/**
 * 计划分享串编解码。
 *
 * 格式：`GYM1:<Base64URL(JSON)>`
 * JSON 内容：版本号 v、计划名 name、动作列表 exercises（id / 名称 / 组数 / 次数 / 重量标记）。
 * 动作 id 为动作库全局统一 id（如 0001-2gPfomN），接收方安装同一动作库即可精确还原；
 * 自定义动作（custom=true）接收方没有，由导入方自动创建同名自定义动作。
 */
object PlanShareCodec {

    const val PREFIX = "GYM1:"
    const val CURRENT_VERSION = 1

    /** 分享串中的单个动作。 */
    data class SharedExercise(
        val id: String,
        val name: String,
        val custom: Boolean,
        val sets: Int,
        val reps: Int,
        val weightMode: Int = 0,
    )

    /** 分享串中的计划。 */
    data class SharedPlan(
        val v: Int = CURRENT_VERSION,
        val name: String,
        val exercises: List<SharedExercise>,
    )

    /**
     * 旧版紧凑键名（a/b/c/d/e/f）分享串模型，仅用于向后兼容解码。
     * 早期版本的分享串用单字母键名缩短长度，字段语义与 SharedPlan/SharedExercise 一一对应。
     */
    private data class LegacyPlan(
        @Json(name = "a") val v: Int = CURRENT_VERSION,
        @Json(name = "b") val name: String = "",
        @Json(name = "c") val exercises: List<LegacyExercise> = emptyList(),
    )

    private data class LegacyExercise(
        @Json(name = "a") val id: String = "",
        @Json(name = "b") val name: String = "",
        @Json(name = "c") val custom: Boolean = false,
        @Json(name = "d") val sets: Int = 0,
        @Json(name = "e") val reps: Int = 0,
        @Json(name = "f") val weightMode: Int = 0,
    )

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(SharedPlan::class.java)
    private val legacyAdapter = moshi.adapter(LegacyPlan::class.java)

    /** 编码为分享串，如 GYM1:eyJ2IjoxLCJuYW1lIjoi... */
    fun encode(plan: SharedPlan): String {
        val json = adapter.toJson(plan)
        val b64 = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(json.toByteArray(Charsets.UTF_8))
        return PREFIX + b64
    }

    /** 最小有效载荷长度：最短的合法分享串 JSON 编码后远长于此，用于过滤随机文本。 */
    private const val MIN_PAYLOAD_LENGTH = 50

    /** 常见的不可见字符与窄空格：isWhitespace() 不覆盖 NBSP 等，需要显式过滤。 */
    private const val INVISIBLE_CHARS = "​‌‍﻿ "

    /**
     * 从任意粘贴文本中提取完整分享串（`GYM1:` 前缀 + Base64URL 主体）。
     * 容忍前后有多余文字（聊天软件复制时常带标签或换行）；找不到返回 null。
     */
    fun extractShareString(text: String): String? {
        val cleaned = text.filterNot { it.isWhitespace() || it in INVISIBLE_CHARS }
        val prefixIndex = cleaned.lastIndexOf(PREFIX)
        if (prefixIndex >= 0) {
            val payload = base64RunFrom(cleaned, prefixIndex + PREFIX.length)
            return if (payload.length >= MIN_PAYLOAD_LENGTH) PREFIX + payload else null
        }
        // 无前缀：找全文中最长的 Base64URL 连续段
        val best = longestBase64Run(cleaned)
        return if (best != null) PREFIX + best else null
    }

    /**
     * 解码分享串；无效返回 null。
     * 宽容处理：无前缀的原始 Base64 也可解析（方便用户粘贴时被截断前缀）；
     * 聊天软件复制长文本可能折行、插入不可见字符或带前后文字，解码前统一清除；
     * 兼容旧版紧凑键名（a/b/c/d/e/f）分享串。
     */
    fun decode(raw: String): SharedPlan? {
        val cleaned = raw.filterNot { it.isWhitespace() || it in INVISIBLE_CHARS }
        val payload = extractPayload(cleaned) ?: return null
        return try {
            val json = String(Base64.getUrlDecoder().decode(payload), Charsets.UTF_8)
            val plan = try {
                adapter.fromJson(json)
            } catch (_: Exception) {
                null
            } ?: legacyAdapter.fromJson(json)?.toSharedPlan()
            if (plan == null || !isValid(plan)) null else plan
        } catch (_: Exception) {
            null
        }
    }

    /** 从已清理文本中提取 Base64URL 主体：优先取 GYM1: 前缀之后的连续段。 */
    private fun extractPayload(cleaned: String): String? {
        val prefixIndex = cleaned.lastIndexOf(PREFIX)
        val payload = if (prefixIndex >= 0) {
            base64RunFrom(cleaned, prefixIndex + PREFIX.length)
        } else {
            longestBase64Run(cleaned) ?: ""
        }
        return if (payload.length >= MIN_PAYLOAD_LENGTH) payload else null
    }

    /** 从 [start] 起截取连续 Base64URL 字符段。 */
    private fun base64RunFrom(text: String, start: Int): String {
        var end = start
        while (end < text.length && isBase64UrlChar(text[end])) end++
        return text.substring(start, end)
    }

    /** 全文中最长的 Base64URL 连续段（无前缀时兜底）；无有效段返回 null。 */
    private fun longestBase64Run(text: String): String? {
        var best: String? = null
        var i = 0
        while (i < text.length) {
            if (isBase64UrlChar(text[i])) {
                val run = base64RunFrom(text, i)
                if (best == null || run.length > best.length) best = run
                i += run.length
            } else {
                i++
            }
        }
        return best
    }

    private fun isBase64UrlChar(c: Char): Boolean =
        c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' || c == '-' || c == '_' || c == '='

    private fun isValid(plan: SharedPlan): Boolean =
        plan.v == CURRENT_VERSION &&
            plan.name.isNotBlank() &&
            plan.exercises.isNotEmpty() &&
            plan.exercises.none {
                it.id.isBlank() || it.name.isBlank() ||
                    it.sets !in 1..99 || it.reps !in 1..99 || it.weightMode !in 0..2
            }

    private fun LegacyPlan.toSharedPlan(): SharedPlan = SharedPlan(
        v = v,
        name = name,
        exercises = exercises.map { e ->
            SharedExercise(
                id = e.id,
                name = e.name,
                custom = e.custom,
                sets = e.sets,
                reps = e.reps,
                weightMode = e.weightMode,
            )
        },
    )
}
