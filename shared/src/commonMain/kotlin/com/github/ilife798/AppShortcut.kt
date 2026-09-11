package com.github.ilife798

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 桌面图标快捷方式（长按菜单）触发的导航请求。
 * Android 端 MainActivity 在冷启动与 onNewIntent 时调用 [requestScan]，
 * UI 层观察 [scanRequestId] 变化并跳转到扫码页。
 */
object AppShortcut {
    var scanRequestId by mutableIntStateOf(0)
        private set

    fun requestScan() {
        scanRequestId++
    }

    fun consumeScan() {
        scanRequestId = 0
    }

    /** 快捷设置图块点击后请求启动的设备编号；null 表示无请求 */
    var startDeviceId by mutableStateOf<String?>(null)
        private set

    fun requestStartDevice(deviceId: String) {
        startDeviceId = deviceId
    }

    fun consumeStartDevice() {
        startDeviceId = null
    }
}
