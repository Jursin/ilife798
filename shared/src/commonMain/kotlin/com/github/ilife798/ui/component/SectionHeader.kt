package com.github.ilife798.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 卡片内的标题与分隔线。
@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MiuixTheme.textStyles.title2,
        color = MiuixTheme.colorScheme.onSurface,
    )
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
}
