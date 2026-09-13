package com.github.ilife798.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.ilife798.ui.theme.WindowBlurEffect
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

// 对话框底部的取消/确认按钮行。
@Composable
fun DialogActionRow(
    confirmText: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    cancelText: String = "取消",
    cancelEnabled: Boolean = true,
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    primary: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(
            modifier = Modifier.weight(1f),
            onClick = onCancel,
            enabled = cancelEnabled,
            text = cancelText,
        )
        TextButton(
            modifier = Modifier.weight(1f),
            onClick = onConfirm,
            enabled = confirmEnabled,
            text = confirmText,
            colors =
                when {
                    destructive -> ButtonDefaults.textButtonColors(textColor = MiuixTheme.colorScheme.error)
                    primary -> ButtonDefaults.textButtonColorsPrimary()
                    else -> ButtonDefaults.textButtonColors()
                },
        )
    }
}

// 通用确认对话框：[content] 用于金额、选项等自定义正文，未传 [message] 时正文即由它提供。
@Composable
fun ConfirmDialog(
    title: String,
    appBlur: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = "确定",
    cancelText: String = "取消",
    destructive: Boolean = false,
    cancelEnabled: Boolean = true,
    confirmEnabled: Boolean = true,
    primary: Boolean = true,
    message: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    WindowDialog(
        show = true,
        onDismissRequest = onDismiss,
        title = title,
        content = {
            WindowBlurEffect(useBlur = appBlur)
            val dismiss = LocalDismissState.current
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (message != null) Text(text = message)
                content?.invoke(this)
                Spacer(modifier = Modifier.height(24.dp))
                DialogActionRow(
                    confirmText = confirmText,
                    onConfirm = onConfirm,
                    onCancel = { dismiss?.invoke() },
                    cancelText = cancelText,
                    cancelEnabled = cancelEnabled,
                    confirmEnabled = confirmEnabled,
                    destructive = destructive,
                    primary = primary,
                )
            }
        },
    )
}
