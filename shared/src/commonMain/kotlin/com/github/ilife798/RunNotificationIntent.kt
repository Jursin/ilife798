package com.github.ilife798

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 运行状态通知中的操作（点击通知/停止按钮）触发的 UI 请求。
 *
 * Android 端 MainActivity 收到通知携带的 action 后写入这里，
 * UI 层观察对应字段变化并执行跳转与停止操作，完成后消费请求。
 */
object RunNotificationIntent {
    /** 通知“停止”按钮请求停止的设备编号；null 表示无请求。 */
    var stopDeviceId by mutableStateOf<String?>(null)
        private set

    fun requestStopDevice(deviceId: String) {
        stopDeviceId = deviceId
    }

    fun consumeStopDevice() {
        stopDeviceId = null
    }

    /** 点击设备通知正文：仅回到首页。 */
    var openHomeRequestId by mutableIntStateOf(0)
        private set

    fun requestOpenHome() {
        openHomeRequestId++
    }

    fun consumeOpenHome() {
        openHomeRequestId = 0
    }

    /** 点击积分任务通知正文：跳转到任务页。 */
    var openTasksRequestId by mutableIntStateOf(0)
        private set

    fun requestOpenTasks() {
        openTasksRequestId++
    }

    fun consumeOpenTasks() {
        openTasksRequestId = 0
    }

    /** 点击积分任务通知“停止”按钮：跳转到任务页并停止任务。 */
    var stopTasksRequestId by mutableIntStateOf(0)
        private set

    fun requestStopTasks() {
        stopTasksRequestId++
    }

    fun consumeStopTasks() {
        stopTasksRequestId = 0
    }
}
