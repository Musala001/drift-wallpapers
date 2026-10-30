package com.driftwalls.app.data.remote.net

/**
 * Blocking token bucket: up to [burst] calls at once, refilling at [permitsPerSecond].
 * Callers reserve a token immediately and sleep off any debt, so concurrent callers queue fairly.
 */
class RateLimiter(
    private val permitsPerSecond: Double,
    private val burst: Int = 1,
    private val nanoClock: () -> Long = System::nanoTime,
    private val sleepNanos: (Long) -> Unit = { Thread.sleep(it / 1_000_000, (it % 1_000_000).toInt()) },
) {
    private var tokens = burst.toDouble()
    private var lastRefill = nanoClock()

    init {
        require(permitsPerSecond > 0 && burst >= 1)
    }

    fun acquire() {
        val waitNanos = synchronized(this) {
            val now = nanoClock()
            tokens = minOf(burst.toDouble(), tokens + (now - lastRefill) / 1e9 * permitsPerSecond)
            lastRefill = now
            tokens -= 1
            if (tokens >= 0) 0L else (-tokens / permitsPerSecond * 1e9).toLong()
        }
        if (waitNanos > 0) sleepNanos(waitNanos)
    }
}
