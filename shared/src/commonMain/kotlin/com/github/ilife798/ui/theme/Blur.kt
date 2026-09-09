package com.github.ilife798.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.shader.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Remember a [LayerBackdrop] that captures the content drawn with [Modifier.layerBackdrop].
 * Returns null when blur is disabled or the platform doesn't support runtime shaders.
 */
@Composable
fun rememberAppBlurBackdrop(enabled: Boolean): LayerBackdrop? {
    if (!enabled || !isRuntimeShaderSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/** Transparent when blur is active so the backdrop shows through, otherwise the surface color. */
@Composable
fun blurAppBarColor(backdrop: LayerBackdrop?): Color =
    if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface

/** Captures this composable's content into [backdrop] so bars can blur it. No-op when null. */
fun Modifier.captureForBlur(backdrop: LayerBackdrop?): Modifier =
    if (backdrop != null) this.layerBackdrop(backdrop) else this

/** Applies the frosted-glass blur to a bar, using [backdrop] as its source. */
@Composable
fun Modifier.appBarBlur(backdrop: LayerBackdrop?, blurRadius: Float = 25f): Modifier {
    if (backdrop == null) return this
    return this.textureBlur(
        backdrop = backdrop,
        shape = RectangleShape,
        blurRadius = blurRadius,
        colors = BlurColors(
            blendColors = listOf(
                BlendColorEntry(MiuixTheme.colorScheme.surface.copy(alpha = 0.8f))
            )
        ),
    )
}
