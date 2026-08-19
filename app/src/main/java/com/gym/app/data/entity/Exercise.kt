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

    /** 中文名称（预翻译映射），可能为 null 则回退到英文 name */
    @ColumnInfo(name = "name_zh")
    val nameZh: String? = null,

    /** 是否为用户自定义动作（无 GIF/要点，gifPath 为空） */
    @ColumnInfo(name = "is_custom")
    val isCustom: Boolean = false,

    /** 重量模式：0=普通（记实际负重），1=助力（记助力值，实际=体重−助力），2=负重（记附加重，实际=体重+附加重） */
    @ColumnInfo(name = "weight_mode")
    val weightMode: Int = WEIGHT_MODE_NORMAL,
) {

    val isAssisted: Boolean get() = weightMode == WEIGHT_MODE_ASSISTED

    val isWeighted: Boolean get() = weightMode == WEIGHT_MODE_WEIGHTED

    /** 有效对抗重量：助力=体重−助力值，负重=体重+附加重，普通=原始重量。 */
    fun effectiveWeight(bodyWeightKg: Float, recordedWeight: Float): Float = when (weightMode) {
        WEIGHT_MODE_ASSISTED -> (bodyWeightKg - recordedWeight).coerceAtLeast(0f)
        WEIGHT_MODE_WEIGHTED -> bodyWeightKg + recordedWeight
        else -> recordedWeight
    }

    companion object {
        const val WEIGHT_MODE_NORMAL = 0
        const val WEIGHT_MODE_ASSISTED = 1
        const val WEIGHT_MODE_WEIGHTED = 2

        /** 配重助力器械动作清单（leverage machine 的 assisted 变体），导入/迁移时自动标记。 */
        val ASSISTED_EXERCISE_NAMES: Set<String> = setOf(
            "assisted chest dip (kneeling)",
            "assisted parallel close grip pull-up",
            "assisted pull-up",
            "assisted standing chin-up",
            "assisted standing pull-up",
            "assisted triceps dip (kneeling)",
            "assisted wide-grip chest dip (kneeling)",
            "lever assisted chin-up",
        )

        /** 体重基动作的负重变体清单（自重为对抗基础：引体/双杠臂屈伸/俯卧撑/双力臂），导入/迁移时自动标记。 */
        val WEIGHTED_EXERCISE_NAMES: Set<String> = setOf(
            "weighted bench dip",
            "weighted close grip chin-up on dip cage",
            "weighted drop push up",
            "weighted muscle up",
            "weighted muscle up (on bar)",
            "weighted one hand pull up",
            "weighted pull-up",
            "weighted straight bar dip",
            "weighted three bench dips",
            "weighted tricep dips",
            "weighted triceps dip on high parallel bars",
        )
    }
}
