package com.gym.app.data.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * ImportParser 单元测试：模拟旧训练记录 App 截图的 OCR 行。
 */
class ImportParserTest {

    private val today = LocalDate.of(2026, 8, 18)

    // --- 组数据行 ---

    @Test
    fun `组行 重量kg乘次数`() {
        val set = ImportParser.parseSetLine("40kg × 12")
        assertEquals(40f, set?.weight ?: -1f)
        assertEquals(12, set?.reps ?: -1)
    }

    @Test
    fun `组行 小数重量`() {
        val set = ImportParser.parseSetLine("22.5 kg × 10")
        assertEquals(22.5f, set?.weight ?: -1f)
        assertEquals(10, set?.reps ?: -1)
    }

    @Test
    fun `组行 乘号变体与大小写`() {
        assertEquals(40f, ImportParser.parseSetLine("40kg x 12")?.weight ?: -1f)
        assertEquals(12, ImportParser.parseSetLine("40KG*12")?.reps ?: -1)
        assertEquals(40f, ImportParser.parseSetLine("40 X 12")?.weight ?: -1f)
    }

    @Test
    fun `组行 无单位默认重量在前`() {
        val set = ImportParser.parseSetLine("40 × 12")
        assertEquals(40f, set?.weight ?: -1f)
        assertEquals(12, set?.reps ?: -1)
    }

    @Test
    fun `组行 次数在前重量带kg`() {
        val set = ImportParser.parseSetLine("12 × 40kg")
        assertEquals(40f, set?.weight ?: -1f)
        assertEquals(12, set?.reps ?: -1)
    }

    @Test
    fun `组行 次数单位`() {
        val set = ImportParser.parseSetLine("40kg × 12次")
        assertEquals(40f, set?.weight ?: -1f)
        assertEquals(12, set?.reps ?: -1)
    }

    @Test
    fun `组行 前缀组号不影响解析`() {
        val set = ImportParser.parseSetLine("1. 40kg × 12")
        assertEquals(40f, set?.weight ?: -1f)
        assertEquals(12, set?.reps ?: -1)
    }

    @Test
    fun `汇总行 含组字不当作组数据`() {
        assertNull(ImportParser.parseSetLine("4组 × 12次"))
        assertNull(ImportParser.parseSetLine("共 4 组"))
    }

    // --- 表格行（无乘号） ---

    @Test
    fun `表格行 组号重量次数`() {
        val set = ImportParser.parseSetLine("1 40kg 12")
        assertEquals(40f, set?.weight ?: -1f)
        assertEquals(12, set?.reps ?: -1)
    }

    @Test
    fun `表格行 重量kg分隔`() {
        val set = ImportParser.parseSetLine("40 kg 12")
        assertEquals(40f, set?.weight ?: -1f)
        assertEquals(12, set?.reps ?: -1)
    }

    @Test
    fun `表格行 双列无单位`() {
        val set = ImportParser.parseSetLine("40 12")
        assertEquals(40f, set?.weight ?: -1f)
        assertEquals(12, set?.reps ?: -1)
    }

    @Test
    fun `表格行 无单位小数字不误判`() {
        // 「2 12」更像是残行而非 2kg 的组
        assertNull(ImportParser.parseSetLine("2 12"))
    }

    @Test
    fun `表格行 日期不误判为组`() {
        assertNull(ImportParser.parseSetLine("2026-08-15"))
    }

    // --- 跨行合并（重量与次数被 OCR 拆成两行） ---

    @Test
    fun `跨行 重量与次数分列合并`() {
        val entries = ImportParser.parse(
            listOf("平板杠铃卧推", "40 kg", "12", "40 kg", "10")
        )
        assertEquals(1, entries.size)
        assertEquals(2, entries[0].sets.size)
        assertEquals(40f, entries[0].sets[0].weight)
        assertEquals(12, entries[0].sets[0].reps)
        assertEquals(10, entries[0].sets[1].reps)
    }

    @Test
    fun `跨行 连续重量列与次数列按序配对`() {
        val entries = ImportParser.parse(
            listOf("平板杠铃卧推", "40 kg", "37.5 kg", "12", "10")
        )
        assertEquals(1, entries.size)
        assertEquals(2, entries[0].sets.size)
        assertEquals(40f, entries[0].sets[0].weight)
        assertEquals(12, entries[0].sets[0].reps)
        assertEquals(37.5f, entries[0].sets[1].weight)
        assertEquals(10, entries[0].sets[1].reps)
    }

    @Test
    fun `跨行 未跟次数的重量行丢弃`() {
        val entries = ImportParser.parse(
            listOf("平板杠铃卧推", "40 kg", "杠铃深蹲", "20kg × 10")
        )
        assertEquals(1, entries.size)
        assertEquals("杠铃深蹲", entries[0].rawName)
    }

    // --- 条目分组 ---

    @Test
    fun `动作名与组数据正确分组`() {
        val lines = listOf(
            "平板杠铃卧推",
            "40kg × 12",
            "40kg × 10",
            "上斜哑铃卧推",
            "22.5 kg × 10",
        )
        val entries = ImportParser.parse(lines)
        assertEquals(2, entries.size)
        assertEquals("平板杠铃卧推", entries[0].rawName)
        assertEquals(2, entries[0].sets.size)
        assertEquals("上斜哑铃卧推", entries[1].rawName)
        assertEquals(1, entries[1].sets.size)
        assertEquals(22.5f, entries[1].sets[0].weight)
    }

    @Test
    fun `噪声行被忽略`() {
        val lines = listOf(
            "总容量 1,234 kg",
            "时长 45 分钟",
            "2026-08-15",
            "平板杠铃卧推",
            "40kg × 12",
        )
        val entries = ImportParser.parse(lines)
        assertEquals(1, entries.size)
        assertEquals("平板杠铃卧推", entries[0].rawName)
    }

    @Test
    fun `没有组的动作条目被丢弃`() {
        val lines = listOf(
            "杠铃深蹲",
            "总容量 100 kg",
            "平板杠铃卧推",
            "40kg × 12",
        )
        val entries = ImportParser.parse(lines)
        assertEquals(1, entries.size)
        assertEquals("平板杠铃卧推", entries[0].rawName)
    }

    @Test
    fun `名称行之前的组数据归入未命名条目`() {
        val lines = listOf("40kg × 12", "40kg × 10")
        val entries = ImportParser.parse(lines)
        assertEquals(1, entries.size)
        assertEquals("", entries[0].rawName)
        assertEquals(2, entries[0].sets.size)
    }

    @Test
    fun `纯数字行忽略`() {
        assertTrue(ImportParser.parse(listOf("1234", "5.5")).isEmpty())
    }

    // --- 日期解析 ---

    @Test
    fun `日期 完整日期`() {
        assertEquals("2026-08-15", ImportParser.parseDate(listOf("2026-08-15"), today))
    }

    @Test
    fun `日期 短日期补当年`() {
        assertEquals("2026-08-15", ImportParser.parseDate(listOf("08月15日"), today))
    }

    @Test
    fun `日期 今天与昨天`() {
        assertEquals("2026-08-18", ImportParser.parseDate(listOf("今天 胸部训练"), today))
        assertEquals("2026-08-17", ImportParser.parseDate(listOf("昨天"), today))
    }

    @Test
    fun `日期 未找到回退今天`() {
        assertEquals("2026-08-18", ImportParser.parseDate(listOf("总容量 100 kg"), today))
    }
}
