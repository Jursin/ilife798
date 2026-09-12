package com.github.ilife798

import platform.Foundation.NSLog

actual fun showToast(message: String) {
    NSLog("Toast: %@", message)
}

actual fun dismissToast() {}
