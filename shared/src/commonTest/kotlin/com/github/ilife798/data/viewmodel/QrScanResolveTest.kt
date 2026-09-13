package com.github.ilife798.data.viewmodel

import com.github.ilife798.data.api.QrUseResult
import kotlin.test.Test
import kotlin.test.assertEquals

class QrScanResolveTest {
    @Test
    fun deviceAndDoorLockTypesReturnDeviceId() {
        assertEquals(
            QrResolveOutcome.Device("dev1"),
            resolveQrUseResult(QrUseResult(success = true, type = QR_TYPE_DEVICE, deviceId = "dev1")),
        )
        assertEquals(
            QrResolveOutcome.Device("dev2"),
            resolveQrUseResult(QrUseResult(success = true, type = QR_TYPE_DOOR_LOCK, deviceId = "dev2")),
        )
    }

    @Test
    fun unsupportedType() {
        assertEquals(
            QrResolveOutcome.UnsupportedType,
            resolveQrUseResult(QrUseResult(success = true, type = 99, deviceId = "dev1")),
        )
        assertEquals(
            QrResolveOutcome.UnsupportedType,
            resolveQrUseResult(QrUseResult(success = true, type = null, deviceId = "dev1")),
        )
    }

    @Test
    fun missingDeviceId() {
        assertEquals(
            QrResolveOutcome.MissingDevice,
            resolveQrUseResult(QrUseResult(success = true, type = QR_TYPE_DEVICE, deviceId = "")),
        )
    }

    @Test
    fun failedWhenResponseUnsuccessful() {
        assertEquals(
            QrResolveOutcome.Failed,
            resolveQrUseResult(QrUseResult(success = false, type = QR_TYPE_DEVICE, deviceId = "dev1")),
        )
    }
}
