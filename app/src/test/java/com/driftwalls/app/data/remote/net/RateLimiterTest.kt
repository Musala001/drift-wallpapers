package com.driftwalls.app.data.remote.net

import org.junit.Assert.assertEquals
import org.junit.Test

class RateLimiterTest {
    private var now = 0L
    private val sleeps = mutableListOf<Long>()
    private fun limiter(perSecond: Double, burst: Int) = RateLimiter(
        permitsPerSecond = perSecond,
        burst = burst,
        nanoClock = { now },
        sleepNanos = { sleeps += it; now += it },
    )

    @Test
    fun `allows the burst immediately, then spaces calls out`() {
        val limiter = limiter(perSecond = 1.0, burst = 2)
        repeat(4) { limiter.acquire() }
        assertEquals(listOf(1_000_000_000L, 1_000_000_000L), sleeps)
    }

    @Test
    fun `refills over idle time but never above the burst`() {
        val limiter = limiter(perSecond = 2.0, burst = 1)
        limiter.acquire()
        now += 10_000_000_000L // idle for 10s
        limiter.acquire()
        limiter.acquire()
        assertEquals(listOf(500_000_000L), sleeps)
    }

    @Test
    fun `sixty calls take at least a minute at one per second`() {
        val limiter = limiter(perSecond = 1.0, burst = 1)
        repeat(61) { limiter.acquire() }
        assertEquals(60_000_000_000L, now)
    }
}
