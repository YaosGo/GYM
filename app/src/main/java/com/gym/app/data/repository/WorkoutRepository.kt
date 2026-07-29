package com.gym.app.data.repository

import com.gym.app.data.dao.WorkoutPlanDao
import com.gym.app.data.dao.WorkoutSessionDao
import com.gym.app.data.entity.ExerciseRecord
import com.gym.app.data.entity.WorkoutPlan
import com.gym.app.data.entity.WorkoutPlanExercise
import com.gym.app.data.entity.WorkoutSession
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(
    private val planDao: WorkoutPlanDao,
    private val sessionDao: WorkoutSessionDao,
) {

    // --- Plans ---

    fun getAllPlans(): Flow<List<WorkoutPlan>> = planDao.getAllPlans()

    suspend fun getPlanById(planId: Long): WorkoutPlan? = planDao.getPlanById(planId)

    suspend fun createPlanWithExercises(
        plan: WorkoutPlan,
        exercises: List<WorkoutPlanExercise>,
    ): Long = planDao.createPlanWithExercises(plan, exercises)

    suspend fun updatePlan(plan: WorkoutPlan) = planDao.updatePlan(plan)

    suspend fun deletePlan(plan: WorkoutPlan) = planDao.deletePlan(plan)

    fun getPlanExercises(planId: Long): Flow<List<WorkoutPlanExercise>> =
        planDao.getPlanExercises(planId)

    suspend fun getPlanExercisesOnce(planId: Long): List<WorkoutPlanExercise> =
        planDao.getPlanExercisesOnce(planId)

    suspend fun deletePlanExercises(planId: Long) =
        planDao.deletePlanExercisesByPlanId(planId)

    suspend fun insertPlanExercises(exercises: List<WorkoutPlanExercise>) =
        planDao.insertPlanExercises(exercises)

    // --- Sessions ---

    fun getAllSessions(): Flow<List<WorkoutSession>> = sessionDao.getAllSessions()

    fun getSessionsByPlanId(planId: Long): Flow<List<WorkoutSession>> =
        sessionDao.getSessionsByPlanId(planId)

    suspend fun getSessionById(sessionId: Long): WorkoutSession? =
        sessionDao.getSessionById(sessionId)

    suspend fun getLatestSession(planId: Long): WorkoutSession? =
        sessionDao.getLatestSession(planId)

    suspend fun saveSessionWithRecords(
        session: WorkoutSession,
        records: List<ExerciseRecord>,
    ): Long = sessionDao.saveSessionWithRecords(session, records)

    suspend fun getRecordsBySessionId(sessionId: Long): List<ExerciseRecord> =
        sessionDao.getRecordsBySessionId(sessionId)

    suspend fun getRecordsForExerciseHistory(
        planId: Long,
        exerciseId: String,
    ): List<ExerciseRecord> = sessionDao.getRecordsForExerciseHistory(planId, exerciseId)

    suspend fun getLatestRecordForExercise(
        planId: Long,
        exerciseId: String,
    ): ExerciseRecord? = sessionDao.getLatestRecordForExercise(planId, exerciseId)

    suspend fun getSessionCountByDate(date: String): Int =
        sessionDao.getSessionCountByDate(date)

    suspend fun getAllWorkoutDates(): List<String> =
        sessionDao.getAllWorkoutDates()

    suspend fun getSessionsByDate(date: String): List<WorkoutSession> =
        sessionDao.getSessionsByDate(date)

    suspend fun getWorkoutDatesByMonth(yearMonth: String): List<String> =
        sessionDao.getWorkoutDatesByMonth(yearMonth)

    suspend fun getTotalWorkoutCount(): Int =
        sessionDao.getTotalWorkoutCount()

    suspend fun getMonthlyWorkoutCount(yearMonth: String): Int =
        sessionDao.getMonthlyWorkoutCount(yearMonth)
}
