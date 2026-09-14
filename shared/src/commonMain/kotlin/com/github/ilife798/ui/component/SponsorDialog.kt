package com.github.ilife798.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

// 启动次数达标后的赞助提示，[onDismiss] 表示未作选择（继续计数）。
@Composable
fun SponsorDialog(
    appBlur: Boolean,
    message: String,
    onDismiss: () -> Unit,
    onNeverRemind: () -> Unit,
    onSponsor: () -> Unit,
) {
    WindowDialog(
        show = true,
        onDismissRequest = onDismiss,
        title = "赞助提示",
        content = {
            WindowBlurEffect(useBlur = appBlur)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = message)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "作者：Jursin",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = onNeverRemind,
                        text = "不再提醒",
                    )
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = onSponsor,
                        text = "立即赞助",
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }
        },
    )
}
