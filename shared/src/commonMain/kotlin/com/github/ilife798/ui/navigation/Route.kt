package com.github.ilife798.ui.navigation

import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

@Serializable
sealed class Page : NavKey {
    @Serializable data object Home : Page()
    @Serializable data class Login(val isAlipay: Boolean = false) : Page()
    @Serializable data object Score : Page()
    @Serializable data object Account : Page()
    @Serializable data object Bill : Page()
    @Serializable data object DeviceAdd : Page()
}
