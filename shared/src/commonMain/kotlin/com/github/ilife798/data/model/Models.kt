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
    val ownerId: String = "",
    val shareUserId: String = ""
)

@Serializable
data class PointsInfo(
    val available: Int? = null,
    val todaySpent: Double? = null
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
    val isDailySignin: Boolean = false
)

@Serializable
data class ScoreRecord(
    val score: Int = 0,
    val name: String = "",
    val time: String = ""
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
