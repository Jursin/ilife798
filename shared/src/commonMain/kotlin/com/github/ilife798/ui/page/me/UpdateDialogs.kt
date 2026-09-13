package com.github.ilife798.ui.page.me

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.ilife798.data.viewmodel.AppViewModel
import com.github.ilife798.ui.component.ConfirmDialog
import com.github.ilife798.ui.theme.WindowBlurEffect
import com.github.ilife798.update.UpdateDialogState
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun UpdateDialogs(
    viewModel: AppViewModel,
    showGithubProxyDialog: Boolean,
    onDismissGithubProxyDialog: () -> Unit,
) {
    when (val dialog = viewModel.updateDialog) {
        is UpdateDialogState.Available -> {
            ConfirmDialog(
                title = "检查到新版本",
                message = "确定更新到最新版本 v${dialog.version} 吗？",
                appBlur = viewModel.state.appBlur,
                confirmText = "更新",
                onConfirm = { viewModel.startUpdate() },
                onDismiss = { viewModel.dismissUpdateDialog() },
            )
        }

        is UpdateDialogState.Downloading -> {
            ConfirmDialog(
                title = "正在下载更新",
                appBlur = viewModel.state.appBlur,
                confirmText = "停止下载",
                cancelText = "后台下载",
                destructive = true,
                onConfirm = { viewModel.stopUpdate() },
                onDismiss = { viewModel.hideUpdateProgressDialog() },
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = dialog.progress,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "已下载 ${(dialog.progress * 100).toInt()}%",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }

        is UpdateDialogState.AwaitingPermission -> {
            WindowDialog(
                show = true,
                onDismissRequest = null,
                title = "正在等待授权",
                content = {
                    WindowBlurEffect(useBlur = viewModel.state.appBlur)
                    Column {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            progress = null,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "请在系统设置中允许安装应用")
                    }
                },
            )
        }

        is UpdateDialogState.NeedPermission -> {
            ConfirmDialog(
                title = "需要安装应用权限",
                message = "更新应用需要安装应用权限，请在系统设置中开启。",
                appBlur = viewModel.state.appBlur,
                confirmText = "去设置",
                onConfirm = { viewModel.openInstallSettings() },
                onDismiss = { viewModel.dismissUpdateDialog() },
            )
        }

        UpdateDialogState.None -> {}
    }

    if (showGithubProxyDialog) {
        var url by remember { mutableStateOf(viewModel.githubProxyUrl) }
        ConfirmDialog(
            title = "设置 GitHub 加速地址",
            appBlur = viewModel.state.appBlur,
            onConfirm = {
                viewModel.setGithubProxy(url)
                onDismissGithubProxyDialog()
            },
            onDismiss = onDismissGithubProxyDialog,
        ) {
            TextField(
                value = url,
                onValueChange = { url = it },
                label = "URL",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
