package com.github.ilife798

import android.widget.Toast

actual fun showToast(message: String) {
    Toast.makeText(ApplicationContext.instance, message, Toast.LENGTH_SHORT).show()
}
