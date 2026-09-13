package com.github.ilife798.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun currentTimeFormatted(pattern: String): String = SimpleDateFormat(pattern, Locale.getDefault()).format(Date())

actual fun formatTimestamp(
    timestamp: Long,
    pattern: String,
): String = SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))

actual fun getDayOfWeek(): Int {
    val calendarDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    return if (calendarDay == Calendar.SUNDAY) 7 else calendarDay - 1
}

actual fun getTodayStart(now: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = now
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
