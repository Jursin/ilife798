package com.github.ilife798.update

import io.ktor.client.HttpClient

// 用于检查更新与下载的独立 HttpClient，不携带业务接口的默认请求头。
expect fun createUpdateHttpClient(): HttpClient

// 本机 ABI 列表，按优先级排序；不支持安装 Android 安装包的系统返回空列表。
expect fun currentAbis(): List<String>

// 是否已授权本应用安装未知来源应用。
expect fun canInstallPackages(): Boolean

// 跳转到系统的“安装未知应用”授权页面。
expect fun openInstallPermissionSettings()

// 调用系统安装器安装指定路径的安装包。返回是否成功发起安装。
expect fun installApk(filePath: String): Boolean

// 下载安装包到本地临时文件，通过 [onProgress] 回调 0f..1f 的进度，返回文件路径。
expect suspend fun downloadApkToFile(
    url: String,
    onProgress: (Float) -> Unit,
): String

// 请求通知权限（Android 13+ 需要）。
expect fun requestNotificationPermission()

// 在通知栏显示下载进度。
expect fun showUpdateProgressNotification(progress: Float)

// 取消下载进度通知。
expect fun cancelUpdateProgressNotification()

// 删除已下载的安装包文件。
expect fun deleteDownloadedApk()

// 若已下载的安装包与期望一致（优先比对 sha256，其次比对文件大小）则返回其路径，否则返回 null。
// [sha256] 为期望的十六进制摘要，[size] 为期望字节数（未知时传 0）。
expect fun downloadedApkPathIfValid(
    sha256: String?,
    size: Long,
): String?
