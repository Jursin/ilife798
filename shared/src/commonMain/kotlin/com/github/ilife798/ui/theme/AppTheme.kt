package com.github.ilife798.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.github.ilife798.data.model.PaletteStyle
import com.github.ilife798.data.model.ThemeMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.System,
    customColor: Boolean = true,
    dynamicColor: Boolean = true,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    seedColor: Color = Color(0xFF6750A4),
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }

    val colorSchemeMode = when {
        customColor && isDark -> ColorSchemeMode.MonetDark
        customColor && !isDark -> ColorSchemeMode.MonetLight
        isDark -> ColorSchemeMode.Dark
        else -> ColorSchemeMode.Light
    }

    val dynamicKeyColor = if (customColor && dynamicColor) systemDynamicColorKey() else null
    val keyColor = when {
        !customColor -> null
        dynamicColor -> dynamicKeyColor
        else -> seedColor
    }

    val controller = remember(colorSchemeMode, keyColor, paletteStyle, customColor, isDark) {
        if (customColor) {
            ThemeController(
                colorSchemeMode = colorSchemeMode,
                keyColor = keyColor,
                paletteStyle = paletteStyle.toMiuixPaletteStyle(),
                colorSpec = ThemeColorSpec.Spec2025,
                isDark = isDark
            )
        } else {
            ThemeController(colorSchemeMode)
        }
    }

    MiuixTheme(controller = controller) {
        content()
    }
}
