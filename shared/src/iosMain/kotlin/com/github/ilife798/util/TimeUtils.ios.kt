package com.github.ilife798.util

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitWeekday
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.timeIntervalSince1970

// iOS 时间工具：NSDateFormatter 默认使用设备本地时区与本地化设置，
// 与 Android SimpleDateFormat(Locale.getDefault()) 行为对齐。
private fun newFormatter(pattern: String): NSDateFormatter =
    NSDateFormatter().apply {
        dateFormat = pattern
    }

// NSDate 仅提供 referenceDate（2001-01-01T00:00:00Z）构造；换算 1970 纪元差秒数。
private const val REFERENCE_DATE_EPOCH_OFFSET = 978_307_200.0

private fun epochMsToDate(epochMs: Long): NSDate = NSDate(timeIntervalSinceReferenceDate = epochMs / 1000.0 - REFERENCE_DATE_EPOCH_OFFSET)

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun currentTimeFormatted(pattern: String): String = newFormatter(pattern).stringFromDate(NSDate())

actual fun formatTimestamp(
    timestamp: Long,
    pattern: String,
): String = newFormatter(pattern).stringFromDate(epochMsToDate(timestamp))

// Android 端语义：周一=1..周日=7。注意 ICU 的 "u" 格式是 Extended Year（年份），
// 不能用 NSDateFormatter("u") 取周几；NSCalendar 的 weekday 为周日=1..周六=7，需换算。
actual fun getDayOfWeek(): Int {
    val weekday =
        NSCalendar.currentCalendar
            .component(NSCalendarUnitWeekday, fromDate = NSDate())
            .toInt()
    return if (weekday == 1) 7 else weekday - 1
}

actual fun getTodayStart(now: Long): Long {
    val day = newFormatter("yyyy-MM-dd").stringFromDate(epochMsToDate(now))
    val midnight = newFormatter("yyyy-MM-dd HH:mm:ss").dateFromString("$day 00:00:00") ?: return now
    return (midnight.timeIntervalSince1970 * 1000).toLong()
}
