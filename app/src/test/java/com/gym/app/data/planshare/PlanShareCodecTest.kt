package com.gym.app.data.planshare

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * PlanShareCodec 单元测试：编解码往返、宽容解析、非法输入拒绝。
 */
class PlanShareCodecTest {

    private fun samplePlan() = PlanShareCodec.SharedPlan(
        name = "练腿日",
        exercises = listOf(
            PlanShareCodec.SharedExercise(
                id = "0001-2gPfomN", name = "杠铃深蹲", custom = false, sets = 4, reps = 10,
            ),
            PlanShareCodec.SharedExercise(
                id = "0110-abc", name = "腿举", custom = false, sets = 3, reps = 12, weightMode = 2,
            ),
            PlanShareCodec.SharedExercise(
                id = "custom_123", name = "保加利亚单腿蹬", custom = true, sets = 3, reps = 12,
            ),
        ),
    )

    @Test
    fun `encode then decode round trip`() {
        val encoded = PlanShareCodec.encode(samplePlan())
        val decoded = PlanShareCodec.decode(encoded)

        assertNotNull(decoded)
        assertEquals("练腿日", decoded!!.name)
        assertEquals(3, decoded.exercises.size)
        assertEquals("0001-2gPfomN", decoded.exercises[0].id)
        assertEquals(4, decoded.exercises[0].sets)
        assertEquals(2, decoded.exercises[1].weightMode)
        assertEquals(true, decoded.exercises[2].custom)
    }

    @Test
    fun `encoded string has GYM1 prefix`() {
        val encoded = PlanShareCodec.encode(samplePlan())
        assertEquals(true, encoded.startsWith(PlanShareCodec.PREFIX))
    }

    @Test
    fun `decode accepts raw payload without prefix`() {
        val encoded = PlanShareCodec.encode(samplePlan())
        val rawPayload = encoded.removePrefix(PlanShareCodec.PREFIX)
        assertNotNull(PlanShareCodec.decode(rawPayload))
    }

    @Test
    fun `decode accepts surrounding whitespace`() {
        val encoded = PlanShareCodec.encode(samplePlan())
        assertNotNull(PlanShareCodec.decode("  $encoded\n"))
    }

    @Test
    fun `decode rejects garbage text`() {
        assertNull(PlanShareCodec.decode("随便一段文字"))
        assertNull(PlanShareCodec.decode("GYM1:###not-base64###"))
        assertNull(PlanShareCodec.decode(""))
        assertNull(PlanShareCodec.decode("   "))
    }

    @Test
    fun `decode rejects unknown version`() {
        val plan = samplePlan().copy(v = 99)
        assertNull(PlanShareCodec.decode(PlanShareCodec.encode(plan)))
    }

    @Test
    fun `decode rejects blank plan name`() {
        val plan = samplePlan().copy(name = "  ")
        assertNull(PlanShareCodec.decode(PlanShareCodec.encode(plan)))
    }

    @Test
    fun `decode rejects empty exercise list`() {
        val plan = samplePlan().copy(exercises = emptyList())
        assertNull(PlanShareCodec.decode(PlanShareCodec.encode(plan)))
    }

    @Test
    fun `decode rejects out of range sets and reps`() {
        val badSets = samplePlan().copy(
            exercises = listOf(
                PlanShareCodec.SharedExercise(id = "x", name = "y", custom = false, sets = 0, reps = 10),
            ),
        )
        assertNull(PlanShareCodec.decode(PlanShareCodec.encode(badSets)))

        val badReps = samplePlan().copy(
            exercises = listOf(
                PlanShareCodec.SharedExercise(id = "x", name = "y", custom = false, sets = 3, reps = 100),
            ),
        )
        assertNull(PlanShareCodec.decode(PlanShareCodec.encode(badReps)))
    }

    @Test
    fun `decode accepts legacy compact key share string`() {
        // 用户实测粘贴的旧版紧凑键名（a/b/c/d/e/f）分享串，向后兼容回归用例
        val legacy = "GYM1:eyJhIjoxLCJiIjoi5LiJ5YiG5YyWLeaLiSIsImMiOlt7ImEiOiIxMzQ3IiwiYiI6IuadoOadhuWNleiHguS-p-S4i-aLiSIsImMiOmZhbHNlLCJkIjo0LCJlIjoxNSwiZiI6MH0seyJhIjoiMDAxNyIsImIiOiLovoXliqnlvJXkvZPlkJHkuIoiLCJjIjpmYWxzZSwiZCI6NCwiZSI6MTAsImYiOjF9LHsiYSI6IjEzNTYiLCJiIjoi5p2g5p2G5Y2V6IeC5L6n5YiS6Ii5IiwiYyI6ZmFsc2UsImQiOjQsImUiOjE1LCJmIjowfSx7ImEiOiIyNjE2IiwiYiI6Iue7s-e0ouS-p-S4i-aLiVbmiooiLCJjIjpmYWxzZSwiZCI6NCwiZSI6MTUsImYiOjB9LHsiYSI6IjAxODAiLCJiIjoi57uz57Si5Z2Q5ae_5YiS6Ii5IiwiYyI6ZmFsc2UsImQiOjQsImUiOjE1LCJmIjowfSx7ImEiOiIyNDAzIiwiYiI6IuWTkemTg-S6jOabv-iCseS6jOWktOiCjOW8r-S4vuiHgiIsImMiOmZhbHNlLCJkIjozLCJlIjoxNSwiZiI6MH1dfQ"
        val decoded = PlanShareCodec.decode(legacy)

        assertNotNull(decoded)
        assertEquals("三分化-拉", decoded!!.name)
        assertEquals(6, decoded.exercises.size)
        assertEquals("1347", decoded.exercises[0].id)
        assertEquals(4, decoded.exercises[0].sets)
        assertEquals(15, decoded.exercises[0].reps)
        assertEquals("0017", decoded.exercises[1].id)
        assertEquals(1, decoded.exercises[1].weightMode)
    }

    @Test
    fun `decode accepts line breaks inside payload`() {
        // 聊天软件复制长文本时会折行
        val encoded = PlanShareCodec.encode(samplePlan())
        val payload = encoded.removePrefix(PlanShareCodec.PREFIX)
        val wrapped = PlanShareCodec.PREFIX + payload.chunked(20).joinToString("\n")
        val decoded = PlanShareCodec.decode(wrapped)

        assertNotNull(decoded)
        assertEquals("练腿日", decoded!!.name)
        assertEquals(3, decoded.exercises.size)
    }

    @Test
    fun `decode ignores zero width characters from chat copy`() {
        val encoded = PlanShareCodec.encode(samplePlan())
        val withInvisible = encoded.replace(PlanShareCodec.PREFIX, PlanShareCodec.PREFIX + "​")
        assertNotNull(PlanShareCodec.decode(withInvisible))
    }
}
