package com.github.ilife798.util

// 解析设备链接/编号的通用入口：内容可能来自二维码扫描或 NFC 标签（两者同源）。
// 纯数字=设备编号；/q/{n}/{kind}?id= 经 /qr/use 换取；
// bm?/ 路径取固定前缀后内容；其余 /q/ 路径末段为设备编号；
// 另支持应用自身深链 ilife798s://i/device/(nav|nfc)?id=（NFC 标签常见写法，id 即设备编号）。
object DeviceLinkParser {
    private val digits = Regex("^\\d+$")
    private val appLink = Regex("^ilife798s://i/device/(nav|nfc)\\?id=[^&#]+(&.*)?$")
    private val idQuery = Regex("^(http|https)://.+/q/\\d+/(b|s|billing|vip|wallet)\\?id=\\w+")
    private val bm = Regex("^(http|https)://.+/q/\\d+/bm\\?/[a-zA-Z0-9-]+.*")
    private val generic = Regex("^(http|https)://.+/q/\\d+/[a-zA-Z0-9-]+.*")

    // 设备编号格式（AddDeviceActivity → p.w）：12-16 位小写字母和数字
    private val deviceIdFormat = Regex("[0-9a-z]{12,16}")

    // 手动输入的设备编号是否合法
    fun isValidDeviceId(id: String): Boolean = deviceIdFormat.matches(id)

    // @param deviceId 可直接作为设备编号使用
    // @param qrId 需通过 /qr/use 换取设备信息
    data class Result(
        val deviceId: String? = null,
        val qrId: String? = null,
    )

    fun parse(raw: String): Result {
        val text = raw.trim()
        if (text.isEmpty()) return Result()
        if (digits.matches(text)) return Result(deviceId = text)
        if (appLink.matches(text)) {
            val id = text.substringAfter("?id=", "").substringBefore("&").trim()
            return if (id.isEmpty()) Result() else Result(deviceId = id)
        }
        if (idQuery.matches(text)) {
            val id = text.substringAfter("?id=", "").trim()
            return if (id.isEmpty()) Result() else Result(qrId = id)
        }
        if (bm.matches(text)) {
            val token = text.substringAfter("q/1/", "").trim()
            return if (token.isEmpty()) Result() else Result(qrId = token)
        }
        if (generic.matches(text)) {
            val deviceId = text.substringAfterLast("/").trim()
            return if (deviceId.isEmpty()) Result() else Result(deviceId = deviceId)
        }
        // 未知内容视为无效
        return Result()
    }
}
