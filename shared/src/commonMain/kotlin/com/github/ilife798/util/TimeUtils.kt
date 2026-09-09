package com.github.ilife798.util

expect fun currentTimeMillis(): Long

expect fun currentTimeFormatted(pattern: String): String

expect fun formatTimestamp(timestamp: Long, pattern: String): String

expect fun getDayOfWeek(): Int

expect fun getTodayStart(now: Long): Long

expect fun md5(input: String): String
