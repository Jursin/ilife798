package com.github.ilife798

import androidx.compose.ui.window.ComposeUIViewController
import com.github.ilife798.data.api.ApiConfig
import platform.Foundation.NSBundle

fun MainViewController() = run {
    val info = NSBundle.mainBundle
    ApiConfig.init(
        gateway = info.objectForInfoDictionaryKey("ApiGateway") as? String ?: "",
        salt = info.objectForInfoDictionaryKey("SignSalt") as? String ?: "",
        clientId = info.objectForInfoDictionaryKey("ApiCid") as? String ?: ""
    )
    ComposeUIViewController { App() }
}