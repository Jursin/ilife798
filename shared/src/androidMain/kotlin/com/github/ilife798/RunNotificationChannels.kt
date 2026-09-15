package com.github.ilife798

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

internal const val CHANNEL_DEVICE = "device_running"
internal const val CHANNEL_TASK = "task_running"

internal fun ensureRunChannels(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    if (manager.getNotificationChannel(CHANNEL_DEVICE) == null) {
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DEVICE,
                "设备运行状态",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "设备运行期间显示运行状态与停止操作"
                setShowBadge(false)
            },
        )
    }
    if (manager.getNotificationChannel(CHANNEL_TASK) == null) {
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TASK,
                "积分任务运行状态",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "积分任务运行期间显示已获得积分与停止操作"
                setShowBadge(false)
            },
        )
    }
}

internal fun canPostNotification(
    context: Context,
    channelId: String,
): Boolean {
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

internal fun cancelNotification(id: Int) {
    runCatching {
        ApplicationContext.instance.getSystemService(NotificationManager::class.java)?.cancel(id)
    }
}
