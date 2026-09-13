package com.github.ilife798.data.api

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

fun JsonObject.strOrNull(key: String): String? = this[key]?.jsonPrimitive?.content

fun JsonObject.str(
    key: String,
    def: String = "",
): String = strOrNull(key) ?: def

fun JsonObject.intOrNull(key: String): Int? = this[key]?.jsonPrimitive?.content?.toIntOrNull()

fun JsonObject.int(
    key: String,
    def: Int = 0,
): Int = intOrNull(key) ?: def

fun JsonObject.longOrNull(key: String): Long? = this[key]?.jsonPrimitive?.content?.toLongOrNull()

fun JsonObject.long(
    key: String,
    def: Long = 0L,
): Long = longOrNull(key) ?: def

fun JsonObject.doubleOrNull(key: String): Double? = this[key]?.jsonPrimitive?.content?.toDoubleOrNull()

fun JsonObject.double(
    key: String,
    def: Double = 0.0,
): Double = doubleOrNull(key) ?: def

fun JsonObject.bool(
    key: String,
    def: Boolean = false,
): Boolean = this[key]?.jsonPrimitive?.content?.toBoolean() ?: def

fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

fun JsonObject.arr(key: String): JsonArray? = this[key] as? JsonArray
