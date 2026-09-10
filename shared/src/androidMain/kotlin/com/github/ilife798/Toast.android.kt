package com.github.ilife798

import android.os.Handler
import android.os.Looper
import android.widget.Toast

private val mainHandler = Handler(Looper.getMainLooper())

actual fun showToast(message: String) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        Toast.makeText(ApplicationContext.instance, message, Toast.LENGTH_SHORT).show()
    } else {
        mainHandler.post {
            Toast.makeText(ApplicationContext.instance, message, Toast.LENGTH_SHORT).show()
        }
    }
}
