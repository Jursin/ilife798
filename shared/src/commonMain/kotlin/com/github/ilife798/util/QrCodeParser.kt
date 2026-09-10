package com.github.ilife798.util

object QrCodeParser {

    private val digits = Regex("^\\d+$")
    private val idQuery = Regex("^(http|https)://.+/q/\\d+/(b|s|billing|vip|wallet)\\?id=\\w+")
    private val bm = Regex("^(http|https)://.+/q/\\d+/bm\\?/[a-zA-Z0-9-]+.*")
    private val generic = Regex("^(http|https)://.+/q/\\d+/[a-zA-Z0-9-]+.*")

    /**
     * @param deviceId 可直接作为设备编号使用
     * @param qrId 需通过 /qr/use 换取设备信息
     */
    data class Result(val deviceId: String? = null, val qrId: String? = null)

    fun parse(raw: String): Result {
        val text = raw.trim()
        if (text.isEmpty()) return Result()
        if (digits.matches(text)) return Result(deviceId = text)
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
        return Result(qrId = text)
    }
}
