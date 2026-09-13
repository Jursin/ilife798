package com.github.ilife798.ui.theme

import androidx.compose.runtime.Composable

// 给当前窗口（对话框或 Activity）加上窗口级背景模糊。
// Android 12+ 支持，其它平台不处理。
@Composable
expect fun WindowBlurEffect(
    useBlur: Boolean,
    blurRadius: Int = 30,
)
