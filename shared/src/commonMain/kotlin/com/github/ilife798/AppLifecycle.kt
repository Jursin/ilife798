package com.github.ilife798

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

// 跨平台的应用回到前台回调。Android 端在 Activity.onResume 时触发。
object AppLifecycle {
    var onResumed: (() -> Unit)? = null

    // 回到前台的次数，供 UI 在从系统设置返回后重新检查平台状态。
    var resumeCount by mutableIntStateOf(0)
        private set

    fun notifyResumed() {
        resumeCount++
        onResumed?.invoke()
    }
}
