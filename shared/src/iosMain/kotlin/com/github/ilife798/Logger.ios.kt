package com.github.ilife798

import platform.Foundation.NSLog

// 日志：NSLog（含 tag 前缀）；走 %@ 占位避免消息内 % 被当作格式符。
actual fun logDebug(
    tag: String,
    message: String,
) {
    NSLog("%@", "[$tag] $message")
}
