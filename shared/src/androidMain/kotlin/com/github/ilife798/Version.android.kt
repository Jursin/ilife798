package com.github.ilife798

actual fun getAppVersion(): String {
    return try {
        val packageInfo = ApplicationContext.instance.packageManager.getPackageInfo(ApplicationContext.instance.packageName, 0)
        packageInfo.versionName ?: "unknown"
    } catch (_: Exception) {
        "unknown"
    }
}

actual fun getAppVersionCode(): String {
    return try {
        val packageInfo = ApplicationContext.instance.packageManager.getPackageInfo(ApplicationContext.instance.packageName, 0)
        val code = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        code.toString()
    } catch (_: Exception) {
        "unknown"
    }
}
