package com.gym.app.ui.workout

/**
 * 训练计时器：elapsedRealtime 时间戳差值法。
 *
 * 显示时长 = 现在 − 训练开始 − 暂停累计，后台/熄屏期间单调时钟照常走，
 * 回前台自动校正，不靠「每秒 +1」数秒（Doze 会冻结线程导致少计时）。
 *
 * 暂停：记录暂停起点并冻结当前时长；恢复：结算暂停段时长。
 * [nowMs] 可注入（测试用假时钟），生产用 [android.os.SystemClock.elapsedRealtime]。
 */
internal class WorkoutTimer(private val nowMs: () -> Long) {

    private var startedAt: Long = 0L
    /** 当前暂停段的起点；null = 运行中 */
    private var pausedAt: Long? = null
    /** 暂停累计时长（毫秒），恢复时结算 */
    private var pausedAccumulatedMs: Long = 0L
    /** 暂停时冻结的秒数 */
    private var frozenSeconds: Int = 0

    /** 开始计时（清零）。 */
    fun start() {
        startedAt = nowMs()
        pausedAt = null
        pausedAccumulatedMs = 0L
        frozenSeconds = 0
    }

    /** 当前训练时长（秒）；暂停中返回冻结值。 */
    fun currentSeconds(): Int {
        if (pausedAt != null) return frozenSeconds
        return ((nowMs() - startedAt - pausedAccumulatedMs) / 1000L).toInt()
    }

    /** 暂停：冻结当前时长，记录暂停起点。 */
    fun pause() {
        if (pausedAt != null) return
        val now = nowMs()
        // 冻结值必须在把暂停段计入 pausedAccumulatedMs 之前计算，
        // 否则「现在 − 开始 − 暂停累计」会把刚结算的运行段也减掉，时间归零
        frozenSeconds = ((now - startedAt - pausedAccumulatedMs) / 1000L).toInt()
        pausedAt = now
    }

    /** 恢复：结算暂停段时长，继续计时。 */
    fun resume() {
        val pausedAt = pausedAt ?: return
        pausedAccumulatedMs += nowMs() - pausedAt
        this.pausedAt = null
    }

    /** 完成时结算最终时长（暂停中返回冻结值）。 */
    fun finalSeconds(): Int {
        if (pausedAt != null) return frozenSeconds
        return ((nowMs() - startedAt - pausedAccumulatedMs) / 1000L).toInt()
    }
}
