package com.github.ilife798.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val phone: String = "",
    val isLoggedIn: Boolean = false,
    val pointsLoginDone: Boolean = false,
    val uid: String = "",
    val eid: String = "",
    val token: String = "",
    val appToken: String = ""
)

@Serializable
data class Device(
    val id: String,
    val name: String,
    val status: Int = 0,
    val geneStatus: Int = 0,
    val deviceStatus: Int = 1,
    val dtype: Int = 0
)

// 首页场景类型
enum class HomeDeviceType(val label: String, val deviceType: Int) {
    Laundry("洗烘", 10),
    HairDrying("吹风", 20),
    Drinking("饮水", 8),
    Shower("淋浴", 6);

    companion object {
        fun fromDeviceType(dtype: Int): HomeDeviceType? =
            entries.firstOrNull { it.deviceType == dtype }
    }
}

// 设备可选项（来自 /ui/app/dev/status?more=true 的 bm.parts）
@Serializable
data class DeviceOption(
    val mode: Int = -1,
    val name: String = "",
    val rate: Double = 0.0,
    val maxT: Int = 0
)

// 货道（售货机/加液），对应 gs.items
@Serializable
data class DeviceGoods(val pos: Int = 0, val out: Int = 0)

// 设备启动可选项集合
@Serializable
data class DeviceStartOptions(
    val parts: List<DeviceOption> = emptyList(),
    val subCount: Int = 0,
    val goods: List<DeviceGoods> = emptyList()
)

// 待用户确认启动的设备（含可选项）
data class DevicePendingStart(
    val deviceId: String,
    val deviceName: String,
    val options: DeviceStartOptions
)

@Serializable
data class PointsInfo(
    val available: Int? = null,
    val total: Int? = null
)

@Serializable
data class TaskRecord(
    val timestamp: Long,
    val message: String
)

@Serializable
data class MissionInfo(
    val adId: String = "",
    val name: String = "",
    val score: Int = 0,
    val limit: Int = 1,
    val dailyCompleted: Int = 0,
    val isDailySignin: Boolean = false,
    val sourceToken: String = ""
)

@Serializable
data class ScoreRecord(
    val score: Int = 0,
    val name: String = "",
    val time: String = ""
)

enum class ScoreFilter(val src: Int?) { All(null), Income(101), Expense(105) }

@Serializable
data class WalletAccount(
    val id: String = "",
    val eid: String = "",
    val ownerId: String = "",
    val name: String = "",
    val olCash: Double = 0.0,
    val olGift: Double = 0.0,
    val ofCash: Double = 0.0,
    val ofGift: Double = 0.0,
    val total: Double = 0.0,
    val auth: Boolean = false,
    val chargeEnabled: Boolean = true,
    val refundEnabled: Boolean = true
) {
    val refundable: Double get() = if (auth) olCash + ofCash else olCash
}

@Serializable
data class RechargeProduct(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val originalPrice: Double = 0.0
)

@Serializable
data class RefundProgress(
    val ctime: Long = -1L,
    val count: Int = 0,
    val total: Double = 0.0,
    val fail: Int? = null
) {
    val active: Boolean get() = ctime != -1L
}

@Serializable
data class SpendingStats(
    val today: Double = 0.0,
    val yesterday: Double = 0.0,
    val monthAverage: Double = 0.0
)

@Serializable
data class BillRecord(
    val id: String = "",
    val cata: Int = 0,
    val type: Int = 0,
    val msg: String = "",
    val status: Int = 0,
    val dir: Int = 1,
    val payment: Double = 0.0,
    val time: Long = 0L
)

enum class ThemeMode { Light, Dark, System }

@Serializable
data class AccountInfo(
    val img: String = "",
    val name: String = "",
    val pn: String = ""
)

@Serializable
data class AppState(
    val account: Account = Account(),
    val devices: List<Device> = emptyList(),
    val points: PointsInfo = PointsInfo(),
    val taskRecords: List<TaskRecord> = emptyList(),
    val wallets: List<WalletAccount> = emptyList(),
    val activeWalletId: String = "",
    val billRecords: List<BillRecord> = emptyList(),
    val taskCompleted: Boolean = false,
    val taskLogs: List<String> = emptyList(),
    val dynamicColor: Boolean = true,
    val floatingNav: Boolean = false,
    val appBlur: Boolean = true,
    val predictiveBackEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.System,
    val accountInfo: AccountInfo = AccountInfo(),
    val weekMask: Int = 0
)
