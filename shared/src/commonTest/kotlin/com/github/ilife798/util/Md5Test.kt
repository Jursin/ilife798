package com.github.ilife798.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Md5Test {
    @Test
    fun emptyInput() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", md5(""))
    }

    @Test
    fun knownVectors() {
        assertEquals("900150983cd24fb0d6963f7d28e17f72", md5("abc"))
        assertEquals("5d41402abc4b2a76b9719d911017c592", md5("hello"))
    }

    @Test
    fun lowerCaseHexOfFixedLength() {
        val hash = md5("慧生活798")
        assertEquals(32, hash.length)
        assertTrue(hash.all { it in "0123456789abcdef" })
    }
}
