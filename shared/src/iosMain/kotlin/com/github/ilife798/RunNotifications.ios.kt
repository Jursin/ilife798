package com.github.ilife798

/** iOS 端暂无后台运行通知，空实现。 */
actual object RunNotifications {
    actual fun ensureInitialized() {}

    actual fun updateDevice(deviceId: String, deviceName: String, running: Boolean) {}

    actual fun updateTask(gained: Int) {}

    actual fun removeTask() {}
}
