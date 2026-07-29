package com.gym.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 健身计划，如"三分化-推日"、"四分化-腿日"。
 * 一个计划可包含多个训练动作，通过 [WorkoutPlanExercise] 关联。
 */
@Entity(tableName = "workout_plans")
data class WorkoutPlan(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "plan_id")
    val planId: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String = "",

    /** 创建日期 (epoch millis) */
    @ColumnInfo(name = "created_date")
    val createdDate: Long = System.currentTimeMillis(),

    /** 计划排序序号 */
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
)
