package com.github.ilife798

import platform.Foundation.NSUserDefaults

// iOS 持久化：系统键值存储（标准 suite）。key 统一加前缀防与其它存储碰撞；
// 首次读取缺失返回 null/默认值；值域为 String/Boolean/Int 封装。
actual class PersistentStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun saveString(
        key: String,
        value: String,
    ) {
        defaults.setObject(value, forKey = "$KEY_PREFIX$key")
    }

    actual fun getString(key: String): String? = defaults.stringForKey("$KEY_PREFIX$key")

    actual fun saveBoolean(
        key: String,
        value: Boolean,
    ) {
        defaults.setBool(value, forKey = "$KEY_PREFIX$key")
    }

    actual fun getBoolean(key: String): Boolean = defaults.boolForKey("$KEY_PREFIX$key")

    actual fun getBoolean(
        key: String,
        defaultValue: Boolean,
    ): Boolean =
        if (defaults.objectForKey("$KEY_PREFIX$key") == null) {
            defaultValue
        } else {
            defaults.boolForKey("$KEY_PREFIX$key")
        }

    actual fun saveInt(
        key: String,
        value: Int,
    ) {
        defaults.setInteger(value.toLong(), forKey = "$KEY_PREFIX$key")
    }

    actual fun getInt(
        key: String,
        defaultValue: Int,
    ): Int =
        if (defaults.objectForKey("$KEY_PREFIX$key") == null) {
            defaultValue
        } else {
            defaults.integerForKey("$KEY_PREFIX$key").toInt()
        }

    private companion object {
        const val KEY_PREFIX = "ilife798."
    }
}
