package com.github.ilife798

import android.Manifest
import android.app.Activity
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi

/**
 * Android 端的后台运行通知实现。
 *
 * 设备运行与积分任务运行通知只在应用处于后台时展示：应用回到前台会自动收起，
 * 再次进入后台且运行仍未结束时会依据最近一次上报的状态重新展示。
 * Android 16（API 36）及以上使用进度样式（Live Updates）并请求提升为常驻动态。
 */
actual object RunNotifications {
    private const val CHANNEL_DEVICE = "device_running"
    private const val CHANNEL_TASK = "task_running"
    private const val RUN_NOTIFICATION_GROUP = "com.github.ilife798.run"
    private const val DEVICE_ID_BASE = 2201
    private const val DEVICE_ID_RANGE = 900
    private const val TASK_NOTIFICATION_ID = 2301

    private const val REQUEST_DEVICE_CONTENT = 4101
    private const val REQUEST_TASK_CONTENT = 4103
    private const val REQUEST_STOP_TASKS = 4104
    private const val REQUEST_STOP_DEVICE_BASE = 4200
    private const val REQUEST_STOP_DEVICE_RANGE = 500

    private const val CRITICAL_TEXT_RUNNING = "运行中"
    private const val PROGRESS_COLOR = -12877066

    private var initialized = false
    private var callbacksRegistered = false

    @Volatile
    private var foreground = true

    private data class DeviceState(val deviceId: String, val deviceName: String)

    // 仅在设备运行中保留状态，用于下一次进入后台时重新展示
    private val deviceStates = LinkedHashMap<String, DeviceState>()

    private var taskGained: Int? = null

    actual fun ensureInitialized() {
        if (initialized) return
        initialized = true
        runCatching { registerLifecycle() }
    }

    actual fun updateDevice(deviceId: String, deviceName: String, running: Boolean) {
        if (deviceId.isBlank()) return
        ensureInitialized()
        if (running) {
            val state = DeviceState(deviceId, deviceName)
            deviceStates[deviceId] = state
            if (!foreground) postDevice(state)
        } else {
            // 停止即收起通知，不保留状态
            deviceStates.remove(deviceId)
            cancel(deviceNotificationId(deviceId))
        }
    }

    actual fun updateTask(gained: Int) {
        ensureInitialized()
        taskGained = gained.coerceAtLeast(0)
        renderTask()
    }

    actual fun removeTask() {
        taskGained = null
        cancel(TASK_NOTIFICATION_ID)
    }

    private fun registerLifecycle() {
        if (callbacksRegistered) return
        val application = ApplicationContext.instance.applicationContext as? Application ?: return
        callbacksRegistered = true
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            private var startedActivities = 0

            override fun onActivityStarted(activity: Activity) {
                startedActivities++
                if (startedActivities == 1) setForeground(true)
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivities = (startedActivities - 1).coerceAtLeast(0)
                if (startedActivities == 0) setForeground(false)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
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
                        (statusBarNotification.notification.group == RUN_NOTIFICATION_GROUP ||
                            statusBarNotification.id == TASK_NOTIFICATION_ID)
                    ) {
                        manager.cancel(statusBarNotification.tag, statusBarNotification.id)
                    }
                }
            }
        }
        deviceStates.keys.forEach { cancel(deviceNotificationId(it)) }
        cancel(TASK_NOTIFICATION_ID)
    }

    // --- 设备运行通知 ---

    private fun deviceNotificationId(deviceId: String): Int =
        DEVICE_ID_BASE + ((deviceId.hashCode() and 0x7FFFFFFF) % DEVICE_ID_RANGE)

    private fun postDevice(state: DeviceState) {
        val context = ApplicationContext.instance
        ensureChannels(context)
        if (!canPost(context, CHANNEL_DEVICE)) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        runCatching {
            manager.notify(deviceNotificationId(state.deviceId), buildDeviceNotification(context, state))
        }
    }

    private fun buildDeviceNotification(context: Context, state: DeviceState): Notification {
        val label = state.deviceName.takeIf { it.isNotBlank() } ?: "设备"
        val builder = Notification.Builder(context, CHANNEL_DEVICE)
            .setSmallIcon(com.github.ilife798.shared.R.drawable.ic_notify_device)
            .setContentTitle("设备运行中")
            .setContentText("$label · 编号 ${state.deviceId}")
            .setContentIntent(
                activityPendingIntent(
                    context,
                    REQUEST_DEVICE_CONTENT,
                    RunNotificationIntents.ACTION_OPEN_HOME,
                    deviceId = null
                )
            )
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setGroup(RUN_NOTIFICATION_GROUP)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(true)
            .setAutoCancel(false)
        builder.addAction(stopDeviceAction(context, state.deviceId))
        applyProgress(builder)
        return builder.build()
    }

    private fun stopDeviceAction(context: Context, deviceId: String): Notification.Action {
        val requestCode = REQUEST_STOP_DEVICE_BASE +
            ((deviceId.hashCode() and 0x7FFFFFFF) % REQUEST_STOP_DEVICE_RANGE)
        val pendingIntent = activityPendingIntent(
            context,
            requestCode,
            RunNotificationIntents.ACTION_STOP_DEVICE,
            deviceId
        )
        return Notification.Action.Builder(
            Icon.createWithResource(context, android.R.drawable.ic_menu_close_clear_cancel),
            "停止",
            pendingIntent
        ).build()
    }

    // --- 积分任务运行通知 ---

    private fun renderTask() {
        val gained = taskGained ?: return
        if (foreground) {
            cancel(TASK_NOTIFICATION_ID)
            return
        }
        val context = ApplicationContext.instance
        ensureChannels(context)
        if (!canPost(context, CHANNEL_TASK)) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        runCatching {
            manager.notify(TASK_NOTIFICATION_ID, buildTaskNotification(context, gained))
        }
    }

    private fun buildTaskNotification(context: Context, gained: Int): Notification {
        val stopIntent = activityPendingIntent(
            context,
            REQUEST_STOP_TASKS,
            RunNotificationIntents.ACTION_STOP_TASKS,
            deviceId = null
        )
        val builder = Notification.Builder(context, CHANNEL_TASK)
            .setSmallIcon(com.github.ilife798.shared.R.drawable.ic_notify_task)
            .setContentTitle("积分任务运行中")
            .setContentText("已获得 $gained 积分")
            .setContentIntent(
                activityPendingIntent(
                    context,
                    REQUEST_TASK_CONTENT,
                    RunNotificationIntents.ACTION_OPEN_TASKS,
                    deviceId = null
                )
            )
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setGroup(RUN_NOTIFICATION_GROUP)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(true)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, android.R.drawable.ic_menu_close_clear_cancel),
                    "停止",
                    stopIntent
                ).build()
            )
        applyProgress(builder)
        return builder.build()
    }

    // --- 公共构建逻辑 ---

    private fun applyProgress(builder: Notification.Builder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            applyProgressStyle(builder)
        } else {
            builder.setProgress(0, 0, true)
        }
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun applyProgressStyle(builder: Notification.Builder) {
        val style = Notification.ProgressStyle()
            .setStyledByProgress(false)
            .setProgressIndeterminate(true)
            .setProgressSegments(listOf(Notification.ProgressStyle.Segment(100).setColor(PROGRESS_COLOR)))
        builder.style = style
        builder.setShortCriticalText(CRITICAL_TEXT_RUNNING)
        if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1) {
            builder.setRequestPromotedOngoing(true)
        }
    }

    private fun activityPendingIntent(
        context: Context,
        requestCode: Int,
        action: String,
        deviceId: String?
    ): PendingIntent {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(Intent.ACTION_MAIN).setPackage(context.packageName)
        launchIntent.action = action
        launchIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        )
        if (deviceId != null) {
            launchIntent.putExtra(RunNotificationIntents.EXTRA_DEVICE_ID, deviceId)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_DEVICE) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_DEVICE,
                    "设备运行状态",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "设备运行期间显示运行状态与停止操作"
                    setShowBadge(false)
                }
            )
        }
        if (manager.getNotificationChannel(CHANNEL_TASK) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_TASK,
                    "积分任务运行状态",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "积分任务运行期间显示已获得积分与停止操作"
                    setShowBadge(false)
                }
            )
        }
    }

    private fun canPost(context: Context, channelId: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        if (!manager.areNotificationsEnabled()) return false
        val channel = manager.getNotificationChannel(channelId) ?: return false
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    private fun cancel(id: Int) {
        runCatching {
            ApplicationContext.instance.getSystemService(NotificationManager::class.java)?.cancel(id)
        }
    }
}
