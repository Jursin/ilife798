package com.github.ilife798.data.viewmodel

import com.github.ilife798.data.api.QrUseResult

// 官方 /qr/use 返回的二维码类型：设备 / 门锁
internal const val QR_TYPE_DEVICE = 3
internal const val QR_TYPE_DOOR_LOCK = 8

// /qr/use 结果到设备编号的纯决策，供 DeviceController 与测试复用。
internal sealed interface QrResolveOutcome {
    data class Device(
        val deviceId: String,
    ) : QrResolveOutcome

    data object UnsupportedType : QrResolveOutcome

    data object MissingDevice : QrResolveOutcome

    data object Failed : QrResolveOutcome
}

internal fun resolveQrUseResult(result: QrUseResult): QrResolveOutcome {
    if (!result.success) return QrResolveOutcome.Failed
    if (result.type != QR_TYPE_DEVICE && result.type != QR_TYPE_DOOR_LOCK) return QrResolveOutcome.UnsupportedType
    if (result.deviceId.isEmpty()) return QrResolveOutcome.MissingDevice
    return QrResolveOutcome.Device(result.deviceId)
}
