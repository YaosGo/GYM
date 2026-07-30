package com.gym.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.gym.app.data.entity.ExerciseRecord
import com.gym.app.data.entity.WorkoutSession
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {

    // --- Session CRUD ---

    @Query("SELECT * FROM workout_sessions ORDER BY date DESC, completed_at DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE plan_id = :planId ORDER BY date DESC")
    fun getSessionsByPlanId(planId: Long): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE session_id = :sessionId")
    suspend fun getSessionById(sessionId: Long): WorkoutSession?

    /** 获取指定计划的最近一次训练记录（用于显示 "Previous" 数据） */
    @Query(
        """
        SELECT * FROM workout_sessions
        WHERE plan_id = :planId
        ORDER BY date DESC, completed_at DESC
        LIMIT 1
        """
    )
    suspend fun getLatestSession(planId: Long): WorkoutSession?

    @Insert
    suspend fun insertSession(session: WorkoutSession): Long

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE date = :date")
    suspend fun getSessionCountByDate(date: String): Int

    /** 获取所有有训练记录的日期（用于日历标记） */
    @Query("SELECT DISTINCT date FROM workout_sessions ORDER BY date ASC")
    suspend fun getAllWorkoutDates(): List<String>

    /** 获取指定日期的所有训练记录 */
    @Query("SELECT * FROM workout_sessions WHERE date = :date ORDER BY completed_at DESC")
    suspend fun getSessionsByDate(date: String): List<WorkoutSession>

    /** 获取指定月份的训练日期 */
    @Query("SELECT date FROM workout_sessions WHERE date LIKE :yearMonth || '%' ORDER BY date ASC")
    suspend fun getWorkoutDatesByMonth(yearMonth: String): List<String>

    /** 总训练次数 */
    @Query("SELECT COUNT(*) FROM workout_sessions")
    suspend fun getTotalWorkoutCount(): Int

    /** 本月训练次数 */
    @Query("SELECT COUNT(DISTINCT date) FROM workout_sessions WHERE date LIKE :yearMonth || '%'")
    suspend fun getMonthlyWorkoutCount(yearMonth: String): Int

    // --- Exercise Record CRUD ---

    @Query(
        """
        SELECT * FROM exercise_records
        WHERE session_id = :sessionId
        ORDER BY sort_order ASC
        """
    )
    suspend fun getRecordsBySessionId(sessionId: Long): List<ExerciseRecord>

    /** 获取某个动作在指定计划下的所有历史记录（用于计算最佳组和趋势图） */
    @Query(
        """
        SELECT er.* FROM exercise_records er
        INNER JOIN workout_sessions ws ON er.session_id = ws.session_id
        WHERE ws.plan_id = :planId AND er.exercise_id = :exerciseId
        ORDER BY ws.date ASC
        """
    )
    suspend fun getRecordsForExerciseHistory(planId: Long, exerciseId: String): List<ExerciseRecord>

    /** 获取上次训练中指定动作的记录 */
    @Query(
        """
        SELECT er.* FROM exercise_records er
        INNER JOIN workout_sessions ws ON er.session_id = ws.session_id
        WHERE ws.plan_id = :planId
          AND er.exercise_id = :exerciseId
          AND ws.session_id = (
              SELECT session_id FROM workout_sessions
              WHERE plan_id = :planId
              ORDER BY date DESC, completed_at DESC
              LIMIT 1
          )
        """
    )
    suspend fun getLatestRecordForExercise(planId: Long, exerciseId: String): ExerciseRecord?

    @Insert
    suspend fun insertRecord(record: ExerciseRecord): Long

    @Insert
    suspend fun insertRecords(records: List<ExerciseRecord>)

    // --- Transaction: save session with records ---

    @Transaction
    suspend fun saveSessionWithRecords(
        session: WorkoutSession,
        records: List<ExerciseRecord>,
    ): Long {
        val sessionId = insertSession(session)
        insertRecords(records.map { it.copy(sessionId = sessionId) })
        return sessionId
    }
}
