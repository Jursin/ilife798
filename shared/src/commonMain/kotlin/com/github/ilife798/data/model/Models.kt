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
    val deviceStatus: Int = 1
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
    val scoreRecords: List<ScoreRecord> = emptyList(),
    val wallets: List<WalletAccount> = emptyList(),
    val activeWalletId: String = "",
    val billRecords: List<BillRecord> = emptyList(),
    val taskCompleted: Boolean = false,
    val taskLogs: List<String> = emptyList(),
    val dynamicColor: Boolean = false,
    val floatingNav: Boolean = false,
    val appBlur: Boolean = true,
    val predictiveBackEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.System,
    val accountInfo: AccountInfo = AccountInfo(),
    val weekMask: Int = 0
)
