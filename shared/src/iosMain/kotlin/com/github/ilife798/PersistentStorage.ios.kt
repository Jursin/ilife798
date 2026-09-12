package com.github.ilife798

import platform.Foundation.NSUserDefaults

actual class PersistentStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun saveString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }

    actual fun getString(key: String): String? {
        return defaults.stringForKey(key)
    }

    actual fun saveBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
    }

    actual fun getBoolean(key: String): Boolean {
        return defaults.boolForKey(key)
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return if (defaults.objectForKey(key) == null) defaultValue else defaults.boolForKey(key)
    }

    actual fun saveInt(key: String, value: Int) {
        defaults.setInteger(value.toLong(), forKey = key)
    }

    actual fun getInt(key: String, defaultValue: Int): Int {
        return if (defaults.objectForKey(key) == null) defaultValue else defaults.integerForKey(key).toInt()
    }
}
