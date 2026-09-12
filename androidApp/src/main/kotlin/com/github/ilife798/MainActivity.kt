package com.github.ilife798

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.github.ilife798.data.api.ApiConfig
import com.github.ilife798.update.ACTION_SHOW_UPDATE

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ApplicationContext.instance = applicationContext
        ActivityHolder.current = this
        AppStorage.instance = PersistentStorage(applicationContext)
        DeviceTile.controller = AndroidDeviceTileController(applicationContext)
        ApiConfig.init(
            gateway = BuildConfig.API_GATEWAY,
            salt = BuildConfig.SIGN_SALT,
            clientId = BuildConfig.API_CID
        )

        RunNotifications.ensureInitialized()
        handleIntent(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        AppLifecycle.notifyResumed()
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_SCAN -> AppShortcut.requestScan()
            ACTION_START_DEVICE -> {
                intent.getStringExtra(EXTRA_DEVICE_ID)?.let { AppShortcut.requestStartDevice(it) }
            }
            ACTION_SHOW_UPDATE -> AppUpdateIntent.requestShow()
            RunNotificationIntents.ACTION_OPEN_HOME -> RunNotificationIntent.requestOpenHome()
            RunNotificationIntents.ACTION_STOP_DEVICE -> {
                intent.getStringExtra(RunNotificationIntents.EXTRA_DEVICE_ID)
                    ?.let { RunNotificationIntent.requestStopDevice(it) }
            }
            RunNotificationIntents.ACTION_OPEN_TASKS -> RunNotificationIntent.requestOpenTasks()
            RunNotificationIntents.ACTION_STOP_TASKS -> RunNotificationIntent.requestStopTasks()
        }
    }

    override fun onDestroy() {
        if (ActivityHolder.current === this) {
            ActivityHolder.current = null
        }
        super.onDestroy()
    }
}
