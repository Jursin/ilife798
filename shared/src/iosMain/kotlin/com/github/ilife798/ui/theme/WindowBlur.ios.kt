package com.github.ilife798.ui.theme

import androidx.compose.runtime.Composable

// iOS 无窗口级背景模糊 API；commonMain 契约注释「其它平台不处理」，直通 no-op。
@Composable
actual fun WindowBlurEffect(useBlur: Boolean) {}
