package com.github.ilife798

/**
 * 跨平台的后台运行通知入口。
 *
 * 调用方（[com.github.ilife798.data.viewmodel.AppViewModel]）只需在设备/积分任务状态变化时
 * 上报最新状态；Android 端仅在应用处于后台时展示通知，应用回到前台会自动收起。
 * 其它平台为空实现。
 */
expect object RunNotifications {
    /** 注册应用前后台监听（Android 端在 Activity 创建时调用）。 */
    fun ensureInitialized()

    /** 上报设备运行状态；[running] 为 false 表示已停止。 */
    fun updateDevice(deviceId: String, deviceName: String, running: Boolean)

    /** 上报积分任务运行中已获得的积分。 */
    fun updateTask(gained: Int)

    /** 积分任务结束后移除任务通知。 */
    fun removeTask()
}
