package com.github.ilife798

/**
 * 设备快捷设置图块（Quick Settings Tile）绑定结果。
 */
enum class DeviceTileResult {
    /** 图块已添加 */
    ADDED,

    /** 图块此前已添加 */
    ALREADY_ADDED,

    /** 系统不支持自动添加（Android 13 以下），需用户手动添加 */
    UNSUPPORTED,

    /** 请求失败 */
    FAILED
}

/**
 * 由各平台实现的图块控制器；Android 端负责持久化绑定并请求系统添加图块。
 * 其它平台不设置 [DeviceTile.controller]，调用时视为不支持。
 */
interface DeviceTileController {
    fun bind(deviceId: String, deviceName: String, onResult: (DeviceTileResult) -> Unit)

    /** 当前设备上是否已存在本应用的快捷设置图块。 */
    fun isTileAdded(): Boolean

    /** 记录已在设备上创建图块，供下次启动判断是否还需要显示引导。 */
    fun markTileAdded()
}

object DeviceTile {
    var controller: DeviceTileController? = null

    fun bind(deviceId: String, deviceName: String, onResult: (DeviceTileResult) -> Unit) {
        val c = controller
        if (c == null) onResult(DeviceTileResult.UNSUPPORTED) else c.bind(deviceId, deviceName, onResult)
    }

    fun isTileAdded(): Boolean = controller?.isTileAdded() ?: false

    fun markTileAdded() {
        controller?.markTileAdded()
    }
}
