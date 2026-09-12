package com.github.ilife798

import android.os.Handler
import android.os.Looper
import android.widget.Toast

private val mainHandler = Handler(Looper.getMainLooper())
private var currentToast: Toast? = null

actual fun showToast(message: String) {
    val show = {
        currentToast?.cancel()
        val toast = Toast.makeText(ApplicationContext.instance, message, Toast.LENGTH_SHORT)
        currentToast = toast
        toast.show()
    }
    if (Looper.myLooper() == Looper.getMainLooper()) {
        show()
    } else {
        mainHandler.post(show)
    }
}

actual fun dismissToast() {
    val dismiss = {
        currentToast?.cancel()
        currentToast = null
    }
    if (Looper.myLooper() == Looper.getMainLooper()) {
        dismiss()
    } else {
        mainHandler.post(dismiss)
    }
}
