package com.github.ilife798

// 跨平台的运行通知与前台保活入口。
// 调用方（[com.github.ilife798.data.viewmodel.AppViewModel]）只需在设备/积分任务状态变化时
// 上报最新状态；Android 端在运行期间常驻展示运行通知（含实时动态），并借此维持前台服务保活。
// iOS 由 UNUserNotificationCenter 实现运行状态通知；其余平台为空实现。
expect object RunNotifications {
    // 上报设备运行状态；[running] 为 false 表示已停止。
    fun updateDevice(
        deviceId: String,
        deviceName: String,
        running: Boolean,
    )

    // 上报积分任务运行中已获得的积分。
    fun updateTask(gained: Int)

    // 积分任务结束后移除任务通知。
    fun removeTask()
}
