package com.gym.app.data.importer

import android.content.Context
import com.gym.app.data.dao.ExerciseDao
import com.gym.app.data.entity.Exercise
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 负责从 assets/exercises.json 读取数据并批量导入 Room 数据库。
 * 仅在首次启动时调用一次。
 */
object ExerciseImporter {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    /**
     * 导入 exercises.json 到数据库。
     * @return 导入的动作数量
     */
    suspend fun importIfNeeded(context: Context, exerciseDao: ExerciseDao): Int =
        withContext(Dispatchers.IO) {
            val count = exerciseDao.getCount()
            if (count > 0) {
                return@withContext count // 已导入，跳过
            }

            val json = context.assets.open("exercises.json")
                .bufferedReader()
                .use { it.readText() }

            val listType = Types.newParameterizedType(
                List::class.java,
                ExerciseJson::class.java,
            )
            val adapter = moshi.adapter<List<ExerciseJson>>(listType)
            val exercisesJson: List<ExerciseJson> = adapter.fromJson(json) ?: emptyList()

            val entities = exercisesJson.map { it.toEntity() }
            exerciseDao.insertAll(entities)

            entities.size
        }
}

/**
 * 将 JSON 模型转换为 Room Entity。
 */
fun ExerciseJson.toEntity(): Exercise {
    // Moshi 序列化指令步骤为 JSON 数组字符串存储在 Room 中
    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    val listAdapter = moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))

    return Exercise(
        id = id,
        name = name,
        category = category,
        bodyPart = bodyPart,
        equipment = equipment,
        instructionsZh = instructions["zh"] ?: instructions["en"] ?: "",
        instructionStepsZh = listAdapter.toJson(instructionSteps["zh"] ?: instructionSteps["en"] ?: emptyList()),
        muscleGroup = muscleGroup,
        secondaryMuscles = listAdapter.toJson(secondaryMuscles),
        target = target,
        imagePath = image,
        gifPath = gifUrl,
    )
}
