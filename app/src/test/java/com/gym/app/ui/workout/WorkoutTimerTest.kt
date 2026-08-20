package com.gym.app.ui.workout

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * WorkoutTimer 单元测试：时间戳差值计时，覆盖暂停/恢复/完成结算。
 * 用假时钟模拟 elapsedRealtime（单位毫秒）。
 */
class WorkoutTimerTest {

    private class FakeClock {
        var now: Long = 0L
    }

    private fun timer(clock: FakeClock) = WorkoutTimer { clock.now }

    @Test
    fun `elapsed counts from start`() {
        val clock = FakeClock()
        val t = timer(clock)

        t.start()                    // now = 0
        clock.now = 60_000
        assertEquals(60, t.currentSeconds())

        clock.now = 61_500
        assertEquals(61, t.currentSeconds()) // 截断到秒
    }

    @Test
    fun `pause freezes current time not zero`() {
        val clock = FakeClock()
        val t = timer(clock)

        t.start()
        clock.now = 60_000
        assertEquals(60, t.currentSeconds())

        t.pause()                    // now = 60_000 → 冻结 60
        assertEquals(60, t.currentSeconds())

        // 暂停期间时钟继续走，显示保持冻结
        clock.now = 90_000
        assertEquals(60, t.currentSeconds())
    }

    @Test
    fun `resume continues from frozen value`() {
        val clock = FakeClock()
        val t = timer(clock)

        t.start()                    // now = 0
        clock.now = 60_000
        t.pause()                    // 冻结 60
        assertEquals(60, t.currentSeconds())

        clock.now = 90_000           // 暂停 30 秒
        t.resume()
        clock.now = 100_000          // 恢复后跑了 10 秒
        assertEquals(70, t.currentSeconds())

        clock.now = 130_000
        assertEquals(100, t.currentSeconds())
    }

    @Test
    fun `multiple pause resume cycles accumulate correctly`() {
        val clock = FakeClock()
        val t = timer(clock)

        t.start()                    // 0
        clock.now = 20_000
        assertEquals(20, t.currentSeconds())
        t.pause()                    // 冻结 20
        clock.now = 30_000           // 暂停 10s
        t.resume()
        clock.now = 50_000           // 运行 20s → 40
        assertEquals(40, t.currentSeconds())
        t.pause()                    // 冻结 40
        clock.now = 80_000           // 暂停 30s
        t.resume()
        clock.now = 90_000           // 运行 10s → 50
        assertEquals(50, t.currentSeconds())
    }

    @Test
    fun `finalSeconds settles total running time`() {
        val clock = FakeClock()
        val t = timer(clock)

        t.start()                    // 0
        clock.now = 60_000
        t.pause()                    // 冻结 60
        clock.now = 120_000          // 暂停 60s
        t.resume()
        clock.now = 150_000          // 运行 30s → 90
        assertEquals(90, t.finalSeconds())
    }

    @Test
    fun `finalSeconds while paused returns frozen value`() {
        val clock = FakeClock()
        val t = timer(clock)

        t.start()
        clock.now = 45_000
        t.pause()
        clock.now = 999_999
        assertEquals(45, t.finalSeconds())
    }

    @Test
    fun `double pause is ignored`() {
        val clock = FakeClock()
        val t = timer(clock)

        t.start()
        clock.now = 10_000
        t.pause()                    // 冻结 10
        t.pause()                    // 幂等
        clock.now = 20_000
        assertEquals(10, t.currentSeconds())
    }

    @Test
    fun `resume without pause is ignored`() {
        val clock = FakeClock()
        val t = timer(clock)

        t.start()
        t.resume()                   // 未暂停：忽略
        clock.now = 15_000
        assertEquals(15, t.currentSeconds())
    }

    @Test
    fun `start resets all state`() {
        val clock = FakeClock()
        val t = timer(clock)

        t.start()
        clock.now = 60_000
        t.pause()
        clock.now = 90_000
        assertEquals(60, t.currentSeconds())

        t.start()                    // 重新开始
        clock.now = 95_000
        assertEquals(5, t.currentSeconds())
    }
}
