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
    fun `decode accepts legacy compact key share string - user reported failure case`() {
        // 用户 2026-08-20 实机反馈「无法识别的内容」的分享串（三分化-推），回归用例
        val legacy = "GYM1:eyJhIjoxLCJiIjoi5LiJ5YiG5YyWLeaOqCIsImMiOlt7ImEiOiIwMDI1IiwiYiI6IuadoOmTg-WNp-aOqCIsImMiOmZhbHNlLCJkIjo0LCJlIjoxMCwiZiI6MH0seyJhIjoiMDMxNCIsImIiOiLlk5Hpk4PkuIrmlpzljafmjqgiLCJjIjpmYWxzZSwiZCI6NCwiZSI6MTUsImYiOjB9LHsiYSI6IjAyNTEiLCJiIjoi5Y-M5p2g6IeC5bGI5Ly4IiwiYyI6ZmFsc2UsImQiOjQsImUiOjE1LCJmIjoxfSx7ImEiOiIwMzg5IiwiYiI6IuWTkemTg-WdkOWnv-WHs-S8uOWxlSIsImMiOmZhbHNlLCJkIjo1LCJlIjoxNSwiZiI6MH0seyJhIjoiMDQwNSIsImIiOiLlk5Hpk4PlnZDlp7_ogqnmjqjkuL4iLCJjIjpmYWxzZSwiZCI6MywiZSI6MTAsImYiOjB9LHsiYSI6IjAxOTIiLCJiIjoi57uz57Si5Y2V6IeC5L6n5bmz5Li-IiwiYyI6ZmFsc2UsImQiOjMsImUiOjE1LCJmIjowfV19"
        val decoded = PlanShareCodec.decode(legacy)

        assertNotNull(decoded)
        assertEquals("三分化-推", decoded!!.name)
        assertEquals(6, decoded.exercises.size)
        assertEquals("0025", decoded.exercises[0].id)
        assertEquals("双杠臂屈伸", decoded.exercises[2].name)
        assertEquals(1, decoded.exercises[2].weightMode)
        assertEquals("0192", decoded.exercises[5].id)
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

    @Test
    fun `decode tolerates leading and trailing junk text`() {
        val encoded = PlanShareCodec.encode(samplePlan())
        val withJunk = "这是分享给你的计划，复制这段：$encoded 记得导入哦！"
        val decoded = PlanShareCodec.decode(withJunk)

        assertNotNull(decoded)
        assertEquals("练腿日", decoded!!.name)
        assertEquals(3, decoded.exercises.size)
    }

    @Test
    fun `decode tolerates non breaking spaces from chat copy`() {
        val encoded = PlanShareCodec.encode(samplePlan())
        // NBSP (U+00A0) 不是 Java isWhitespace，聊天软件可能把它塞进长文本
        val withNbsp = encoded.replace(PlanShareCodec.PREFIX, PlanShareCodec.PREFIX + " ")
        assertNotNull(PlanShareCodec.decode(withNbsp))
    }

    @Test
    fun `decode tolerates junk without prefix`() {
        val encoded = PlanShareCodec.encode(samplePlan())
        val payload = encoded.removePrefix(PlanShareCodec.PREFIX)
        val withJunk = "复制这段代码 $payload 结尾"
        val decoded = PlanShareCodec.decode(withJunk)

        assertNotNull(decoded)
        assertEquals("练腿日", decoded!!.name)
    }

    @Test
    fun `extractShareString returns clean string from messy clipboard`() {
        val encoded = PlanShareCodec.encode(samplePlan())
        val clipboard = "猫教练计划分享\n$encoded\n长按复制整段文字"

        assertEquals(encoded, PlanShareCodec.extractShareString(clipboard))
    }

    @Test
    fun `extractShareString returns null for unrelated text`() {
        assertEquals(null, PlanShareCodec.extractShareString("今天练背，记得带毛巾"))
        assertEquals(null, PlanShareCodec.extractShareString(""))
    }
}
