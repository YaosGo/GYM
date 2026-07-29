package com.gym.app.data.importer

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * exercises.json 中单条数据的 Moshi 映射。
 * 字段名与 JSON 完全一致，仅映射我们需要的数据。
 */
@JsonClass(generateAdapter = true)
data class ExerciseJson(
    @Json(name = "id")
    val id: String,

    @Json(name = "name")
    val name: String,

    @Json(name = "category")
    val category: String,

    @Json(name = "body_part")
    val bodyPart: String,

    @Json(name = "equipment")
    val equipment: String,

    @Json(name = "instructions")
    val instructions: Map<String, String>,

    @Json(name = "instruction_steps")
    val instructionSteps: Map<String, List<String>>,

    @Json(name = "muscle_group")
    val muscleGroup: String,

    @Json(name = "secondary_muscles")
    val secondaryMuscles: List<String>,

    @Json(name = "target")
    val target: String,

    @Json(name = "image")
    val image: String,

    @Json(name = "gif_url")
    val gifUrl: String,
)

/**
 * Moshi 单例，用于解析 exercises.json。
 */
object JsonParser {

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    inline fun <reified T> fromJson(json: String): T? {
        return moshi.adapter(T::class.java).fromJson(json)
    }

    inline fun <reified T> toJson(obj: T): String {
        return moshi.adapter(T::class.java).toJson(obj)
    }
}
