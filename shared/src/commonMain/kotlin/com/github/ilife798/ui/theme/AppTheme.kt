package com.github.ilife798.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.ilife798.data.model.ThemeMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.System,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }

    val colorSchemeMode = when {
        dynamicColor && isDark -> ColorSchemeMode.MonetDark
        dynamicColor && !isDark -> ColorSchemeMode.MonetLight
        isDark -> ColorSchemeMode.Dark
        else -> ColorSchemeMode.Light
    }

    val controller = remember(themeMode, dynamicColor) {
        ThemeController(colorSchemeMode)
    }

    MiuixTheme(controller = controller) {
        content()
    }
}
