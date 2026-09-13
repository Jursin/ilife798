package com.github.ilife798.update

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.github.ilife798.ActivityHolder
import com.github.ilife798.ApplicationContext
import com.github.ilife798.logDebug
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout

actual fun createUpdateHttpClient(): HttpClient =
    HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 60_000
        }
    }

actual fun currentAbis(): List<String> = Build.SUPPORTED_ABIS.toList()

actual fun canInstallPackages(): Boolean = ApplicationContext.instance.packageManager.canRequestPackageInstalls()

actual fun openInstallPermissionSettings() {
    val context = ApplicationContext.instance
    val packageUri = "package:${context.packageName}".toUri()
    val intents =
        listOf(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageUri),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri),
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
        val file = java.io.File(filePath)
        if (!file.exists()) return false
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
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

private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 9001

actual fun requestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val activity = ActivityHolder.current ?: return
    if (activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
    try {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            NOTIFICATION_PERMISSION_REQUEST_CODE,
        )
    } catch (e: Exception) {
        logDebug("ILife798", "requestNotificationPermission: ${e.message}")
    }
}
