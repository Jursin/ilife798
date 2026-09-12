package com.github.ilife798

/** 运行状态通知携带的 Intent action 与 extra，供 MainActivity 解析。 */
object RunNotificationIntents {
    const val ACTION_STOP_DEVICE = "com.github.ilife798.action.STOP_DEVICE"
    const val ACTION_OPEN_HOME = "com.github.ilife798.action.OPEN_HOME"
    const val ACTION_OPEN_TASKS = "com.github.ilife798.action.OPEN_TASKS"
    const val ACTION_STOP_TASKS = "com.github.ilife798.action.STOP_TASKS"
    const val EXTRA_DEVICE_ID = "com.github.ilife798.extra.DEVICE_ID"
}
