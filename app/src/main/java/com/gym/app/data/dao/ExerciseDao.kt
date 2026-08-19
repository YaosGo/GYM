package com.gym.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gym.app.data.entity.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE body_part = :bodyPart ORDER BY name ASC")
    fun getExercisesByBodyPart(bodyPart: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE equipment = :equipment ORDER BY name ASC")
    fun getExercisesByEquipment(equipment: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' OR name_zh LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchExercises(query: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: String): Exercise?

    @Query("SELECT DISTINCT body_part FROM exercises WHERE body_part != '' ORDER BY body_part ASC")
    fun getAllBodyParts(): Flow<List<String>>

    @Query("SELECT DISTINCT equipment FROM exercises WHERE equipment != '' ORDER BY equipment ASC")
    fun getAllEquipments(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<Exercise>)

    /** 插入单个自定义动作。 */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExercise(exercise: Exercise)

    /** 更新动作的重量模式（助力/负重手动标记）。 */
    @Query("UPDATE exercises SET weight_mode = :weightMode WHERE id = :id")
    suspend fun updateWeightMode(id: String, weightMode: Int)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getCount(): Int
}
