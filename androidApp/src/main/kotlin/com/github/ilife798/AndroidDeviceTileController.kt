package com.github.ilife798

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.TileService

class AndroidDeviceTileController(private val context: Context) : DeviceTileController {
    override fun isTileAdded(): Boolean = DeviceTilePrefs.isTileAdded(context)

    override fun markTileAdded() {
        DeviceTilePrefs.setTileAdded(context, true)
    }

    override fun bind(deviceId: String, deviceName: String, onResult: (DeviceTileResult) -> Unit) {
        // 绑定信息持久化，供 TileService 读取
        DeviceTilePrefs.save(context, deviceId, deviceName)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onResult(DeviceTileResult.UNSUPPORTED)
            return
        }
        val manager = context.getSystemService(StatusBarManager::class.java)
        if (manager == null) {
            onResult(DeviceTileResult.UNSUPPORTED)
            return
        }
        val component = ComponentName(context, DeviceTileService::class.java)
        val icon = Icon.createWithResource(context, R.drawable.ic_tile_device)
        try {
            manager.requestAddTileService(component, deviceName, icon, context.mainExecutor) { result ->
                val mapped = when (result) {
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> DeviceTileResult.ADDED
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> {
                        // 图块已存在且绑定信息已更新为当前设备，刷新图块以显示新的设备名
                        TileService.requestListeningState(context, component)
                        DeviceTileResult.ALREADY_ADDED
                    }
                    else -> DeviceTileResult.FAILED
                }
                onResult(mapped)
            }
        } catch (_: Exception) {
            onResult(DeviceTileResult.FAILED)
        }
    }
}
