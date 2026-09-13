package com.github.ilife798.util

import kotlin.math.abs
import kotlin.math.round

// 金额格式化：保留两位小数，负数保留符号。
fun formatMoney(value: Double): String {
    val negative = value < 0
    val scaled = round(abs(value) * 100).toLong()
    val intPart = scaled / 100
    val frac = (scaled % 100).toString().padStart(2, '0')
    return (if (negative) "-" else "") + "$intPart.$frac"
}

fun formatMoney(value: Double?): String = formatMoney(value ?: 0.0)

// 时间戳格式化为 yyyy-MM-dd HH:mm:ss，无效值返回空串。
fun formatDateTime(time: Long): String = if (time <= 0L) "" else formatTimestamp(time, "yyyy-MM-dd HH:mm:ss")

// 字符串时间戳格式化，非数字字符串原样返回。
fun formatDateTime(time: String): String = formatDateTime(time.toLongOrNull() ?: return time)
