package com.gym.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 计划-动作关联表 (多对多)。
 * 存储一个计划中每个动作的目标组数和次数。
 */
@Entity(
    tableName = "workout_plan_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlan::class,
            parentColumns = ["plan_id"],
            childColumns = ["plan_id"],
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
        Index("plan_id"),
        Index("exercise_id"),
    ],
)
data class WorkoutPlanExercise(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "plan_id")
    val planId: Long,

    @ColumnInfo(name = "exercise_id")
    val exerciseId: String,

    /** 目标组数 */
    @ColumnInfo(name = "target_sets")
    val targetSets: Int,

    /** 目标每组次数 */
    @ColumnInfo(name = "target_reps")
    val targetReps: Int,

    /** 目标每组重量（kg）；0 表示计划未设定，训练时无上次数据则预填此值 */
    @ColumnInfo(name = "target_weight")
    val targetWeight: Float = 0f,

    /** 动作在计划中的排序 */
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
)
