package com.gym.app.data.importer

import com.gym.app.data.entity.Exercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * ImportMatcher 单元测试：OCR 动作名 → 动作库匹配。
 */
class ImportMatcherTest {

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
        exercise("4", "barbell squat", "杠铃深蹲"),
    )

    private fun entry(name: String, sets: List<ParsedSet> = listOf(ParsedSet(40f, 12))) =
        ParsedExerciseEntry(rawName = name, sets = sets)

    @Test
    fun `完全匹配中文名`() {
        val result = ImportMatcher.match(listOf(entry("平板杠铃卧推")), library)
        assertEquals("1", result[0].exercise?.id)
    }

    @Test
    fun `词序颠倒也能匹配`() {
        val result = ImportMatcher.match(listOf(entry("哑铃平板卧推")), library)
        assertEquals("2", result[0].exercise?.id)
    }

    @Test
    fun `括号后缀拆段匹配`() {
        val result = ImportMatcher.match(listOf(entry("平板卧推（宽握）")), library)
        assertNotNull(result[0].exercise)
        assertEquals("1", result[0].exercise?.id)
    }

    @Test
    fun `垃圾行未匹配`() {
        val result = ImportMatcher.match(listOf(entry("¥%@# 乱码")), library)
        assertNull(result[0].exercise)
    }

    @Test
    fun `未命名条目未匹配`() {
        val result = ImportMatcher.match(listOf(entry("")), library)
        assertNull(result[0].exercise)
    }
}
