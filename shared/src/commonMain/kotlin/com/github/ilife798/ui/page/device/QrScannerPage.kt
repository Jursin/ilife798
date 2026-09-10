package com.github.ilife798.ui.page.device

import androidx.compose.runtime.Composable

/**
 * 全屏二维码扫描页。识别成功后回调原始文本并由上层负责解析。
 * Android 使用 CameraX + ML Kit；其它平台为占位实现。
 */
@Composable
expect fun QrScannerPage(
    onBack: () -> Unit,
    onResult: (String) -> Unit
)
