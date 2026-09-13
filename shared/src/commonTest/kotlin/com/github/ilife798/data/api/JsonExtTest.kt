package com.github.ilife798.data.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsonExtTest {
    private val obj =
        Json
            .parseToJsonElement(
                """
                {
                  "s": "text",
                  "i": 42,
                  "d": 1.5,
                  "b": true,
                  "o": { "id": "x" },
                  "a": [1, 2]
                }
                """.trimIndent(),
            ).jsonObject

    @Test
    fun stringValues() {
        assertEquals("text", obj.str("s"))
        assertEquals("", obj.str("missing"))
        assertEquals("fallback", obj.str("missing", "fallback"))
        assertNull(obj.strOrNull("missing"))
    }

    @Test
    fun numberAndBooleanValues() {
        assertEquals(42, obj.int("i"))
        assertEquals(42, obj.intOrNull("i"))
        assertEquals(7, obj.int("missing", 7))
        assertNull(obj.intOrNull("missing"))
        assertEquals(1.5, obj.double("d"))
        assertEquals(1.5, obj.doubleOrNull("d"))
        assertEquals(false, obj.bool("b").not())
        assertEquals(false, obj.bool("missing"))
    }

    @Test
    fun nestedObjectAndArray() {
        assertEquals("x", obj.obj("o")?.str("id"))
        assertEquals(2, obj.arr("a")?.size)
        assertNull(obj.obj("missing"))
        assertNull(obj.arr("s"))
    }
}
