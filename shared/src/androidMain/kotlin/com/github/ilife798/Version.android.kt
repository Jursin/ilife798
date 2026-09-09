package com.github.ilife798

actual fun getAppVersion(): String {
    return try {
        val packageInfo = ApplicationContext.instance.packageManager.getPackageInfo(ApplicationContext.instance.packageName, 0)
        packageInfo.versionName ?: "unknown"
    } catch (_: Exception) {
        "unknown"
    }
}
