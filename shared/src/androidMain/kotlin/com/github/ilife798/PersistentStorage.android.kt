package com.github.ilife798

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

actual class PersistentStorage(
    context: Context,
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ilife798_prefs", Context.MODE_PRIVATE)

    actual fun saveString(
        key: String,
        value: String,
    ) {
        prefs.edit(commit = true) { putString(key, value) }
    }

    actual fun getString(key: String): String? = prefs.getString(key, null)

    actual fun saveBoolean(
        key: String,
        value: Boolean,
    ) {
        prefs.edit(commit = true) { putBoolean(key, value) }
    }

    actual fun getBoolean(key: String): Boolean = prefs.getBoolean(key, false)

    actual fun getBoolean(
        key: String,
        defaultValue: Boolean,
    ): Boolean = prefs.getBoolean(key, defaultValue)

    actual fun saveInt(
        key: String,
        value: Int,
    ) {
        prefs.edit(commit = true) { putInt(key, value) }
    }

    actual fun getInt(
        key: String,
        defaultValue: Int,
    ): Int = prefs.getInt(key, defaultValue)
}
