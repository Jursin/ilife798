package com.github.ilife798.util

// iOS 无电池优化限制概念；恒 true 避免误导性引导 UI（design §2.2）。
actual fun openBatteryOptimizationSettings() {}

actual fun isIgnoringBatteryOptimizations(): Boolean = true
