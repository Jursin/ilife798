package com.github.ilife798.data.viewmodel

import com.github.ilife798.data.model.DeviceGoods

// 通道设备固定 {pos,mode}，其余有模式走 {mode,ext}，仅通道走 {pos,mode:null}
internal fun buildDeviceDetailStartArgs(
    goods: List<DeviceGoods>,
    partMode: Int?,
    channelIndex: Int,
    passageDevice: Boolean,
): String {
    if (passageDevice) {
        if (channelIndex < 0) return ""
        return """{"pos":$channelIndex,"mode":${partMode ?: "null"}}"""
    }
    if (partMode != null) {
        val ext = goods.joinToString(",") { """{"pos":${it.pos},"out":${it.out}}""" }
        return """{"mode":$partMode,"ext":[$ext]}"""
    }
    if (channelIndex >= 0) {
        return """{"pos":$channelIndex,"mode":null}"""
    }
    return ""
}
