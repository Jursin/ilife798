package com.github.ilife798.ui.theme

import androidx.compose.runtime.Composable

/**
 * Applies a window-level blur-behind effect to the current window (dialog or activity).
 * Supported on Android 12+; a no-op elsewhere.
 */
@Composable
expect fun WindowBlurEffect(useBlur: Boolean, blurRadius: Int = 30)
