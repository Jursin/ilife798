package com.github.ilife798

import platform.Foundation.NSBundle

actual fun getAppVersion(): String {
    return NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "unknown"
}

actual fun getAppVersionCode(): String {
    return NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String ?: "unknown"
}
