package com.github.ilife798

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.github.ilife798.data.api.ApiConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ApplicationContext.instance = applicationContext
        AppStorage.instance = PersistentStorage(applicationContext)
        ApiConfig.init(
            gateway = BuildConfig.API_GATEWAY,
            salt = BuildConfig.SIGN_SALT,
            clientId = BuildConfig.API_CID
        )

        setContent {
            App()
        }
    }
}
