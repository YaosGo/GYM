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
}
