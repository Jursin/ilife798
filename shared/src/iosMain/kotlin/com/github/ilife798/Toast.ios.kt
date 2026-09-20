package com.github.ilife798

// iOS 无系统 Toast 组件；按设计静默 no-op（Change 2 评估 HUD 替代）。
actual fun showToast(message: String) {}

actual fun dismissToast() {}
