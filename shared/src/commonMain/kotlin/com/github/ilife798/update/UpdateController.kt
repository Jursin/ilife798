package com.github.ilife798.update

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.ilife798.AppStorage
import com.github.ilife798.StorageKeys
import com.github.ilife798.getAppVersion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// 应用内更新：检查、下载、安装以及相关偏好设置的状态机。
class UpdateController(
    private val scope: CoroutineScope,
    private val onToast: (String) -> Unit,
    private val onDismissToast: () -> Unit,
    private val onLogError: (String, Exception) -> Unit,
) {
    var dialog by mutableStateOf<UpdateDialogState>(UpdateDialogState.None)
        private set

    var githubProxyUrl by mutableStateOf("")
        private set

    var developerMode by mutableStateOf(false)
        private set

    var checkUpdateOnStart by mutableStateOf(true)
        private set

    private var pendingUpdateUrl = ""
    private var pendingUpdateSha256: String? = null
    private var pendingUpdateSize = 0L
    private var downloadedApkPath: String? = null
    private var checkingUpdate = false
    private var downloadingUpdate = false
    private var progressDialogDismissed = false
    private var lastUpdateProgress = 0f
    private var updateJob: Job? = null

    fun loadSettings() {
        try {
            val storage = AppStorage.instance
            githubProxyUrl = storage.getString(StorageKeys.GITHUB_PROXY) ?: ""
            developerMode = storage.getBoolean(StorageKeys.DEVELOPER_MODE, false)
            checkUpdateOnStart = storage.getBoolean(StorageKeys.CHECK_UPDATE_ON_START, true)
        } catch (e: Exception) {
            onLogError("loadUpdateSettings", e)
        }
    }

    fun checkForUpdate(silent: Boolean = false) {
        if (downloadingUpdate) {
            // 正在下载时再次点击：重新弹出下载进度对话框（同时收起通知）
            reopenUpdateProgressDialog()
            return
        }
        if (checkingUpdate) return
        checkingUpdate = true
        if (!silent) onToast("正在检查更新")
        scope.launch {
            try {
                val release = AppUpdate.fetchLatestRelease()
                val remoteVersion = release.tagName.removePrefix("v").trim()
                if (AppUpdate.compareVersions(remoteVersion, getAppVersion()) > 0) {
                    onDismissToast()
                    val asset = AppUpdate.selectAsset(release, currentAbis())
                    if (asset == null || asset.browserDownloadUrl.isEmpty()) {
                        if (!silent) onToast("暂无本设备安装包")
                        return@launch
                    }
                    pendingUpdateUrl = asset.browserDownloadUrl
                    pendingUpdateSha256 = asset.digest?.substringAfter(':')?.takeIf { it.isNotBlank() }
                    pendingUpdateSize = asset.size
                    dialog = UpdateDialogState.Available(remoteVersion)
                } else if (!silent) {
                    onDismissToast()
                    onToast("已是最新版")
                }
            } catch (e: Exception) {
                onLogError("checkForUpdate", e)
                if (!silent) {
                    onDismissToast()
                    onToast("检查更新失败")
                }
            } finally {
                checkingUpdate = false
            }
        }
    }

    fun startUpdate() {
        if (downloadingUpdate) return
        val url = pendingUpdateUrl
        if (url.isEmpty()) {
            dialog = UpdateDialogState.None
            return
        }
        // 已下载的安装包与目标一致时直接安装，无需重复下载
        val existing = downloadedApkPathIfValid(pendingUpdateSha256, pendingUpdateSize)
        if (existing != null) {
            downloadedApkPath = existing
            progressDialogDismissed = false
            afterDownload(existing)
            return
        }
        downloadingUpdate = true
        progressDialogDismissed = false
        lastUpdateProgress = 0f
        downloadedApkPath = null
        dialog = UpdateDialogState.Downloading(0f)
        updateJob =
            scope.launch {
                try {
                    val path =
                        downloadApkToFile(AppUpdate.applyProxy(url, githubProxyUrl)) { progress ->
                            lastUpdateProgress = progress
                            if (progressDialogDismissed) {
                                showUpdateProgressNotification(progress)
                            } else {
                                dialog = UpdateDialogState.Downloading(progress)
                            }
                        }
                    downloadedApkPath = path
                    cancelUpdateProgressNotification()
                    afterDownload(path)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    onLogError("startUpdate", e)
                    cancelUpdateProgressNotification()
                    onToast("下载失败：${e.message}")
                    dialog = UpdateDialogState.None
                } finally {
                    downloadingUpdate = false
                    updateJob = null
                }
            }
    }

    fun hideUpdateProgressDialog() {
        progressDialogDismissed = true
        dialog = UpdateDialogState.None
        requestNotificationPermission()
        showUpdateProgressNotification(lastUpdateProgress)
    }

    fun reopenUpdateProgressDialog() {
        if (!downloadingUpdate) return
        progressDialogDismissed = false
        cancelUpdateProgressNotification()
        dialog = UpdateDialogState.Downloading(lastUpdateProgress)
    }

    fun stopUpdate() {
        updateJob?.cancel()
        updateJob = null
        downloadingUpdate = false
        progressDialogDismissed = false
        lastUpdateProgress = 0f
        downloadedApkPath = null
        cancelUpdateProgressNotification()
        deleteDownloadedApk()
        dialog = UpdateDialogState.None
    }

    private fun afterDownload(path: String) {
        if (canInstallPackages()) {
            installApkOrPrompt(path)
        } else {
            openInstallPermissionSettings()
            dialog = UpdateDialogState.AwaitingPermission
        }
    }

    private fun installApkOrPrompt(path: String) {
        dialog =
            if (installApk(path)) {
                UpdateDialogState.None
            } else {
                UpdateDialogState.NeedPermission
            }
    }

    fun onAppResumed() {
        if (dialog != UpdateDialogState.AwaitingPermission) return
        val path = downloadedApkPath ?: return
        if (canInstallPackages()) {
            installApkOrPrompt(path)
        } else {
            dialog = UpdateDialogState.NeedPermission
        }
    }

    fun openInstallSettings() {
        val path = downloadedApkPath
        if (path != null && canInstallPackages()) {
            installApkOrPrompt(path)
        } else {
            openInstallPermissionSettings()
            dialog = UpdateDialogState.AwaitingPermission
        }
    }

    fun dismissDialog() {
        dialog = UpdateDialogState.None
    }

    fun setGithubProxy(url: String) {
        githubProxyUrl = url.trim()
        try {
            AppStorage.instance.saveString(StorageKeys.GITHUB_PROXY, githubProxyUrl)
        } catch (e: Exception) {
            onLogError("setGithubProxy", e)
        }
    }

    fun setCheckUpdateOnStartEnabled(enabled: Boolean) {
        checkUpdateOnStart = enabled
        try {
            AppStorage.instance.saveBoolean(StorageKeys.CHECK_UPDATE_ON_START, enabled)
        } catch (e: Exception) {
            onLogError("setCheckUpdateOnStartEnabled", e)
        }
    }

    fun enableDeveloperMode() {
        developerMode = true
        try {
            AppStorage.instance.saveBoolean(StorageKeys.DEVELOPER_MODE, true)
        } catch (e: Exception) {
            onLogError("enableDeveloperMode", e)
        }
    }

    fun disableDeveloperMode() {
        developerMode = false
        try {
            AppStorage.instance.saveBoolean(StorageKeys.DEVELOPER_MODE, false)
        } catch (e: Exception) {
            onLogError("disableDeveloperMode", e)
        }
    }
}
