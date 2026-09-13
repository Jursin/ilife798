package com.github.ilife798.ui.page.account

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.ilife798.copyToClipboard
import com.github.ilife798.data.model.Account
import com.github.ilife798.data.viewmodel.AppViewModel
import com.github.ilife798.ui.component.BlurredTopAppBar
import com.github.ilife798.ui.component.ConfirmDialog
import com.github.ilife798.ui.component.PageScrollColumn
import com.github.ilife798.ui.theme.rememberAppBlurBackdrop
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

private enum class CredentialField(
    val key: String,
    val title: String,
) {
    APP_TOKEN("appToken", "设备控制"),
    TOKEN("token", "积分任务"),
    UID("uid", "用户 ID"),
}

private fun valueOf(
    account: Account,
    field: CredentialField,
): String =
    when (field) {
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
) {
    val account = viewModel.state.account
    val scrollBehavior = MiuixScrollBehavior()
    val blurBackdrop = rememberAppBlurBackdrop(viewModel.state.appBlur)

    var editingField by remember { mutableStateOf<CredentialField?>(null) }
    var inputValue by remember { mutableStateOf("") }

    Scaffold(
        topBar = { BlurredTopAppBar("账号信息", blurBackdrop, scrollBehavior, onBack = onBack) },
    ) { paddingValues ->
        PageScrollColumn(blurBackdrop, scrollBehavior, paddingValues) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    CredentialField.entries.forEach { field ->
                        val value = valueOf(account, field)
                        val display = if (value.isEmpty()) "-" else maskCredential(value)
                        val copy = {
                            copyToClipboard(value, "已复制 ${field.key}")
                        }
                        val edit = {
                            inputValue = value
                            editingField = field
                        }
                        if (field == CredentialField.UID) {
                            BasicComponent(
                                title = field.title,
                                summary = "${field.key}: $display",
                                onClick = { if (value.isNotEmpty()) copy() },
                            )
                        } else {
                            BasicComponent(
                                title = field.title,
                                summary = "${field.key}: $display",
                                modifier =
                                    Modifier.combinedClickable(
                                        onClick = { if (value.isNotEmpty()) copy() else edit() },
                                        onLongClick = edit,
                                    ),
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        contentColor = MiuixTheme.colorScheme.onSurface,
                    ),
            ) {
                Text(
                    text = "设备控制/积分任务为空时点击弹出输入对话框，有值时点击复制，长按弹出输入对话框。\n用户 ID 有值时点击复制。",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }

    val currentEditing = editingField
    if (currentEditing != null) {
        val hasExistingValue = valueOf(account, currentEditing).isNotEmpty()
        ConfirmDialog(
            title = "填写 ${currentEditing.key}",
            appBlur = viewModel.state.appBlur,
            onDismiss = { editingField = null },
            onConfirm = {
                val input = inputValue.trim()
                if (input.isEmpty() && hasExistingValue) {
                    viewModel.clearAccountField(currentEditing.key)
                } else {
                    viewModel.updateAccountField(currentEditing.key, input)
                }
                editingField = null
            },
        ) {
            TextField(
                value = inputValue,
                onValueChange = { inputValue = it },
                modifier = Modifier.fillMaxWidth(),
                label = currentEditing.key,
            )
        }
    }
}
