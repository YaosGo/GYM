package com.gym.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.gym.app.data.entity.WorkoutPlan
import com.gym.app.data.entity.WorkoutPlanExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutPlanDao {

    // --- Plan CRUD ---

    @Query("SELECT * FROM workout_plans ORDER BY sort_order ASC, created_date DESC")
    fun getAllPlans(): Flow<List<WorkoutPlan>>

    @Query("SELECT * FROM workout_plans WHERE plan_id = :planId")
    suspend fun getPlanById(planId: Long): WorkoutPlan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: WorkoutPlan): Long

    @Update
    suspend fun updatePlan(plan: WorkoutPlan)

    @Delete
    suspend fun deletePlan(plan: WorkoutPlan)

    // --- Plan Exercise CRUD ---

    @Query(
        """
        SELECT * FROM workout_plan_exercises
        WHERE plan_id = :planId
        ORDER BY sort_order ASC
        """
    )
    fun getPlanExercises(planId: Long): Flow<List<WorkoutPlanExercise>>

    @Query(
        """
        SELECT * FROM workout_plan_exercises
        WHERE plan_id = :planId
        ORDER BY sort_order ASC
        """
    )
    suspend fun getPlanExercisesOnce(planId: Long): List<WorkoutPlanExercise>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanExercise(exercise: WorkoutPlanExercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanExercises(exercises: List<WorkoutPlanExercise>)

    @Update
    suspend fun updatePlanExercise(exercise: WorkoutPlanExercise)

    @Delete
    suspend fun deletePlanExercise(exercise: WorkoutPlanExercise)

    @Query("DELETE FROM workout_plan_exercises WHERE plan_id = :planId")
    suspend fun deletePlanExercisesByPlanId(planId: Long)

    // --- Transaction: create plan with exercises ---

    @Transaction
    suspend fun createPlanWithExercises(plan: WorkoutPlan, exercises: List<WorkoutPlanExercise>): Long {
        val planId = insertPlan(plan)
        insertPlanExercises(exercises.map { it.copy(planId = planId) })
        return planId
    }
}
