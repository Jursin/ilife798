package com.github.ilife798

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * 点击“正在下载”通知时触发的导航请求。
 * Android 端 MainActivity 收到对应 action 后调用 [requestShow]，
 * UI 层观察 [showRequestId] 变化并跳转到“我的”页并重新弹出下载对话框。
 */
object AppUpdateIntent {
    var showRequestId by mutableIntStateOf(0)
        private set

    fun requestShow() {
        showRequestId++
    }

    fun consume() {
        showRequestId = 0
    }
}
