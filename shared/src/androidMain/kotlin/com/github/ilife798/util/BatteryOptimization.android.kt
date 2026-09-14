package com.github.ilife798.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri
import com.github.ilife798.ApplicationContext
import com.github.ilife798.logDebug

// 直接请求“忽略电池优化”授权（系统弹窗），失败退回应用信息页。
@SuppressLint("BatteryLife")
actual fun openBatteryOptimizationSettings() {
    val context = ApplicationContext.instance
    val packageUri = "package:${context.packageName}".toUri()
    val intents =
        listOf(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri),
        )
    for (intent in intents) {
        if (startSafely(context, intent)) return
    }
}

// 读取系统的电池优化白名单状态；部分厂商 ROM 可能存在额外的后台限制。
actual fun isIgnoringBatteryOptimizations(): Boolean =
    try {
        val context = ApplicationContext.instance
        val manager = context.getSystemService(PowerManager::class.java)
        manager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    } catch (e: Exception) {
        logDebug("ILife798", "isIgnoringBatteryOptimizations: ${e.message}")
        false
    }

private fun startSafely(
    context: Context,
    intent: Intent,
): Boolean =
    try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        logDebug("ILife798", "start battery settings failed: ${intent.action} ${e.message}")
        false
    }
