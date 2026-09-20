package com.github.ilife798.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreGraphics.CGFloatVar
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

// iOS 不公开用户全局强调色 API（Material You 无对应物）。
// 尝试读取当前前台 key window 的 tintColor（跟随系统强调色设置），
// 不可用/未就绪时返回 null 走静态主题——这是设计允许的回退路径。
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
@Composable
actual fun systemDynamicColorKey(): Color? =
    runCatching {
        val window =
            UIApplication.sharedApplication.connectedScenes
                .filterIsInstance<UIWindowScene>()
                .firstOrNull { it.activationState == UISceneActivationStateForegroundActive }
                ?.windows
                ?.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true } as? UIWindow
                ?: return null
        val tint: UIColor = window.tintColor
        memScoped {
            val r = alloc<CGFloatVar>()
            val g = alloc<CGFloatVar>()
            val b = alloc<CGFloatVar>()
            val a = alloc<CGFloatVar>()
            if (!tint.getRed(r.ptr, green = g.ptr, blue = b.ptr, alpha = a.ptr)) return null
            Color(
                red = r.value.toFloat(),
                green = g.value.toFloat(),
                blue = b.value.toFloat(),
                alpha = a.value.toFloat(),
            )
        }
    }.getOrNull()
