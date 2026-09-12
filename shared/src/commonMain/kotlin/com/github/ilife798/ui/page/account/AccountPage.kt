package com.github.ilife798.ui.page.account

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog
import com.github.ilife798.copyTextToClipboard
import com.github.ilife798.data.model.Account
import com.github.ilife798.data.viewmodel.AppViewModel
import com.github.ilife798.showToast
import com.github.ilife798.ui.theme.WindowBlurEffect
import com.github.ilife798.ui.theme.appBarBlur
import com.github.ilife798.ui.theme.blurAppBarColor
import com.github.ilife798.ui.theme.captureForBlur
import com.github.ilife798.ui.theme.rememberAppBlurBackdrop

private enum class EmptyAction { EDIT, LOGIN, NONE }

private enum class CredentialField(val key: String, val title: String, val emptyAction: EmptyAction) {
    APP_TOKEN("appToken", "设备控制", EmptyAction.EDIT),
    TOKEN("token", "积分任务", EmptyAction.LOGIN),
    UID("uid", "用户 ID", EmptyAction.NONE)
}

private fun valueOf(account: Account, field: CredentialField): String = when (field) {
    CredentialField.APP_TOKEN -> account.appToken
    CredentialField.TOKEN -> account.token
    CredentialField.UID -> account.uid
}

private fun maskCredential(value: String): String {
    if (value.length <= 8) return value
    return value.take(4) + "*".repeat(value.length - 8) + value.takeLast(4)
}

@Composable
fun AccountPage(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onLoginClick: (isAlipay: Boolean) -> Unit = {}
) {
    val account = viewModel.state.account
    val scrollBehavior = MiuixScrollBehavior()
    val blurBackdrop = rememberAppBlurBackdrop(viewModel.state.appBlur)

    var editingField by remember { mutableStateOf<CredentialField?>(null) }
    var inputValue by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "账号信息",
                modifier = Modifier.appBarBlur(blurBackdrop),
                color = blurAppBarColor(blurBackdrop),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .captureForBlur(blurBackdrop)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .scrollEndHaptic()
                .overScrollVertical()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    CredentialField.entries.forEach { field ->
                        val value = valueOf(account, field)
                        val display = if (value.isEmpty()) "-" else maskCredential(value)
                        val onTap = {
                            if (value.isNotEmpty()) {
                                if (copyTextToClipboard(value)) showToast("已复制 ${field.key}")
                                else showToast("复制失败")
                            } else when (field.emptyAction) {
                                EmptyAction.EDIT -> {
                                    inputValue = ""
                                    editingField = field
                                }

                                EmptyAction.LOGIN -> onLoginClick(true)
                                EmptyAction.NONE -> {}
                            }
                        }
                        if (field == CredentialField.UID) {
                            BasicComponent(
                                title = field.title,
                                summary = "${field.key}: $display",
                                onClick = onTap
                            )
                        } else {
                            BasicComponent(
                                title = field.title,
                                summary = "${field.key}: $display",
                                modifier = Modifier.combinedClickable(
                                    onClick = { onTap() },
                                    onLongClick = {
                                        inputValue = value
                                        editingField = field
                                    }
                                )
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    contentColor = MiuixTheme.colorScheme.onSurface
                )
            ) {
                Text(
                    text = "设备控制为空时点击弹出输入对话框，积分任务为空时点击进入积分登录页面。有值时点击设备控制/积分任务/用户 ID 复制对应值，长按设备控制/积分任务弹出输入框。",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

    val currentEditing = editingField
    if (currentEditing != null) {
        WindowDialog(
            show = true,
            onDismissRequest = { editingField = null },
            title = "填写 ${currentEditing.key}",
            content = {
                WindowBlurEffect(useBlur = viewModel.state.appBlur)
                val dismiss = LocalDismissState.current
                val hasExistingValue = valueOf(account, currentEditing).isNotEmpty()
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TextField(
                        value = inputValue,
                        onValueChange = { inputValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = currentEditing.key
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = { dismiss?.invoke() },
                            text = "取消"
                        )
                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val input = inputValue.trim()
                                if (input.isEmpty() && hasExistingValue) {
                                    viewModel.clearAccountField(currentEditing.key)
                                } else {
                                    viewModel.updateAccountField(currentEditing.key, input)
                                }
                                dismiss?.invoke()
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
