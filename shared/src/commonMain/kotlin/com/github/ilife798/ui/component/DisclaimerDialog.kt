package com.github.ilife798.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.ilife798.ui.theme.WindowBlurEffect
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

// 免责声明对话框，供首次启动与设置页共用。
@Composable
fun DisclaimerDialog(
    appBlur: Boolean,
    onDismiss: () -> Unit,
) {
    WindowDialog(
        show = true,
        onDismissRequest = onDismiss,
        title = "免责声明",
        content = {
            WindowBlurEffect(useBlur = appBlur)
            val dismiss = LocalDismissState.current
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
            ) {
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                    colors =
                        CardDefaults.defaultColors(
                            color = MiuixTheme.colorScheme.surfaceContainerHighest,
                        ),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        disclaimerLines.forEach { line ->
                            Text(
                                text = "• $line",
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceContainer,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { dismiss?.invoke() },
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) {
                    Text(text = "了解")
                }
            }
        },
    )
}

private val disclaimerLines =
    listOf(
        "本项目非官方项目，与慧生活798服务提供方无任何联系。",
        "本项目仅供学习和研究使用，不用于商业目的。",
        "请在本应用上仅使用本人有权访问的账户和设备，并遵守相关服务协议。",
        "你应了解自动运行积分任务存在被官方标记、拉黑甚至追究的风险，因使用本应用造成严重后果的由使用者自行承担，本项目概不负责。",
        "你应了解官方可能会变更接口或相关认证方式，本项目不保证持续可用性，不一定及时通知或修复。",
    )
