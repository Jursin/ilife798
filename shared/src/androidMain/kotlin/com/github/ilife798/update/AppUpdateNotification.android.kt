package com.github.ilife798.update

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.ilife798.ApplicationContext
import com.github.ilife798.IntentActions
import com.github.ilife798.logDebug

private const val UPDATE_CHANNEL_ID = "app_update"
private const val UPDATE_NOTIFICATION_ID = 1001

private fun ensureUpdateChannel(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (manager.getNotificationChannel(UPDATE_CHANNEL_ID) == null) {
        manager.createNotificationChannel(
            NotificationChannel(
                UPDATE_CHANNEL_ID,
                "应用更新",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "下载更新期间显示下载进度"
                setShowBadge(false)
            },
        )
    }
}

@SuppressLint("MissingPermission")
actual fun showUpdateProgressNotification(progress: Float) {
    val context = ApplicationContext.instance
    ensureUpdateChannel(context)
    val percent = (progress.coerceIn(0f, 1f) * 100).toInt()
    val contentIntent =
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { launchIntent ->
            launchIntent.action = IntentActions.ACTION_SHOW_UPDATE
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    val notification =
        NotificationCompat
            .Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("正在下载 · $percent%")
            .setContentIntent(contentIntent)
            .setProgress(100, percent, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    try {
        NotificationManagerCompat.from(context).notify(UPDATE_NOTIFICATION_ID, notification)
    } catch (e: Exception) {
        logDebug("ILife798", "showUpdateProgressNotification: ${e.message}")
    }
}

actual fun cancelUpdateProgressNotification() {
    try {
        NotificationManagerCompat.from(ApplicationContext.instance).cancel(UPDATE_NOTIFICATION_ID)
    } catch (e: Exception) {
        logDebug("ILife798", "cancelUpdateProgressNotification: ${e.message}")
    }
}
