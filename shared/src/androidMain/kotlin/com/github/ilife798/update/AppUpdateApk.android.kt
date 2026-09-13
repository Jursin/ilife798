package com.github.ilife798.update

import com.github.ilife798.ApplicationContext
import com.github.ilife798.logDebug
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

internal fun apkTargetFile(): File {
    val context = ApplicationContext.instance
    val dir = context.getExternalFilesDir(null) ?: context.cacheDir
    return File(dir, "ilife798-update.apk")
}

actual suspend fun downloadApkToFile(
    url: String,
    onProgress: (Float) -> Unit,
): String =
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

actual fun downloadedApkPathIfValid(
    sha256: String?,
    size: Long,
): String? {
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

private fun File.sha256Hex(): String? =
    try {
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
