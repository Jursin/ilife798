package com.github.ilife798.ui.theme

import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun primaryButtonColors(dynamicColor: Boolean = true) = if (dynamicColor) {
    ButtonDefaults.buttonColorsPrimary(
        disabledContentColor = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    )
} else {
    ButtonDefaults.buttonColors(
        disabledColor = MiuixTheme.colorScheme.disabledSecondaryVariant,
        disabledContentColor = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    )
}
