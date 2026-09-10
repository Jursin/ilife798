package com.github.ilife798

import platform.Foundation.NSLog

actual fun logDebug(tag: String, message: String) {
    NSLog("%@: %@", tag, message)
}
