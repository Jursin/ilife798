package com.github.ilife798.util

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatTest {
    @Test
    fun moneyKeepsTwoDecimals() {
        assertEquals("0.00", formatMoney(0.0))
        assertEquals("12.30", formatMoney(12.3))
        assertEquals("12.34", formatMoney(12.34))
        assertEquals("100.00", formatMoney(100.0))
    }

    @Test
    fun moneyKeepsSign() {
        assertEquals("-5.50", formatMoney(-5.5))
        assertEquals("-3.14", formatMoney(-3.141))
    }

    @Test
    fun moneyNullDefaultsToZero() {
        assertEquals("0.00", formatMoney(null as Double?))
    }

    @Test
    fun dateTimeInvalidValues() {
        assertEquals("", formatDateTime(0L))
        assertEquals("", formatDateTime(-1L))
        assertEquals("", formatDateTime(""))
        assertEquals("not-a-time", formatDateTime("not-a-time"))
    }
}
