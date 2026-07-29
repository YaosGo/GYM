package com.gym.app.data.repository

import com.gym.app.data.dao.ExerciseDao
import com.gym.app.data.entity.Exercise
import kotlinx.coroutines.flow.Flow

class ExerciseRepository(private val exerciseDao: ExerciseDao) {

    fun getAllExercises(): Flow<List<Exercise>> = exerciseDao.getAllExercises()

    fun getExercisesByBodyPart(bodyPart: String): Flow<List<Exercise>> =
        exerciseDao.getExercisesByBodyPart(bodyPart)

    fun getExercisesByEquipment(equipment: String): Flow<List<Exercise>> =
        exerciseDao.getExercisesByEquipment(equipment)

    fun searchExercises(query: String): Flow<List<Exercise>> =
        exerciseDao.searchExercises(query)

    /**
     * 中文模糊搜索：支持单字匹配和多词组合。
     * 示例："卧推" → 匹配所有含"卧"和"推"的动作；"哑铃 胸" → 匹配哑铃+胸部动作。
     */
    fun fuzzySearchExercises(query: String): Flow<List<Exercise>> {
        val sqlQuery = ExerciseDao.buildFuzzySearchQuery(query)
        return exerciseDao.fuzzySearchExercises(sqlQuery)
    }

    suspend fun getExerciseById(id: String): Exercise? = exerciseDao.getExerciseById(id)

    fun getAllBodyParts(): Flow<List<String>> = exerciseDao.getAllBodyParts()

    fun getAllEquipments(): Flow<List<String>> = exerciseDao.getAllEquipments()

    suspend fun getCount(): Int = exerciseDao.getCount()
}
