package com.github.ilife798.data.viewmodel

import com.github.ilife798.data.model.BillRecord
import kotlin.test.Test
import kotlin.test.assertEquals

class SpendingAccumulatorTest {
    private fun record(
        dir: Int = 1,
        payment: Double,
        cata: Int = 0,
    ) = BillRecord(dir = dir, payment = payment, cata = cata)

    private fun accumulator() =
        SpendingAccumulator(
            todayKey = "2026-01-02",
            yesterdayKey = "2026-01-01",
            monthPrefix = "2026-01",
        )

    @Test
    fun sumsTodayAndYesterday() {
        val accumulator = accumulator()
        accumulator.add(record(payment = 10.0), "2026-01-02")
        accumulator.add(record(payment = 4.0), "2026-01-01")
        val stats = accumulator.build()
        assertEquals(10.0, stats.today)
        assertEquals(4.0, stats.yesterday)
    }

    @Test
    fun ignoresNonExpenseRecords() {
        val accumulator = accumulator()
        accumulator.add(record(dir = -1, payment = 9.0), "2026-01-02")
        accumulator.add(record(payment = 0.0), "2026-01-02")
        accumulator.add(record(payment = 7.0, cata = 1), "2026-01-02")
        val stats = accumulator.build()
        assertEquals(0.0, stats.today)
        assertEquals(0.0, stats.monthAverage)
    }

    @Test
    fun monthAverageDividesByActiveDays() {
        val accumulator = accumulator()
        accumulator.add(record(payment = 10.0), "2026-01-02")
        accumulator.add(record(payment = 5.0), "2026-01-02")
        accumulator.add(record(payment = 20.0), "2026-01-03")
        assertEquals(17.5, accumulator.build().monthAverage)
    }

    @Test
    fun recordsOutsideMonthExcludedFromAverage() {
        val accumulator = accumulator()
        accumulator.add(record(payment = 30.0), "2025-12-31")
        accumulator.add(record(payment = 10.0), "2026-01-02")
        assertEquals(10.0, accumulator.build().monthAverage)
    }

    @Test
    fun zeroAverageWhenNoData() {
        assertEquals(0.0, accumulator().build().monthAverage)
    }
}
