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
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.github.ilife798.ActivityHolder
import com.github.ilife798.ApplicationContext
import com.github.ilife798.logDebug
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual fun createUpdateHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(HttpTimeout) {
        requestTimeoutMillis = 60_000
        connectTimeoutMillis = 30_000
        socketTimeoutMillis = 60_000
    }
}

actual fun currentAbis(): List<String> = Build.SUPPORTED_ABIS.toList()

actual fun canInstallPackages(): Boolean =
    ApplicationContext.instance.packageManager.canRequestPackageInstalls()

actual fun openInstallPermissionSettings() {
    val context = ApplicationContext.instance
    val packageUri = "package:${context.packageName}".toUri()
    val intents = listOf(
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageUri),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
    )
    for (intent in intents) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
            return
        } catch (e: Exception) {
            logDebug("ILife798", "openInstallPermissionSettings: ${e.message}")
        }
    }
}

actual fun installApk(filePath: String): Boolean {
    return try {
        val context = ApplicationContext.instance
        val file = File(filePath)
        if (!file.exists()) return false
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        logDebug("ILife798", "installApk: ${e.message}")
        false
    }
}

private fun apkTargetFile(): File {
    val context = ApplicationContext.instance
    val dir = context.getExternalFilesDir(null) ?: context.cacheDir
    return File(dir, "ilife798-update.apk")
}

actual suspend fun downloadApkToFile(url: String, onProgress: (Float) -> Unit): String =
    withContext(Dispatchers.IO) {
        val target = apkTargetFile()
        if (target.exists()) target.delete()
        val client = createUpdateHttpClient()
        client.prepareGet(url).execute { response ->
            if (!response.status.isSuccess()) {
                error("HTTP ${response.status.value}")
            }
            val total = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: -1L
            val channel = response.bodyAsChannel()
            val buffer = ByteArray(64 * 1024)
            var downloaded = 0L
            var lastPercent = -1
            target.outputStream().use { out ->
                while (true) {
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read == -1) break
                    if (read > 0) {
                        out.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            val percent = ((downloaded * 100) / total).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent / 100f)
                            }
                        }
                    }
                }
            }
        }
        if (!target.exists() || target.length() == 0L) {
            error("下载失败")
        }
        onProgress(1f)
        target.absolutePath
    }

private const val UPDATE_CHANNEL_ID = "app_update"
private const val UPDATE_NOTIFICATION_ID = 1001
private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 9001

const val ACTION_SHOW_UPDATE = "com.github.ilife798.action.SHOW_UPDATE"

private fun ensureUpdateChannel(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (manager.getNotificationChannel(UPDATE_CHANNEL_ID) == null) {
        manager.createNotificationChannel(
            NotificationChannel(
                UPDATE_CHANNEL_ID,
                "应用更新",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "下载更新期间显示下载进度"
                setShowBadge(false)
            }
        )
    }
}

actual fun requestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val activity = ActivityHolder.current ?: return
    if (activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
    try {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            NOTIFICATION_PERMISSION_REQUEST_CODE
        )
    } catch (e: Exception) {
        logDebug("ILife798", "requestNotificationPermission: ${e.message}")
    }
}

@SuppressLint("MissingPermission")
actual fun showUpdateProgressNotification(progress: Float) {
    val context = ApplicationContext.instance
    ensureUpdateChannel(context)
    val percent = (progress.coerceIn(0f, 1f) * 100).toInt()
    val contentIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { launchIntent ->
        launchIntent.action = ACTION_SHOW_UPDATE
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    val notification = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
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

actual fun deleteDownloadedApk() {
    try {
        apkTargetFile().takeIf { it.exists() }?.delete()
    } catch (e: Exception) {
        logDebug("ILife798", "deleteDownloadedApk: ${e.message}")
    }
    try {
        val fallback = File(ApplicationContext.instance.cacheDir, "ilife798-update.apk")
        if (fallback.exists()) fallback.delete()
    } catch (e: Exception) {
        logDebug("ILife798", "deleteDownloadedApk cache: ${e.message}")
    }
}

actual fun downloadedApkPathIfValid(sha256: String?, size: Long): String? {
    val file = apkTargetFile()
    if (!file.exists() || file.length() == 0L) return null
    if (!sha256.isNullOrBlank()) {
        val actual = file.sha256Hex()
        return if (actual != null && actual.equals(sha256, ignoreCase = true)) file.absolutePath else null
    }
    if (size > 0L) {
        return if (file.length() == size) file.absolutePath else null
    }
    return null
}

private fun File.sha256Hex(): String? = try {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
    }
    digest.digest().joinToString("") { "%02x".format(it) }
} catch (e: Exception) {
    logDebug("ILife798", "sha256Hex: ${e.message}")
    null
}
