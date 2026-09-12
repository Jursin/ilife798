package com.github.ilife798

import android.content.Context
import androidx.core.content.edit

const val ACTION_SCAN = "com.github.ilife798.action.SCAN"
const val ACTION_START_DEVICE = "com.github.ilife798.action.START_DEVICE"
const val ACTION_RUN_TASKS = "com.github.ilife798.action.RUN_TASKS"
const val EXTRA_DEVICE_ID = "com.github.ilife798.extra.DEVICE_ID"

/** 快捷设置图块绑定的设备信息（进程间通过 SharedPreferences 共享给 TileService）。 */
object DeviceTilePrefs {
    const val NAME = "device_tile"
    const val KEY_DEVICE_ID = "device_id"
    const val KEY_DEVICE_NAME = "device_name"
    const val KEY_TILE_ADDED = "tile_added"

    fun save(context: Context, deviceId: String, deviceName: String) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_DEVICE_ID, deviceId)
                    .putString(KEY_DEVICE_NAME, deviceName)
            }
    }

    fun deviceId(context: Context): String? =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString(KEY_DEVICE_ID, null)

    fun deviceName(context: Context): String? =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString(KEY_DEVICE_NAME, null)

    fun setTileAdded(context: Context, added: Boolean) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_TILE_ADDED, added) }
    }

    fun isTileAdded(context: Context): Boolean =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).getBoolean(KEY_TILE_ADDED, false)
}
