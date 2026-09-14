package com.github.ilife798

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

// 设备/积分任务运行期间维持前台服务并持有 Partial WakeLock，
// 避免锁屏或切到后台后进程被厂商省电策略冻结、网络请求被中断。
// 运行状态与通知摘要由 [RunNotifications] 通过 Intent 传入，服务本身不持有状态。
class RunForegroundService : Service() {
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
        ensureRunChannels(this)
        handler.removeCallbacks(renewWakeLock)
        renewWakeLock.run()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildRunForegroundNotification(this, intent?.getStringExtra(EXTRA_SUMMARY).orEmpty()),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
        return START_NOT_STICKY
    }

    private fun acquireWakeLock() {
        val lock =
            wakeLock
                ?: getSystemService(PowerManager::class.java)
                    ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ilife798:run")
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
        private const val NOTIFICATION_ID = 2401
        private const val EXTRA_SUMMARY = "com.github.ilife798.extra.RUN_SUMMARY"
        private const val WAKE_LOCK_TIMEOUT_MS = 10 * 60 * 1000L
        private const val WAKE_LOCK_RENEW_INTERVAL_MS = 9 * 60 * 1000L

        // 运行中启动/更新前台服务；无运行任务时停止服务并释放 WakeLock。
        fun refresh(
            context: Context,
            active: Boolean,
            summary: String,
        ) {
            val intent = Intent(context, RunForegroundService::class.java)
            if (!active) {
                runCatching { context.stopService(intent) }
                return
            }
            runCatching {
                ContextCompat.startForegroundService(context, intent.putExtra(EXTRA_SUMMARY, summary))
            }.onFailure { logDebug("ILife798", "RunForegroundService.refresh: ${it.message}") }
        }
    }
}
