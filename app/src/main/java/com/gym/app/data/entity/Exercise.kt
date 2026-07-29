package com.gym.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 健身动作实体，数据从 exercises.json 导入。
 * 字段对应 JSON 数据集结构，UI 默认展示中文 (zh) 内容。
 */
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "body_part")
    val bodyPart: String,

    @ColumnInfo(name = "equipment")
    val equipment: String,

    /** 中文完整指导说明 */
    @ColumnInfo(name = "instructions_zh")
    val instructionsZh: String,

    /** 中文分步指导，JSON 数组字符串 */
    @ColumnInfo(name = "instruction_steps_zh")
    val instructionStepsZh: String,

    @ColumnInfo(name = "muscle_group")
    val muscleGroup: String,

    /** 次要肌群，JSON 数组字符串 */
    @ColumnInfo(name = "secondary_muscles")
    val secondaryMuscles: String,

    @ColumnInfo(name = "target")
    val target: String,

    /** 静态缩略图路径 (images/XXXX-xxxxx.jpg) */
    @ColumnInfo(name = "image_path")
    val imagePath: String,

    /** GIF 动图路径 (videos/XXXX-xxxxx.gif) */
    @ColumnInfo(name = "gif_path")
    val gifPath: String,
)
