package com.github.ilife798.update

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout

actual fun createUpdateHttpClient(): HttpClient = HttpClient(Darwin) {
    install(HttpTimeout) {
        requestTimeoutMillis = 60_000
        connectTimeoutMillis = 30_000
        socketTimeoutMillis = 60_000
    }
}

actual fun currentAbis(): List<String> = emptyList()

actual fun canInstallPackages(): Boolean = false

actual fun openInstallPermissionSettings() {}

actual fun installApk(filePath: String): Boolean = false

actual suspend fun downloadApkToFile(url: String, onProgress: (Float) -> Unit): String {
    throw UnsupportedOperationException("iOS does not support installing Android packages")
}

actual fun requestNotificationPermission() {}

actual fun showUpdateProgressNotification(progress: Float) {}

actual fun cancelUpdateProgressNotification() {}

actual fun deleteDownloadedApk() {}

actual fun downloadedApkPathIfValid(sha256: String?, size: Long): String? = null
