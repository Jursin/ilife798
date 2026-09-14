package com.github.ilife798

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.github.ilife798.util.currentTimeMillis

// 跨平台的应用前后台回调。Android 端在 Activity.onResume / onStop 时触发。
object AppLifecycle {
    var onResumed: (() -> Unit)? = null

    // 回到前台的次数，供 UI 在从系统设置返回后重新检查平台状态。
    var resumeCount by mutableIntStateOf(0)
        private set

    // 最近一次进入后台的时间戳，用于判断“跳到外部应用后又返回”。
    var backgroundedAt: Long = 0L
        private set

    fun notifyResumed() {
        resumeCount++
        onResumed?.invoke()
    }

    fun notifyStopped() {
        backgroundedAt = currentTimeMillis()
    }
}
