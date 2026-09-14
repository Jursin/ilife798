package com.github.ilife798.util

// 跳转到系统的电池优化/省电策略设置页面，供用户将本应用加入白名单。
expect fun openBatteryOptimizationSettings()

// 本应用是否已不受电池优化限制（已设为「无限制」/已忽略优化）。
expect fun isIgnoringBatteryOptimizations(): Boolean
