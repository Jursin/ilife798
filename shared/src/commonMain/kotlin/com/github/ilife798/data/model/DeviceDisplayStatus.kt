package com.github.ilife798.data.model

// 设备展示状态（首页与详情页状态徽标共用），文案与配色在 UI 层按枚举映射
enum class DeviceDisplayStatus(
    val label: String,
) {
    Offline("离线"),
    Online("在线"),
    Disabled("禁用"),
    Starting("启动中"),
    Running("运行中"),
}

// 具备“启动中”过渡态的设备类型（洗衣机/烘干/饮水/充电桩）
private val STARTING_DTYPES = setOf(10, 80, 90, 120)

fun deviceDisplayStatus(
    deviceStatus: Int,
    geneStatus: Int,
    dtype: Int,
): DeviceDisplayStatus =
    when {
        deviceStatus == 0 -> DeviceDisplayStatus.Offline
        geneStatus == 99 -> DeviceDisplayStatus.Online
        geneStatus == 98 -> DeviceDisplayStatus.Disabled
        (geneStatus == 1 || geneStatus == 30) && dtype in STARTING_DTYPES -> DeviceDisplayStatus.Starting
        else -> DeviceDisplayStatus.Running
    }
