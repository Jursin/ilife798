package com.github.ilife798.ui.theme

import com.github.ilife798.data.model.PaletteStyle
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

fun PaletteStyle.toMiuixPaletteStyle(): ThemePaletteStyle =
    when (this) {
        PaletteStyle.TonalSpot -> ThemePaletteStyle.TonalSpot
        PaletteStyle.Neutral -> ThemePaletteStyle.Neutral
        PaletteStyle.Vibrant -> ThemePaletteStyle.Vibrant
        PaletteStyle.Expressive -> ThemePaletteStyle.Expressive
    }
