package com.github.ilife798.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QrCodeParserTest {
    @Test
    fun pureDigitsAsDeviceId() {
        val result = QrCodeParser.parse("1234567890")
        assertEquals("1234567890", result.deviceId)
        assertNull(result.qrId)
    }

    @Test
    fun walletLinkAsQrId() {
        val result = QrCodeParser.parse("https://gateway.example.com/q/12345/wallet?id=abc123")
        assertEquals("abc123", result.qrId)
        assertNull(result.deviceId)
    }

    @Test
    fun genericLinkAsDeviceId() {
        val result = QrCodeParser.parse("https://gateway.example.com/q/12345/abcdef")
        assertEquals("abcdef", result.deviceId)
        assertNull(result.qrId)
    }

    @Test
    fun trimsSurroundingWhitespace() {
        assertEquals("42", QrCodeParser.parse("  42  ").deviceId)
    }

    @Test
    fun blankReturnsEmptyResult() {
        assertNull(QrCodeParser.parse("").deviceId)
        assertNull(QrCodeParser.parse("").qrId)
        assertNull(QrCodeParser.parse("   ").deviceId)
    }

    @Test
    fun idQueryKindsAllYieldQrId() {
        assertEquals("a1", QrCodeParser.parse("https://i.ilife798.com/q/1/b?id=a1").qrId)
        assertEquals("a2", QrCodeParser.parse("https://i.ilife798.com/q/1/s?id=a2").qrId)
        assertEquals("a3", QrCodeParser.parse("https://i.ilife798.com/q/1/billing?id=a3").qrId)
        assertEquals("a4", QrCodeParser.parse("https://i.ilife798.com/q/1/vip?id=a4").qrId)
        assertEquals("a5", QrCodeParser.parse("https://i.ilife798.com/q/1/wallet?id=a5").qrId)
    }

    @Test
    fun bmLinkUsesContentAfterFixedPrefix() {
        // 固定截取 "q/1/" 之后的内容作为 qrId
        assertEquals("bm?/tok-1", QrCodeParser.parse("https://i.ilife798.com/q/1/bm?/tok-1").qrId)
    }

    @Test
    fun unknownTextIsInvalid() {
        val result = QrCodeParser.parse("hello")
        assertNull(result.deviceId)
        assertNull(result.qrId)
    }
}
