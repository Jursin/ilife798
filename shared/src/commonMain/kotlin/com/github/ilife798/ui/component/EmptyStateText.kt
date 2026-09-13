package com.github.ilife798.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 列表空态文案：未登录时提示登录，否则提示无数据。
@Composable
fun EmptyStateText(
    isLoggedIn: Boolean,
    modifier: Modifier = Modifier,
    emptyText: String = "暂无数据",
) {
    Text(
        text = if (!isLoggedIn) "请先登录" else emptyText,
        style = MiuixTheme.textStyles.body2,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = modifier.padding(vertical = 8.dp),
    )
}
