package com.github.ilife798.ui.page.device

import androidx.compose.runtime.Composable

// 全屏二维码扫描页。识别成功后回调原始文本并由上层负责解析。
// Android 使用 CameraX + ML Kit；iOS 使用 AVFoundation（见 iosMain）。
@Composable
expect fun QrScannerPage(
    onBack: () -> Unit,
    onResult: (String) -> Unit,
)
