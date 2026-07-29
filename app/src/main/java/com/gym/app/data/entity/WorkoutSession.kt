package com.gym.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一次训练记录（session）。
 * 关联到某个健身计划，记录训练日期和总时长。
 */
@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlan::class,
            parentColumns = ["plan_id"],
            childColumns = ["plan_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("plan_id"),
        Index("date"),
    ],
)
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "session_id")
    val sessionId: Long = 0,

    @ColumnInfo(name = "plan_id")
    val planId: Long,

    /** 训练日期，格式 yyyy-MM-dd */
    @ColumnInfo(name = "date")
    val date: String,

    /** 训练总时长（秒） */
    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Int = 0,

    /** 完成时间戳 (epoch millis) */
    @ColumnInfo(name = "completed_at")
    val completedAt: Long = System.currentTimeMillis(),

    /** 训练备注 */
    @ColumnInfo(name = "note")
    val note: String = "",
)
