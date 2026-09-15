package com.github.ilife798

import android.app.NotificationManager

// Android 端的运行通知实现。
// 设备运行与积分任务运行通知在运行期间常驻展示，并直接作为前台服务通知（含实时动态），
// 因此不再区分前后台，也不会与前台服务的保活通知重复。
actual object RunNotifications {
    private data class DeviceState(
        val deviceId: String,
        val deviceName: String,
    )

    private val deviceStates = LinkedHashMap<String, DeviceState>()
    private var taskGained: Int? = null

    actual fun updateDevice(
        deviceId: String,
        deviceName: String,
        running: Boolean,
    ) {
        if (deviceId.isBlank()) return
        if (running) {
            deviceStates[deviceId] = DeviceState(deviceId, deviceName)
        } else {
            deviceStates.remove(deviceId)
            cancelNotification(deviceNotificationId(deviceId))
        }
        sync()
    }

    actual fun updateTask(gained: Int) {
        taskGained = gained.coerceAtLeast(0)
        sync()
    }

    actual fun removeTask() {
        taskGained = null
        sync()
    }

    // 依据当前运行状态驱动前台服务：前台服务通知使用固定 id，内容镜像主通知
    // （任务优先，否则取第一台设备）；其余设备按各自 id 单独常驻展示。
    private fun sync() {
        val context = ApplicationContext.instance
        ensureRunChannels(context)
        val gained = taskGained
        val primary =
            when {
                gained != null -> {
                    buildTaskNotification(context, gained)
                }

                deviceStates.isNotEmpty() -> {
                    val first = deviceStates.values.first()
                    buildDeviceNotification(context, first.deviceId, first.deviceName)
                }

                else -> {
                    null
                }
            }

        if (primary == null) {
            RunForegroundService.refresh(context, active = false, notificationId = RUN_NOTIFICATION_ID, notification = null)
            return
        }

        RunForegroundService.refresh(context, active = true, notificationId = RUN_NOTIFICATION_ID, notification = primary)

        // 主通知镜像已代表第一台设备（无任务时），避免与其单独通知重复
        val mirroredDeviceId = if (gained == null) deviceStates.keys.firstOrNull() else null
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager != null && canPostNotification(context, CHANNEL_DEVICE)) {
            deviceStates.values.forEach { state ->
                val id = deviceNotificationId(state.deviceId)
                if (state.deviceId == mirroredDeviceId) {
                    manager.cancel(id)
                } else {
                    runCatching {
                        manager.notify(id, buildDeviceNotification(context, state.deviceId, state.deviceName))
                    }
                }
            }
        }
    }
}
