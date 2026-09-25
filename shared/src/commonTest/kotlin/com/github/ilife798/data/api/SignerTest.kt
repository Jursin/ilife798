package com.github.ilife798.data.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SignerTest {
    @Test
    fun producesLowerCaseHexMd5() {
        ApiConfig.init("https://example.com", "salt", "cid")
        val sign = Signer.sign(adId = "ad1", token = "token-12345678", uid = "uid-12345678")
        assertEquals(32, sign.length)
        assertTrue(sign.all { it in "0123456789abcdef" })
    }

    @Test
    fun differsWhenInputChanges() {
        ApiConfig.init("https://example.com", "salt", "cid")
        val a = Signer.sign("ad1", "token", "uid")
        val b = Signer.sign("ad2", "token", "uid")
        assertNotEquals(a, b)
    }

    @Test
    fun bucketsTimeIn30SecondIntervals() {
        ApiConfig.init("https://example.com", "salt", "cid")
        val base = 1790266560000L
        val a = Signer.signAt("ad", "token-12345678", "uid-12345678", base)
        val b = Signer.signAt("ad", "token-12345678", "uid-12345678", base + 29999)
        val c = Signer.signAt("ad", "token-12345678", "uid-12345678", base + 30000)
        assertEquals(a, b)
        assertNotEquals(a, c)
    }
}
