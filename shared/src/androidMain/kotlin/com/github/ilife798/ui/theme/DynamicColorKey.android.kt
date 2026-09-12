package com.github.ilife798.ui.theme

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource

@Composable
actual fun systemDynamicColorKey(): Color? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    return colorResource(android.R.color.system_accent1_500)
}
