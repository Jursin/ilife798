package com.github.ilife798.data.viewmodel

// 按 token 限流，保证同一 token 的请求间隔不小于 [minIntervalMs]。
internal class TokenRateLimiter(
    private val minIntervalMs: Long = 30_000L,
) {
    private val nextAllowed = mutableMapOf<String, Long>()

    // 返回距离上次允许时间还需等待的毫秒数，并预约下一次允许时间。
    fun reserveDelay(
        token: String,
        now: Long,
    ): Long {
        val allowedAt = nextAllowed[token] ?: now
        val scheduledAt = maxOf(now, allowedAt)
        nextAllowed[token] = scheduledAt + minIntervalMs
        return scheduledAt - now
    }

    fun clear() {
        nextAllowed.clear()
    }
}
