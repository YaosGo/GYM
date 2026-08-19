package com.gym.app.data.repository

import com.gym.app.data.dao.ExerciseDao
import com.gym.app.data.entity.Exercise
import com.gym.app.data.search.ExerciseSearch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExerciseRepository(private val exerciseDao: ExerciseDao) {

    fun getAllExercises(): Flow<List<Exercise>> = exerciseDao.getAllExercises()

    fun getExercisesByBodyPart(bodyPart: String): Flow<List<Exercise>> =
        exerciseDao.getExercisesByBodyPart(bodyPart)

    fun getExercisesByEquipment(equipment: String): Flow<List<Exercise>> =
        exerciseDao.getExercisesByEquipment(equipment)

    fun searchExercises(query: String): Flow<List<Exercise>> =
        exerciseDao.searchExercises(query)

    /**
     * 中文模糊搜索：内存评分排序。
     * 支持字序颠倒（"哑铃平板卧推"命中"平板哑铃卧推"）、多词组合、英文名。
     * 结果按匹配度从高到低排序。
     */
    fun fuzzySearchExercises(query: String): Flow<List<Exercise>> =
        exerciseDao.getAllExercises().map { all -> ExerciseSearch.search(all, query) }

    suspend fun getExerciseById(id: String): Exercise? = exerciseDao.getExerciseById(id)

    fun getAllBodyParts(): Flow<List<String>> = exerciseDao.getAllBodyParts()

    fun getAllEquipments(): Flow<List<String>> = exerciseDao.getAllEquipments()

    suspend fun getCount(): Int = exerciseDao.getCount()

    /** 更新动作的重量模式（助力/负重手动标记）。 */
    suspend fun updateWeightMode(id: String, weightMode: Int) =
        exerciseDao.updateWeightMode(id, weightMode)

    /**
     * 创建自定义动作（无 GIF/要点），写入动作库后可在计划/训练中复用。
     * id 以 custom_ 前缀避免与数据集动作（exercises.json）冲突；
     * 连续快速创建时时间戳可能同毫秒，循环保证 id 唯一（批量导入场景）。
     */
    suspend fun createCustomExercise(name: String): Exercise {
        val trimmed = name.trim()
        val exercise = Exercise(
            id = uniqueCustomId(),
            name = trimmed,
            category = "",
            bodyPart = "",
            equipment = "",
            instructionsZh = "",
            instructionStepsZh = "[]",
            muscleGroup = "",
            secondaryMuscles = "[]",
            target = "",
            imagePath = "",
            gifPath = "",
            nameZh = trimmed,
            isCustom = true,
        )
        exerciseDao.insertExercise(exercise)
        return exercise
    }

    private suspend fun uniqueCustomId(): String {
        var id: String
        do {
            id = "custom_" + System.currentTimeMillis()
        } while (exerciseDao.getExerciseById(id) != null)
        return id
    }
}
