package com.github.ilife798

/**
 * 跨平台的应用回到前台回调。Android 端在 Activity.onResume 时触发。
 */
object AppLifecycle {
    var onResumed: (() -> Unit)? = null

    fun notifyResumed() {
        onResumed?.invoke()
    }
}
