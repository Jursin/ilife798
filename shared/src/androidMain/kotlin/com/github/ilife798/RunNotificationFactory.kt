package com.github.ilife798

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi

internal const val RUN_NOTIFICATION_GROUP = "com.github.ilife798.run"
internal const val DEVICE_ID_BASE = 2201
internal const val DEVICE_ID_RANGE = 900
internal const val TASK_NOTIFICATION_ID = 2301

private const val REQUEST_DEVICE_CONTENT = 4101
private const val REQUEST_TASK_CONTENT = 4103
private const val REQUEST_STOP_TASKS = 4104
private const val REQUEST_STOP_DEVICE_BASE = 4200
private const val REQUEST_STOP_DEVICE_RANGE = 500

private const val CRITICAL_TEXT_RUNNING = "运行中"
private const val PROGRESS_COLOR = -12877066

internal fun deviceNotificationId(deviceId: String): Int = DEVICE_ID_BASE + ((deviceId.hashCode() and 0x7FFFFFFF) % DEVICE_ID_RANGE)

internal fun buildDeviceNotification(
    context: Context,
    deviceId: String,
    deviceName: String,
): Notification {
    val label = deviceName.takeIf { it.isNotBlank() } ?: "设备"
    val builder =
        Notification
            .Builder(context, CHANNEL_DEVICE)
            .setSmallIcon(com.github.ilife798.shared.R.drawable.ic_notify_device)
            .setContentTitle("设备运行中")
            .setContentText("$label · 编号 $deviceId")
            .setContentIntent(
                activityPendingIntent(
                    context,
                    REQUEST_DEVICE_CONTENT,
                    IntentActions.ACTION_OPEN_HOME,
                    deviceId = null,
                ),
            ).setCategory(Notification.CATEGORY_PROGRESS)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setGroup(RUN_NOTIFICATION_GROUP)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(true)
            .setAutoCancel(false)
    builder.addAction(stopDeviceAction(context, deviceId))
    applyRunProgress(builder)
    return builder.build()
}

internal fun buildTaskNotification(
    context: Context,
    gained: Int,
): Notification {
    val stopIntent =
        activityPendingIntent(
            context,
            REQUEST_STOP_TASKS,
            IntentActions.ACTION_STOP_TASKS,
            deviceId = null,
        )
    val builder =
        Notification
            .Builder(context, CHANNEL_TASK)
            .setSmallIcon(com.github.ilife798.shared.R.drawable.ic_notify_task)
            .setContentTitle("积分任务运行中")
            .setContentText("已获得 $gained 积分")
            .setContentIntent(
                activityPendingIntent(
                    context,
                    REQUEST_TASK_CONTENT,
                    IntentActions.ACTION_OPEN_TASKS,
                    deviceId = null,
                ),
            ).setCategory(Notification.CATEGORY_PROGRESS)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setGroup(RUN_NOTIFICATION_GROUP)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(true)
            .addAction(
                Notification.Action
                    .Builder(
                        Icon.createWithResource(context, android.R.drawable.ic_menu_close_clear_cancel),
                        "停止",
                        stopIntent,
                    ).build(),
            )
    applyRunProgress(builder)
    return builder.build()
}

private fun stopDeviceAction(
    context: Context,
    deviceId: String,
): Notification.Action {
    val requestCode =
        REQUEST_STOP_DEVICE_BASE +
            ((deviceId.hashCode() and 0x7FFFFFFF) % REQUEST_STOP_DEVICE_RANGE)
    val pendingIntent =
        activityPendingIntent(
            context,
            requestCode,
            IntentActions.ACTION_STOP_DEVICE,
            deviceId,
        )
    return Notification.Action
        .Builder(
            Icon.createWithResource(context, android.R.drawable.ic_menu_close_clear_cancel),
            "停止",
            pendingIntent,
        ).build()
}

private fun applyRunProgress(builder: Notification.Builder) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        applyProgressStyle(builder)
    } else {
        builder.setProgress(0, 0, true)
    }
}

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
private fun applyProgressStyle(builder: Notification.Builder) {
    val style =
        Notification
            .ProgressStyle()
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
    deviceId: String?,
): PendingIntent {
    val launchIntent =
        context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(Intent.ACTION_MAIN).setPackage(context.packageName)
    launchIntent.action = action
    launchIntent.addFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_SINGLE_TOP or
            Intent.FLAG_ACTIVITY_CLEAR_TOP,
    )
    if (deviceId != null) {
        launchIntent.putExtra(IntentActions.EXTRA_DEVICE_ID, deviceId)
    }
    return PendingIntent.getActivity(
        context,
        requestCode,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
