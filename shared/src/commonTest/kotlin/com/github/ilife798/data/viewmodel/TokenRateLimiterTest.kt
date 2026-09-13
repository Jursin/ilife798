package com.github.ilife798.data.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals

class TokenRateLimiterTest {
    @Test
    fun firstReservationIsImmediate() {
        val limiter = TokenRateLimiter(minIntervalMs = 1_000L)
        assertEquals(0L, limiter.reserveDelay("token", 0L))
    }

    @Test
    fun repeatedReservationWaitsRemainingInterval() {
        val limiter = TokenRateLimiter(minIntervalMs = 1_000L)
        limiter.reserveDelay("token", 0L)
        assertEquals(900L, limiter.reserveDelay("token", 100L))
        assertEquals(500L, limiter.reserveDelay("token", 1_500L))
    }

    @Test
    fun reservationAfterIntervalIsImmediate() {
        val limiter = TokenRateLimiter(minIntervalMs = 1_000L)
        limiter.reserveDelay("token", 0L)
        assertEquals(0L, limiter.reserveDelay("token", 1_000L))
    }

    @Test
    fun tokensAreRateLimitedIndependently() {
        val limiter = TokenRateLimiter(minIntervalMs = 1_000L)
        limiter.reserveDelay("a", 0L)
        assertEquals(0L, limiter.reserveDelay("b", 0L))
    }

    @Test
    fun clearResetsSchedule() {
        val limiter = TokenRateLimiter(minIntervalMs = 1_000L)
        limiter.reserveDelay("token", 0L)
        limiter.clear()
        assertEquals(0L, limiter.reserveDelay("token", 10L))
    }
}
