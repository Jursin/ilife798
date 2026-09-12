package com.github.ilife798.ui.page.me

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.ilife798.data.viewmodel.AppViewModel
import com.github.ilife798.update.UpdateDialogState
import com.github.ilife798.ui.theme.WindowBlurEffect
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun UpdateDialogs(
    viewModel: AppViewModel,
    showGithubProxyDialog: Boolean,
    onDismissGithubProxyDialog: () -> Unit
) {
    when (val dialog = viewModel.updateDialog) {
        is UpdateDialogState.Available -> WindowDialog(
            show = true,
            onDismissRequest = { viewModel.dismissUpdateDialog() },
            title = "检查到新版本",
            content = {
                WindowBlurEffect(useBlur = viewModel.state.appBlur)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "确定更新到最新版本 v${dialog.version} 吗？")
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.dismissUpdateDialog() },
                            text = "取消"
                        )
                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.startUpdate() },
                            text = "更新",
                            colors = ButtonDefaults.textButtonColorsPrimary()
                        )
                    }
                }
            }
        )

        is UpdateDialogState.Downloading -> WindowDialog(
            show = true,
            onDismissRequest = { viewModel.hideUpdateProgressDialog() },
            title = "正在下载更新",
            content = {
                WindowBlurEffect(useBlur = viewModel.state.appBlur)
                Column {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        progress = dialog.progress
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "已下载 ${(dialog.progress * 100).toInt()}%",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.hideUpdateProgressDialog() },
                            text = "后台下载"
                        )
                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.stopUpdate() },
                            text = "停止下载",
                            colors = ButtonDefaults.textButtonColors(
                                textColor = MiuixTheme.colorScheme.error
                            )
                        )
                    }
                }
            }
        )

        is UpdateDialogState.AwaitingPermission -> WindowDialog(
            show = true,
            onDismissRequest = null,
            title = "正在等待授权",
            content = {
                WindowBlurEffect(useBlur = viewModel.state.appBlur)
                Column {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        progress = null
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "请在系统设置中允许安装应用")
                }
            }
        )

        is UpdateDialogState.NeedPermission -> WindowDialog(
            show = true,
            onDismissRequest = { viewModel.dismissUpdateDialog() },
            title = "需要安装应用权限",
            content = {
                WindowBlurEffect(useBlur = viewModel.state.appBlur)
                Column {
                    Text(text = "更新应用需要安装应用权限，请在系统设置中开启。")
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.dismissUpdateDialog() },
                            text = "取消"
                        )
                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.openInstallSettings() },
                            text = "去设置",
                            colors = ButtonDefaults.textButtonColorsPrimary()
                        )
                    }
                }
            }
        )

        UpdateDialogState.None -> Unit
    }

    if (showGithubProxyDialog) {
        var url by remember { mutableStateOf(viewModel.githubProxyUrl) }
        WindowDialog(
            show = true,
            onDismissRequest = onDismissGithubProxyDialog,
            title = "设置 GitHub 加速地址",
            content = {
                WindowBlurEffect(useBlur = viewModel.state.appBlur)
                Column {
                    TextField(
                        value = url,
                        onValueChange = { url = it },
                        label = "URL",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = onDismissGithubProxyDialog,
                            text = "取消"
                        )
                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.setGithubProxy(url)
                                onDismissGithubProxyDialog()
                            },
                            text = "确定",
                            colors = ButtonDefaults.textButtonColorsPrimary()
                        )
                    }
                }
            }
        )
    }
}
