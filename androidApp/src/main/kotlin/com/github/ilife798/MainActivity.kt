package com.github.ilife798

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.github.ilife798.data.api.ApiConfig

private const val ACTION_SCAN = "com.github.ilife798.action.SCAN"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ApplicationContext.instance = applicationContext
        ActivityHolder.current = this
        AppStorage.instance = PersistentStorage(applicationContext)
        ApiConfig.init(
            gateway = BuildConfig.API_GATEWAY,
            salt = BuildConfig.SIGN_SALT,
            clientId = BuildConfig.API_CID
        )

        if (intent?.action == ACTION_SCAN) {
            AppShortcut.requestScan()
        }

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == ACTION_SCAN) {
            AppShortcut.requestScan()
        }
    }

    override fun onDestroy() {
        if (ActivityHolder.current === this) {
            ActivityHolder.current = null
        }
        super.onDestroy()
    }
}
