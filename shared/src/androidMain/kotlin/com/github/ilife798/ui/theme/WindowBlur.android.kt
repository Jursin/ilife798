package com.github.ilife798.ui.theme

import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import java.util.function.Consumer

@Composable
actual fun WindowBlurEffect(useBlur: Boolean, blurRadius: Int) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

    val window = findCurrentWindow() ?: return
    val blurEnabledBySystem = isCrossWindowBlurEnabled()

    DisposableEffect(window, useBlur, blurRadius, blurEnabledBySystem) {
        if (useBlur && blurEnabledBySystem) {
            window.applyBlur(blurRadius)
        } else {
            window.clearBlur()
        }
        onDispose { window.clearBlur() }
    }
}

@Composable
private fun findCurrentWindow(): Window? {
    val view = LocalView.current
    val dialogWindow = (view.parent as? DialogWindowProvider)?.window
    if (dialogWindow != null) return dialogWindow
    return LocalActivity.current?.window
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun isCrossWindowBlurEnabled(): Boolean {
    val context = LocalContext.current
    val wm = remember(context) { context.getSystemService(WindowManager::class.java) }
    var isEnabled by remember { mutableStateOf(wm.isCrossWindowBlurEnabled) }

    DisposableEffect(wm) {
        val listener = Consumer<Boolean> { enabled -> isEnabled = enabled }
        wm.addCrossWindowBlurEnabledListener(listener)
        onDispose { wm.removeCrossWindowBlurEnabledListener(listener) }
    }
    return isEnabled
}

@RequiresApi(Build.VERSION_CODES.S)
private fun Window.applyBlur(radius: Int) {
    addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
    attributes = attributes.apply {
        blurBehindRadius = radius.coerceIn(0, 150)
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun Window.clearBlur() {
    clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
    attributes = attributes.apply {
        blurBehindRadius = 0
    }
}
