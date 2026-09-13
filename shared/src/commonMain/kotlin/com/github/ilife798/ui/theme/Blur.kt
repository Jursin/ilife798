package com.github.ilife798.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.shader.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 记住一个 [LayerBackdrop]，用于捕获 [Modifier.layerBackdrop] 绘制的内容。
// 模糊关闭或平台不支持运行时着色器时返回 null。
@Composable
fun rememberAppBlurBackdrop(enabled: Boolean): LayerBackdrop? {
    if (!enabled || !isRuntimeShaderSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

// 模糊生效时返回透明色让背景透出，否则返回表面色。
@Composable
fun blurAppBarColor(backdrop: LayerBackdrop?): Color = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface

// 把内容捕获到 [backdrop]，供标题栏模糊使用；为 null 时不处理。
fun Modifier.captureForBlur(backdrop: LayerBackdrop?): Modifier = if (backdrop != null) this.layerBackdrop(backdrop) else this

// 以 [backdrop] 为源，给标题栏加上毛玻璃模糊。
@Composable
fun Modifier.appBarBlur(
    backdrop: LayerBackdrop?,
    blurRadius: Float = 25f,
): Modifier {
    if (backdrop == null) return this
    return this.textureBlur(
        backdrop = backdrop,
        shape = RectangleShape,
        blurRadius = blurRadius,
        colors =
            BlurColors(
                blendColors =
                    listOf(
                        BlendColorEntry(MiuixTheme.colorScheme.surface.copy(alpha = 0.8f)),
                    ),
            ),
    )
}
