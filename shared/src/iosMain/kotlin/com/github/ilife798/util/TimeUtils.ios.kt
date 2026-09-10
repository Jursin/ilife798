package com.github.ilife798.util

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitWeekOfYear
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.localTimeZone
import platform.Foundation.currentLocale

actual fun currentTimeMillis(): Long {
    return (NSDate().timeIntervalSinceReferenceDate * 1000).toLong() + 978307200000L
}

actual fun currentTimeFormatted(pattern: String): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = pattern
    formatter.locale = NSLocale.currentLocale
    formatter.timeZone = NSTimeZone.localTimeZone
    return formatter.stringFromDate(NSDate())
}

actual fun formatTimestamp(timestamp: Long, pattern: String): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = pattern
    formatter.locale = NSLocale.currentLocale
    formatter.timeZone = NSTimeZone.localTimeZone
    val date = NSDate(timeIntervalSinceReferenceDate = (timestamp - 978307200000L) / 1000.0)
    return formatter.stringFromDate(date)
}

actual fun getDayOfWeek(): Int {
    val calendar = NSCalendar.currentCalendar
    val weekday = calendar.ordinalityOfUnit(
        NSCalendarUnitDay,
        NSCalendarUnitWeekOfYear,
        NSDate()
    )
    return weekday.toInt()
}

actual fun getTodayStart(now: Long): Long {
    val calendar = NSCalendar.currentCalendar
    val date = NSDate(timeIntervalSinceReferenceDate = (now - 978307200000L) / 1000.0)
    val components = calendar.components(
        NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay,
        date
    )
    components.hour = 0
    components.minute = 0
    components.second = 0
    val startOfDay = calendar.dateFromComponents(components)!!
    return (startOfDay.timeIntervalSinceReferenceDate * 1000).toLong() + 978307200000L
}

actual fun md5(input: String): String {
    val data = input.encodeToByteArray()
    val hash = md5Pure(data)
    val hexChars = "0123456789abcdef"
    return hash.joinToString("") { byte ->
        val v = byte.toInt() and 0xFF
        "${hexChars[v shr 4]}${hexChars[v and 0x0F]}"
    }
}

private fun md5Pure(input: ByteArray): ByteArray {
    val s = intArrayOf(
        7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
        5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
        4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
        6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21
    )
    val k = intArrayOf(
        -680876936, -389564586, 606105819, -1044525330, -176418897, 1200080426,
        -1473231341, -45705983, 1770035416, -1958414417, -42063, -1990404162,
        1804603682, -40341101, -1502002290, 1236535329, -165796510, -1069501632,
        643717713, -373897302, -701558691, 38016083, -660478335, -405537848,
        568446438, -1019803690, -187363961, 1163531501, -1444681467, -51403784,
        1735328473, -1926607734, -378558, -2022574463, 1839030562, -35309556,
        -1530992060, 1272893353, -155497632, -1094730640, 681279174, -358537222,
        -722521979, 76029189, -640364487, -421815835, 530742520, -995338651,
        -198630844, 1126891415, -1416354905, -57434055, 1700485571, -1894986606,
        -1051523, -2054922799, 1873313359, -30611744, -1560198380, 1309151649,
        -145523070, -1120210379, 718787259, -343485551
    )

    var a0 = 0x67452301
    var b0 = 0xefcdab89.toInt()
    var c0 = 0x98badcfe.toInt()
    var d0 = 0x10325476

    val msgLen = input.size
    val bitLen = msgLen.toLong() * 8
    val paddedLen = ((msgLen + 9 + 63) / 64) * 64
    val padded = ByteArray(paddedLen)
    input.copyInto(padded)
    padded[msgLen] = 0x80.toByte()
    for (i in 0 until 8) {
        padded[paddedLen - 8 + i] = ((bitLen shr (i * 8)) and 0xFF).toByte()
    }

    for (offset in 0 until paddedLen step 64) {
        val m = IntArray(16)
        for (j in 0 until 16) {
            m[j] = (padded[offset + j * 4].toInt() and 0xFF) or
                    ((padded[offset + j * 4 + 1].toInt() and 0xFF) shl 8) or
                    ((padded[offset + j * 4 + 2].toInt() and 0xFF) shl 16) or
                    ((padded[offset + j * 4 + 3].toInt() and 0xFF) shl 24)
        }

        var a = a0; var b = b0; var c = c0; var d = d0

        for (i in 0 until 64) {
            val f: Int; val g: Int
            when {
                i < 16 -> { f = (b and c) or (b.inv() and d); g = i }
                i < 32 -> { f = (d and b) or (d.inv() and c); g = (5 * i + 1) % 16 }
                i < 48 -> { f = b xor c xor d; g = (3 * i + 5) % 16 }
                else -> { f = c xor (b or d.inv()); g = (7 * i) % 16 }
            }
            val temp = d
            d = c
            c = b
            b += ((a + f + k[i] + m[g]) shl s[i]) or ((a + f + k[i] + m[g]) ushr (32 - s[i]))
            a = temp
        }
        a0 += a; b0 += b; c0 += c; d0 += d
    }

    return ByteArray(16).also { result ->
        for (i in 0 until 4) {
            result[i] = ((a0 shr (i * 8)) and 0xFF).toByte()
            result[4 + i] = ((b0 shr (i * 8)) and 0xFF).toByte()
            result[8 + i] = ((c0 shr (i * 8)) and 0xFF).toByte()
            result[12 + i] = ((d0 shr (i * 8)) and 0xFF).toByte()
        }
    }
}
