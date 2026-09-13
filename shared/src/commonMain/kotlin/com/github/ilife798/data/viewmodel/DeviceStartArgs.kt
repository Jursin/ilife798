package com.github.ilife798.data.viewmodel

import com.github.ilife798.data.model.DeviceStartOptions

// 组装 /dev/start 的 args：优先模式(parts)，其次通道(subs)
internal fun buildDeviceStartArgs(
    options: DeviceStartOptions,
    selectedIndex: Int,
): String {
    if (options.parts.isNotEmpty()) {
        val part = options.parts.getOrNull(selectedIndex) ?: return ""
        val ext = options.goods.joinToString(",") { """{"pos":${it.pos},"out":${it.out}}""" }
        return """{"mode":${part.mode},"ext":[$ext]}"""
    }
    if (options.subCount > 1) {
        return """{"pos":$selectedIndex,"mode":null}"""
    }
    return ""
}
