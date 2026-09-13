package com.github.ilife798

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

actual fun copyTextToClipboard(text: String): Boolean =
    try {
        val manager = ApplicationContext.instance.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText("ilife798", text))
        true
    } catch (_: Exception) {
        false
    }
