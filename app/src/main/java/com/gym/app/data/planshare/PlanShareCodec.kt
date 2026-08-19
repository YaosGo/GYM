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

    /**
     * 解码分享串；无效返回 null。
     * 宽容处理：无前缀的原始 Base64 也可解析（方便用户粘贴时被截断前缀）；
     * 聊天软件复制长文本可能折行或插入不可见字符，解码前统一清除；
     * 兼容旧版紧凑键名（a/b/c/d/e/f）分享串。
     */
    fun decode(raw: String): SharedPlan? {
        val trimmed = raw.trim()
        val payload = if (trimmed.startsWith(PREFIX)) trimmed.removePrefix(PREFIX) else trimmed
        // 清除所有空白（换行/空格）与常见不可见字符（零宽空格等）
        val cleaned = payload.filterNot { it.isWhitespace() || it in "​‌‍﻿" }
        if (cleaned.isBlank()) return null
        return try {
            val json = String(Base64.getUrlDecoder().decode(cleaned), Charsets.UTF_8)
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
