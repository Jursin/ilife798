package com.github.ilife798

import platform.Foundation.NSBundle

// 版本号来自 App 包信息；缺失返回 "0"。
actual fun getAppVersion(): String = NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String ?: "0"

actual fun getAppVersionCode(): String = NSBundle.mainBundle.infoDictionary?.get("CFBundleVersion") as? String ?: "0"
