package com.github.ilife798.update

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

// 更新检查用独立 HttpClient；Darwin 引擎不支持独立的 connect/socket 超时，
// 仅设置与 Android 同语义的 60s 请求超时。
actual fun createUpdateHttpClient(): HttpClient =
    HttpClient(Darwin) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
        }
    }

// iOS 端不支持 APK 安装/应用内更新；系列接口按设计提供安全空实现。
actual fun currentAbis(): List<String> = emptyList()

actual fun canInstallPackages(): Boolean = false

actual fun openInstallPermissionSettings() {}

actual fun installApk(filePath: String): Boolean = false

actual suspend fun downloadApkToFile(
    url: String,
    onProgress: (Float) -> Unit,
): String {
    onProgress(0f)
    return ""
}

actual suspend fun requestNotificationPermission() {}

actual fun showUpdateProgressNotification(progress: Float) {}

actual fun cancelUpdateProgressNotification() {}

actual fun deleteDownloadedApk() {}

actual fun downloadedApkPathIfValid(
    sha256: String?,
    size: Long,
): String? = null

// 更新跳浏览器打开 ipa 链接，找不到则回退发布页。
actual fun supportsReleasePageHandoff(): Boolean = true

actual fun openReleasePage(url: String): Boolean =
    runCatching {
        val nsUrl = NSURL(string = url)
        UIApplication.sharedApplication.openURL(nsUrl)
    }.getOrDefault(false)
