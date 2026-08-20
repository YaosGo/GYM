package com.gym.app.ui.workout

import com.gym.app.data.entity.Exercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * recomputeBest 单元测试：勾选/取消勾选/改次数重量后的「最佳」重算逻辑。
 * 回归用例覆盖 2026-08-20 实机 3 个 bug：
 * 1. 勾选完成后状态必须生效（sets 写回 state）
 * 2. 修改次数/重量后必须生效（sets 写回 state）
 * 3. 取消勾选纪录组后「最佳」还原为历史最佳（historyBest 基准）
 */
class RecomputeBestTest {

    private fun exerciseState(
        historyBest: BestSet? = null,
        bestSet: BestSet? = null,
        isNewRecord: Boolean = false,
        sets: List<ActualSet> = emptyList(),
    ) = ExerciseWorkoutState(
        exercise = Exercise(
            id = "0001",
            name = "杠铃深蹲",
            category = "杠铃",
            bodyPart = "腿",
            equipment = "",
            instructionsZh = "",
            instructionStepsZh = "",
            muscleGroup = "",
            secondaryMuscles = "",
            target = "股四头肌",
            imagePath = "",
            gifPath = "",
        ),
        targetSets = 3,
        targetReps = 10,
        actualSets = sets,
        sortOrder = 0,
        historyBest = historyBest,
        bestSet = bestSet,
        isNewRecord = isNewRecord,
    )

    private fun set(reps: Int = 10, weight: Float = 100f, completed: Boolean = false) =
        ActualSet(reps = reps, weight = weight, completed = completed)

    // --- Bug 1+2：修改必须写回 state.actualSets ---

    @Test
    fun `toggle completed writes back the flipped set`() {
        val s0 = set(completed = false)
        val state = exerciseState(sets = listOf(s0))

        val checked = recomputeBest(state, listOf(s0.copy(completed = true)))

        assertTrue(checked.actualSets[0].completed)      // 勾选必须生效
        assertEquals(1, checked.actualSets.size)
    }

    @Test
    fun `updating reps writes back the new value`() {
        val state = exerciseState(sets = listOf(set(reps = 10)))

        val updated = recomputeBest(state, listOf(set(reps = 15)))

        assertEquals(15, updated.actualSets[0].reps)     // 输入必须生效
    }

    @Test
    fun `updating weight writes back the new value`() {
        val state = exerciseState(sets = listOf(set(weight = 100f)))

        val updated = recomputeBest(state, listOf(set(weight = 120f)))

        assertEquals(120f, updated.actualSets[0].weight, 0.001f)
    }

    @Test
    fun `removing a set keeps only remaining sets`() {
        val state = exerciseState(
            sets = listOf(set(), set(weight = 60f), set(weight = 80f)),
        )

        val afterRemove = recomputeBest(state, state.actualSets.filterIndexed { i, _ -> i != 1 })

        assertEquals(2, afterRemove.actualSets.size)
        assertEquals(80f, afterRemove.actualSets[1].weight, 0.001f)
    }

    // --- Bug 3：取消勾选后最佳还原 ---

    @Test
    fun `unchecking the record set reverts best to history`() {
        // 历史最佳 100kg；勾选 120kg 破纪录
        val state = exerciseState(
            historyBest = BestSet(100f, 10),
            bestSet = BestSet(100f, 10),
            sets = listOf(set(weight = 120f)),
        )
        val newRecord = recomputeBest(state, listOf(set(weight = 120f, completed = true)))
        assertEquals(120f, newRecord.bestSet!!.weight, 0.001f)
        assertTrue(newRecord.isNewRecord)

        // 取消勾选 120kg 纪录组 → 还原为历史最佳 100kg，星标消失
        val reverted = recomputeBest(newRecord, listOf(set(weight = 120f, completed = false)))
        assertEquals(100f, reverted.bestSet!!.weight, 0.001f)
        assertFalse(reverted.isNewRecord)
    }

    @Test
    fun `unchecking all sets reverts to history`() {
        val state = exerciseState(
            historyBest = BestSet(100f, 10),
            bestSet = BestSet(100f, 10),
            sets = listOf(set(weight = 120f, completed = true), set(weight = 60f, completed = true)),
        )
        val allUnchecked = recomputeBest(state, listOf(set(weight = 120f), set(weight = 60f)))

        assertEquals(100f, allUnchecked.bestSet!!.weight, 0.001f)
        assertFalse(allUnchecked.isNewRecord)
    }

    @Test
    fun `no history and uncheck all shows no best`() {
        val state = exerciseState(sets = listOf(set(weight = 120f, completed = true)))
        val record = recomputeBest(state, listOf(set(weight = 120f, completed = true)))
        assertEquals(120f, record.bestSet!!.weight, 0.001f)
        assertTrue(record.isNewRecord)

        val reverted = recomputeBest(record, listOf(set(weight = 120f)))
        assertNull(reverted.bestSet)
        assertFalse(reverted.isNewRecord)
    }

    // --- 最佳对比规则：重量优先、同重比次数 ---

    @Test
    fun `best picks higher weight`() {
        val state = exerciseState(
            historyBest = BestSet(100f, 10),
            bestSet = BestSet(100f, 10),
        )
        val result = recomputeBest(
            state,
            listOf(set(weight = 80f, completed = true), set(weight = 130f, completed = true)),
        )
        assertEquals(130f, result.bestSet!!.weight, 0.001f)
        assertTrue(result.isNewRecord)
    }

    @Test
    fun `same weight higher reps wins`() {
        val state = exerciseState(
            historyBest = BestSet(100f, 10),
            bestSet = BestSet(100f, 10),
        )
        val result = recomputeBest(
            state,
            listOf(set(weight = 100f, reps = 12, completed = true)),
        )
        assertEquals(12, result.bestSet!!.reps)
        assertTrue(result.isNewRecord)
    }

    @Test
    fun `below history best keeps history without star`() {
        val state = exerciseState(
            historyBest = BestSet(100f, 10),
            bestSet = BestSet(100f, 10),
        )
        val result = recomputeBest(state, listOf(set(weight = 60f, completed = true)))
        assertEquals(100f, result.bestSet!!.weight, 0.001f)
        assertFalse(result.isNewRecord)
    }

    @Test
    fun `uncompleted sets are ignored`() {
        val state = exerciseState(
            historyBest = BestSet(100f, 10),
            bestSet = BestSet(100f, 10),
        )
        val result = recomputeBest(state, listOf(set(weight = 300f, completed = false)))
        assertEquals(100f, result.bestSet!!.weight, 0.001f)
        assertFalse(result.isNewRecord)
    }

    @Test
    fun `zero weight completed set does not become best`() {
        val state = exerciseState()
        val result = recomputeBest(state, listOf(set(weight = 0f, completed = true)))
        assertNull(result.bestSet)
        assertFalse(result.isNewRecord)
    }
}
