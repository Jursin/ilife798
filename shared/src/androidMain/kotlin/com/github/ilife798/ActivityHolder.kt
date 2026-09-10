package com.github.ilife798

import android.app.Activity
import java.lang.ref.WeakReference

object ActivityHolder {
    private var ref: WeakReference<Activity>? = null

    var current: Activity?
        get() = ref?.get()
        set(value) {
            ref = value?.let { WeakReference(it) }
        }
}
