package com.github.ilife798.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeviceLinkParserTest {
    @Test
    fun pureDigitsAsDeviceId() {
        val result = DeviceLinkParser.parse("1234567890")
        assertEquals("1234567890", result.deviceId)
        assertNull(result.qrId)
    }

    @Test
    fun walletLinkAsQrId() {
        val result = DeviceLinkParser.parse("https://gateway.example.com/q/12345/wallet?id=abc123")
        assertEquals("abc123", result.qrId)
        assertNull(result.deviceId)
    }

    @Test
    fun genericLinkAsDeviceId() {
        val result = DeviceLinkParser.parse("https://gateway.example.com/q/12345/abcdef")
        assertEquals("abcdef", result.deviceId)
        assertNull(result.qrId)
    }

    @Test
    fun trimsSurroundingWhitespace() {
        assertEquals("42", DeviceLinkParser.parse("  42  ").deviceId)
    }

    @Test
    fun blankReturnsEmptyResult() {
        assertNull(DeviceLinkParser.parse("").deviceId)
        assertNull(DeviceLinkParser.parse("").qrId)
        assertNull(DeviceLinkParser.parse("   ").deviceId)
    }

    @Test
    fun idQueryKindsAllYieldQrId() {
        assertEquals("a1", DeviceLinkParser.parse("https://i.ilife798.com/q/1/b?id=a1").qrId)
        assertEquals("a2", DeviceLinkParser.parse("https://i.ilife798.com/q/1/s?id=a2").qrId)
        assertEquals("a3", DeviceLinkParser.parse("https://i.ilife798.com/q/1/billing?id=a3").qrId)
        assertEquals("a4", DeviceLinkParser.parse("https://i.ilife798.com/q/1/vip?id=a4").qrId)
        assertEquals("a5", DeviceLinkParser.parse("https://i.ilife798.com/q/1/wallet?id=a5").qrId)
    }

    @Test
    fun bmLinkUsesContentAfterFixedPrefix() {
        // 固定截取 "q/1/" 之后的内容作为 qrId
        assertEquals("bm?/tok-1", DeviceLinkParser.parse("https://i.ilife798.com/q/1/bm?/tok-1").qrId)
    }

    @Test
    fun appDeepLinkYieldsDeviceId() {
        // NFC 标签常见写法：id 即设备编号
        assertEquals(
            "863781051223338",
            DeviceLinkParser.parse("ilife798s://i/device/nav?id=863781051223338").deviceId,
        )
        assertEquals(
            "dev123abc",
            DeviceLinkParser.parse("ilife798s://i/device/nfc?id=dev123abc&from=nfc").deviceId,
        )
    }

    @Test
    fun unknownTextIsInvalid() {
        val result = DeviceLinkParser.parse("hello")
        assertNull(result.deviceId)
        assertNull(result.qrId)
    }

    @Test
    fun validDeviceIdFormat() {
        assertTrue(DeviceLinkParser.isValidDeviceId("abcdef012345"))
        assertTrue(DeviceLinkParser.isValidDeviceId("863781051223338"))
        assertTrue(DeviceLinkParser.isValidDeviceId("a".repeat(16)))
        assertFalse(DeviceLinkParser.isValidDeviceId("abc123"))
        assertFalse(DeviceLinkParser.isValidDeviceId("a".repeat(17)))
        assertFalse(DeviceLinkParser.isValidDeviceId("ABCDEF012345"))
        assertFalse(DeviceLinkParser.isValidDeviceId("abcdef-012345"))
        assertFalse(DeviceLinkParser.isValidDeviceId(""))
    }
}
