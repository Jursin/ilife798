package com.github.ilife798

import android.app.Activity
import android.app.Application
import android.app.NotificationManager
import android.os.Bundle

// Android 端的后台运行通知实现。
// 设备运行与积分任务运行通知只在应用处于后台时展示：应用回到前台会自动收起，
// 再次进入后台且运行仍未结束时依据最近一次上报的状态重新展示。
// Android 16（API 36）及以上使用进度样式（Live Updates）并请求提升为常驻动态。
actual object RunNotifications {
    private var initialized = false
    private var callbacksRegistered = false

    @Volatile
    private var foreground = true

    private data class DeviceState(
        val deviceId: String,
        val deviceName: String,
    )

    // 仅在设备运行中保留状态，用于下一次进入后台时重新展示
    private val deviceStates = LinkedHashMap<String, DeviceState>()

    private var taskGained: Int? = null

    actual fun ensureInitialized() {
        if (initialized) return
        initialized = true
        runCatching { registerLifecycle() }
    }

    actual fun updateDevice(
        deviceId: String,
        deviceName: String,
        running: Boolean,
    ) {
        if (deviceId.isBlank()) return
        ensureInitialized()
        if (running) {
            val state = DeviceState(deviceId, deviceName)
            deviceStates[deviceId] = state
            if (!foreground) postDevice(state)
        } else {
            // 停止即收起通知，不保留状态
            deviceStates.remove(deviceId)
            cancelNotification(deviceNotificationId(deviceId))
        }
    }

    actual fun updateTask(gained: Int) {
        ensureInitialized()
        taskGained = gained.coerceAtLeast(0)
        renderTask()
    }

    actual fun removeTask() {
        taskGained = null
        cancelNotification(TASK_NOTIFICATION_ID)
    }

    private fun registerLifecycle() {
        if (callbacksRegistered) return
        val application = ApplicationContext.instance.applicationContext as? Application ?: return
        callbacksRegistered = true
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                private var startedActivities = 0

                override fun onActivityStarted(activity: Activity) {
                    startedActivities++
                    if (startedActivities == 1) setForeground(true)
                }

                override fun onActivityStopped(activity: Activity) {
                    startedActivities = (startedActivities - 1).coerceAtLeast(0)
                    if (startedActivities == 0) setForeground(false)
                }

                override fun onActivityCreated(
                    activity: Activity,
                    savedInstanceState: Bundle?,
                ) {}

                override fun onActivityResumed(activity: Activity) {}

                override fun onActivityPaused(activity: Activity) {}

                override fun onActivitySaveInstanceState(
                    activity: Activity,
                    outState: Bundle,
                ) {}

                override fun onActivityDestroyed(activity: Activity) {}
            },
        )
    }

    private fun setForeground(value: Boolean) {
        if (foreground == value) return
        foreground = value
        if (value) {
            cancelRunNotifications()
        } else {
            refreshAll()
        }
    }

    private fun refreshAll() {
        deviceStates.values.forEach { postDevice(it) }
        if (taskGained != null) renderTask()
    }

    private fun cancelRunNotifications() {
        // 通过分组找到本应用展示过的运行通知，即使进程被回收也能收起
        val context = ApplicationContext.instance
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager != null) {
            runCatching {
                manager.activeNotifications.forEach { statusBarNotification ->
                    if (statusBarNotification.packageName == context.packageName &&
                        (
                            statusBarNotification.notification.group == RUN_NOTIFICATION_GROUP ||
                                statusBarNotification.id == TASK_NOTIFICATION_ID
                        )
                    ) {
                        manager.cancel(statusBarNotification.tag, statusBarNotification.id)
                    }
                }
            }
        }
        deviceStates.keys.forEach { cancelNotification(deviceNotificationId(it)) }
        cancelNotification(TASK_NOTIFICATION_ID)
    }

    private fun postDevice(state: DeviceState) {
        val context = ApplicationContext.instance
        ensureRunChannels(context)
        if (!canPostNotification(context, CHANNEL_DEVICE)) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        runCatching {
            manager.notify(deviceNotificationId(state.deviceId), buildDeviceNotification(context, state.deviceId, state.deviceName))
        }
    }

    private fun renderTask() {
        val gained = taskGained ?: return
        if (foreground) {
            cancelNotification(TASK_NOTIFICATION_ID)
            return
        }
        val context = ApplicationContext.instance
        ensureRunChannels(context)
        if (!canPostNotification(context, CHANNEL_TASK)) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        runCatching {
            manager.notify(TASK_NOTIFICATION_ID, buildTaskNotification(context, gained))
        }
    }
}
