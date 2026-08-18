package com.gym.app.data.search

import com.gym.app.data.entity.Exercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ExerciseSearch 评分器单元测试。
 */
class ExerciseSearchTest {

    private fun exercise(id: String, name: String, nameZh: String? = null) = Exercise(
        id = id,
        name = name,
        category = "",
        bodyPart = "",
        equipment = "",
        instructionsZh = "",
        instructionStepsZh = "",
        muscleGroup = "",
        secondaryMuscles = "",
        target = "",
        imagePath = "",
        gifPath = "",
        nameZh = nameZh,
    )

    private val library = listOf(
        exercise("1", "barbell bench press", "平板杠铃卧推"),
        exercise("2", "flat dumbbell bench press", "平板哑铃卧推"),
        exercise("3", "incline dumbbell bench press", "上斜哑铃卧推"),
        exercise("4", "push-up", "俯卧撑"),
        exercise("5", "dumbbell squat", "哑铃深蹲"),
        exercise("6", "barbell deadlift", "杠铃硬拉"),
    )

    private fun names(result: List<Exercise>): List<String> = result.map { it.id }

    @Test
    fun `词序颠倒也能命中且排第一`() {
        val result = ExerciseSearch.search(library, "哑铃平板卧推")
        assertTrue(names(result).contains("2"))
        assertEquals("2", names(result).first())
    }

    @Test
    fun `完全匹配排最前`() {
        val result = ExerciseSearch.search(library, "平板哑铃卧推")
        assertEquals("2", names(result).first())
    }

    @Test
    fun `连续子串命中`() {
        val result = ExerciseSearch.search(library, "哑铃卧推")
        assertTrue(names(result).contains("2"))
        assertTrue(names(result).contains("3"))
        // "哑铃卧推"是"平板哑铃卧推"的连续子串，得分高于仅字序颠倒的候选
        assertEquals("2", names(result).first())
    }

    @Test
    fun `未达全字符覆盖不展示`() {
        val result = ExerciseSearch.search(library, "哑铃平板卧推")
        // "上斜哑铃卧推"缺少"平""板"，不应出现在结果中
        assertFalse(names(result).contains("3"))
        // "俯卧撑"与查询无关
        assertFalse(names(result).contains("4"))
    }

    @Test
    fun `多词搜索每个词都必须命中`() {
        val result = ExerciseSearch.search(library, "哑铃 卧推")
        assertTrue(names(result).contains("2"))
        assertTrue(names(result).contains("3"))
        assertFalse(names(result).contains("5")) // 哑铃深蹲不含"卧推"
    }

    @Test
    fun `英文名搜索`() {
        val result = ExerciseSearch.search(library, "bench press")
        assertTrue(names(result).contains("1"))
        assertTrue(names(result).contains("2"))
        assertTrue(names(result).contains("3"))
        // 三个都是连续子串命中（95 分），同分时按名称序，"barbell..." 排最前
        assertEquals("1", names(result).first())
    }

    @Test
    fun `单字搜索命中所有包含该字的动作`() {
        val result = ExerciseSearch.search(library, "卧")
        assertTrue(names(result).contains("1"))
        assertTrue(names(result).contains("2"))
        assertTrue(names(result).contains("3"))
        assertTrue(names(result).contains("4"))
        assertFalse(names(result).contains("5"))
        assertFalse(names(result).contains("6"))
    }

    @Test
    fun `重复字符按多重集匹配`() {
        // "杠杠"需要两个"杠"，"平板杠铃卧推"只有一个
        val result = ExerciseSearch.search(library, "杠杠")
        assertFalse(names(result).contains("1"))
    }

    @Test
    fun `无匹配返回空列表`() {
        val result = ExerciseSearch.search(library, "哑铃深蹲硬拉")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `空查询返回全部`() {
        val result = ExerciseSearch.search(library, "  ")
        assertEquals(6, result.size)
    }
}
