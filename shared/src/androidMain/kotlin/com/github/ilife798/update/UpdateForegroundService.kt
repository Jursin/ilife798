package com.github.ilife798.update

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.github.ilife798.logDebug

// 应用更新下载期间维持前台服务并持有 Partial WakeLock，
// 避免锁屏或切到后台后下载被厂商省电策略冻结、中断。
// 通知由 [showUpdateProgressNotification] 构建后经 Intent 传入。
class UpdateForegroundService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())

    // WakeLock 带超时获取并由该任务定期续期，服务异常退出时系统也能自动清理。
    private val renewWakeLock =
        object : Runnable {
            override fun run() {
                acquireWakeLock()
                handler.postDelayed(this, WAKE_LOCK_RENEW_INTERVAL_MS)
            }
        }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        ensureUpdateChannel(this)
        val notification = intent?.runNotification()
        if (intent == null || notification == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        handler.removeCallbacks(renewWakeLock)
        renewWakeLock.run()
        ServiceCompat.startForeground(
            this,
            UPDATE_NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun Intent.runNotification(): Notification? = getParcelableExtra(EXTRA_NOTIFICATION)

    private fun acquireWakeLock() {
        val lock =
            wakeLock
                ?: getSystemService(PowerManager::class.java)
                    ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ilife798:update")
                    ?.apply { setReferenceCounted(false) }
                ?: return
        wakeLock = lock
        runCatching { lock.acquire(WAKE_LOCK_TIMEOUT_MS) }
    }

    override fun onDestroy() {
        handler.removeCallbacks(renewWakeLock)
        wakeLock?.let { runCatching { if (it.isHeld) it.release() } }
        wakeLock = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val EXTRA_NOTIFICATION = "com.github.ilife798.extra.UPDATE_NOTIFICATION"
        private const val WAKE_LOCK_TIMEOUT_MS = 10 * 60 * 1000L
        private const val WAKE_LOCK_RENEW_INTERVAL_MS = 9 * 60 * 1000L

        // 下载中启动/更新前台服务；下载结束或停止时停止服务并释放 WakeLock。
        fun refresh(
            context: Context,
            active: Boolean,
            notification: Notification?,
        ) {
            val intent = Intent(context, UpdateForegroundService::class.java)
            if (!active || notification == null) {
                runCatching { context.stopService(intent) }
                return
            }
            intent.putExtra(EXTRA_NOTIFICATION, notification)
            runCatching {
                ContextCompat.startForegroundService(context, intent)
            }.onFailure { logDebug("ILife798", "UpdateForegroundService.refresh: ${it.message}") }
        }
    }
}
