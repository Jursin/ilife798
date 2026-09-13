package com.github.ilife798

import android.content.Intent
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
        ActivityHolder.current = this
        AppStorage.instance = PersistentStorage(applicationContext)
        DeviceTile.controller = AndroidDeviceTileController(applicationContext)
        ApiConfig.init(
            gateway = BuildConfig.API_GATEWAY,
            salt = BuildConfig.SIGN_SALT,
            clientId = BuildConfig.API_CID,
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
            IntentActions.ACTION_SCAN -> {
                AppRequests.scan.request()
            }

            IntentActions.ACTION_START_DEVICE -> {
                intent.getStringExtra(IntentActions.EXTRA_DEVICE_ID)?.let { AppRequests.startDevice.request(it) }
            }

            IntentActions.ACTION_RUN_TASKS -> {
                AppRequests.runTasks.request()
            }

            IntentActions.ACTION_SHOW_UPDATE -> {
                AppRequests.showUpdate.request()
            }

            IntentActions.ACTION_OPEN_HOME -> {
                AppRequests.openHome.request()
            }

            IntentActions.ACTION_STOP_DEVICE -> {
                intent.getStringExtra(IntentActions.EXTRA_DEVICE_ID)?.let { AppRequests.stopDevice.request(it) }
            }

            IntentActions.ACTION_OPEN_TASKS -> {
                AppRequests.openTasks.request()
            }

            IntentActions.ACTION_STOP_TASKS -> {
                AppRequests.stopTasks.request()
            }
        }
    }

    override fun onDestroy() {
        if (ActivityHolder.current === this) {
            ActivityHolder.current = null
        }
        super.onDestroy()
    }
}
