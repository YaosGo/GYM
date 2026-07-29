package com.gym.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 单次训练中每个动作的实际完成记录。
 * 关联到 [WorkoutSession] 和 [Exercise]。
 */
@Entity(
    tableName = "exercise_records",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSession::class,
            parentColumns = ["session_id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("session_id"),
        Index("exercise_id"),
    ],
)
data class ExerciseRecord(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "record_id")
    val recordId: Long = 0,

    @ColumnInfo(name = "session_id")
    val sessionId: Long,

    @ColumnInfo(name = "exercise_id")
    val exerciseId: String,

    /** 目标组数（从计划复制过来） */
    @ColumnInfo(name = "target_sets")
    val targetSets: Int,

    /** 目标每组次数（从计划复制过来） */
    @ColumnInfo(name = "target_reps")
    val targetReps: Int,

    /** 实际完成组数 */
    @ColumnInfo(name = "actual_sets")
    val actualSets: Int,

    /** 实际每组次数，JSON 数组如 "[10,10,8]" */
    @ColumnInfo(name = "actual_reps")
    val actualReps: String,

    /** 每组负重（kg），JSON 数组如 "[20,20,20]"，可选 */
    @ColumnInfo(name = "weight")
    val weight: String = "[]",

    /** 动作排序 */
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
)
