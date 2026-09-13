package com.github.ilife798.data.viewmodel

import com.github.ilife798.data.model.BillRecord
import com.github.ilife798.data.model.SpendingStats

// 账单聚合为消费统计的纯逻辑，供 WalletBillController 与测试复用。
// [add] 的 [dayKey] 需由调用方按 yyyy-MM-dd 计算后传入。
internal class SpendingAccumulator(
    private val todayKey: String,
    private val yesterdayKey: String,
    private val monthPrefix: String,
) {
    private var today = 0.0
    private var yesterday = 0.0
    private val monthDaySums = mutableMapOf<String, Double>()

    fun add(
        record: BillRecord,
        dayKey: String,
    ) {
        // 只统计支出(dir=1)且为正、非充值的记录
        if (record.dir != 1 || record.payment <= 0.0 || record.cata == 1) return
        when (dayKey) {
            todayKey -> today += record.payment
            yesterdayKey -> yesterday += record.payment
        }
        if (dayKey.startsWith(monthPrefix)) {
            monthDaySums[dayKey] = (monthDaySums[dayKey] ?: 0.0) + record.payment
        }
    }

    fun build(): SpendingStats {
        val spendingDays = monthDaySums.count { it.value > 0.0 }
        val monthTotal = monthDaySums.values.sum()
        return SpendingStats(
            today = today,
            yesterday = yesterday,
            monthAverage = if (spendingDays > 0) monthTotal / spendingDays else 0.0,
        )
    }
}
