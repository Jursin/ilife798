package com.github.ilife798

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * 设备快捷设置图块。点击后启动 MainActivity 并携带设备编号，
 * 由 UI 回到首页并触发对应设备的启动按钮流程。
 */
class DeviceTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        val name = DeviceTilePrefs.deviceName(this)
        tile.label = name?.takeIf { it.isNotEmpty() } ?: getString(R.string.tile_device_label)
        // Tile.setSubtitle 需要 API 29+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (name.isNullOrEmpty()) "未绑定设备" else "点击启动"
        }
        tile.state = Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_device)
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val deviceId = DeviceTilePrefs.deviceId(this)
        launchApp(deviceId)
    }

    private fun launchApp(deviceId: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (!deviceId.isNullOrEmpty()) {
                action = ACTION_START_DEVICE
                putExtra(EXTRA_DEVICE_ID, deviceId)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pending)
        } else {
            // API 34 以下无 PendingIntent 重载，只能使用该已废弃方法
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            //noinspection StartActivityAndCollapseDeprecated
            startActivityAndCollapse(intent)
        }
    }
}
