package com.github.ilife798

import android.content.Context
import java.lang.ref.WeakReference

object ApplicationContext {
    private var instanceRef: WeakReference<Context>? = null

    var instance: Context
        get() = instanceRef?.get() ?: throw IllegalStateException("ApplicationContext not initialized")
        set(value) {
            instanceRef = WeakReference(value)
        }
}
