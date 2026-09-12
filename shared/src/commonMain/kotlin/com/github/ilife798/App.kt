package com.github.ilife798

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.ilife798.data.viewmodel.AppViewModel
import com.github.ilife798.ui.navigation.MainScaffold
import com.github.ilife798.ui.theme.AppTheme

@Composable
fun App() {
    val viewModel = viewModel { AppViewModel() }
    val state = viewModel.state
    AppTheme(
        themeMode = state.themeMode,
        customColor = state.customColor,
        dynamicColor = state.dynamicColor,
        paletteStyle = state.paletteStyle,
        seedColor = Color(state.seedColor)
    ) {
        MainScaffold(viewModel = viewModel)
    }
}
