package com.github.ilife798.data.api

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.github.ilife798.data.model.BillRecord
import com.github.ilife798.data.model.RechargeProduct
import com.github.ilife798.data.model.RefundProgress
import com.github.ilife798.data.model.WalletAccount
import com.github.ilife798.util.currentTimeMillis

class IlifeApi(private val client: HttpClient = createHttpClient()) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // 服务端在登录态失效时对所有需鉴权接口返回 code = -99
    var onSessionExpired: (() -> Unit)? = null

    private suspend fun bodyJson(response: HttpResponse): JsonObject {
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        checkSessionExpired(body)
        return body
    }

    private fun checkSessionExpired(body: JsonObject) {
        if (body["code"]?.jsonPrimitive?.content?.toIntOrNull() == -99) {
            onSessionExpired?.invoke()
        }
    }

    // --- Captcha ---
    suspend fun getCaptcha(key: String): ByteArray {
        val timestamp = currentTimeMillis()
        val response = client.get("${ApiConfig.baseUrl}/captcha/") {
            parameter("s", key)
            parameter("r", timestamp)
        }
        return response.bodyAsBytes()
    }

    fun newCaptchaKey(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..10).map { chars.random() }.joinToString("")
    }

    // --- Login ---
    suspend fun sendSmsCode(phone: String, graphCode: String, captchaKey: String): String {
        return try {
            val response = client.post("${ApiConfig.baseUrl}/acc/login/code") {
                contentType(ContentType.Application.Json)
                setBody("""{"un":"$phone","authCode":"$graphCode","s":"$captchaKey"}""")
            }
            response.bodyAsText()
        } catch (e: Exception) {
            """{"code":-1,"msg":"${e.message}"}"""
        }
    }

    suspend fun login(phone: String, smsCode: String, isAlipay: Boolean): LoginResult {
        val appType = if (isAlipay) "1,5" else "1,1"
        val response = client.post("${ApiConfig.baseUrl}/acc/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"openCode":"","authCode":"$smsCode","un":"$phone","cid":"${ApiConfig.cid}"}""")
            header("ApplicationType", appType)
        }
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        val code = body["code"]?.jsonPrimitive?.content?.toIntOrNull() ?: -1
        if (code != 0) return LoginResult(false, error = body["msg"]?.jsonPrimitive?.content ?: "登录失败")
        val al = body["data"]?.jsonObject?.get("al")?.jsonObject
        val token = al?.get("token")?.jsonPrimitive?.content ?: ""
        val uid = al?.get("uid")?.jsonPrimitive?.content ?: ""
        val eid = al?.get("eid")?.jsonPrimitive?.content ?: ""
        return LoginResult(true, token = token, uid = uid, eid = eid)
    }

    // --- Account Info ---
    suspend fun getAccountInfo(token: String): AccountInfoDto? {
        val response = client.get("${ApiConfig.baseUrl}/ui/app/master") {
            header("Authorization", token)
            header("ApplicationType", "1,1")
        }
        val body = bodyJson(response)
        val data = body["data"] as? JsonObject ?: return null
        val account = data["account"] as? JsonObject ?: return null
        return AccountInfoDto(
            img = account["img"]?.jsonPrimitive?.content ?: "",
            name = account["name"]?.jsonPrimitive?.content ?: "",
            pn = account["pn"]?.jsonPrimitive?.content ?: ""
        )
    }

    // --- Account settings ---
    suspend fun setUseScore(token: String, useScore: Int = 1, appType: String = "1,1"): DevResult {
        val response = client.post("${ApiConfig.baseUrl}/acc/upt") {
            contentType(ContentType.Application.Json)
            setBody("""{"useScore":$useScore}""")
            header("Authorization", token)
            header("ApplicationType", appType)
        }
        return parseDevResult(response.bodyAsText())
    }

    // --- Devices ---
    suspend fun getMasterDevices(token: String): MasterResult {
        val response = client.get("${ApiConfig.baseUrl}/ui/app/master") {
            header("Authorization", token)
            header("ApplicationType", "1,1")
        }
        val body = bodyJson(response)
        val data = body["data"] as? JsonObject ?: return MasterResult()
        val account = data["account"] as? JsonObject
        val accountId = account?.get("id")?.jsonPrimitive?.content ?: ""
        val favos = data["favos"] as? JsonArray ?: return MasterResult(accountId)
        val devices = favos.map { item ->
            val obj = item.jsonObject
            val gene = obj["gene"]?.jsonObject
            val deviceStatus = obj["status"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val geneStatus = gene?.get("status")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val owner = obj["owner"]?.jsonObject
            val ownerId = owner?.get("id")?.jsonPrimitive?.content ?: ""
            DeviceDto(
                id = obj["id"]?.jsonPrimitive?.content ?: "",
                name = obj["name"]?.jsonPrimitive?.content ?: "",
                status = deviceStatus,
                geneStatus = geneStatus,
                ownerId = ownerId
            )
        }
        return MasterResult(accountId, devices)
    }

    suspend fun devStart(token: String, did: String, appType: String = "1,1", ptype: Int = 91): DevResult {
        val response = client.get("${ApiConfig.baseUrl}/dev/start") {
            parameter("did", did)
            parameter("upgrade", "true")
            parameter("ptype", ptype.toString())
            parameter("args", "")
            parameter("rcp", "false")
            parameter("cnt", "1")
            header("Authorization", token)
            header("ApplicationType", appType)
        }
        return parseDevResult(response.bodyAsText())
    }

    suspend fun devEnd(token: String, did: String, appType: String = "1,1"): DevResult {
        val response = client.get("${ApiConfig.baseUrl}/dev/end") {
            parameter("did", did)
            header("Authorization", token)
            header("ApplicationType", appType)
        }
        return parseDevResult(response.bodyAsText())
    }

    suspend fun devFavo(token: String, did: String, remove: Boolean): DevResult {
        val response = client.get("${ApiConfig.baseUrl}/dev/favo") {
            parameter("did", did)
            parameter("remove", if (remove) "1" else "0")
            header("Authorization", token)
            header("ApplicationType", "1,1")
        }
        return parseDevResult(response.bodyAsText())
    }

    suspend fun getDevStatus(token: String, did: String, appType: String = "1,1"): DevStatusResult? {
        val response = client.get("${ApiConfig.baseUrl}/ui/app/dev/status") {
            parameter("did", did)
            parameter("more", "false")
            header("Authorization", token)
            header("ApplicationType", appType)
        }
        val body = bodyJson(response)
        if (body["code"]?.jsonPrimitive?.content?.toIntOrNull() != 0) return null
        val data = body["data"] as? JsonObject ?: return null
        val device = data["device"]?.jsonObject ?: return null
        val deviceStatus = device["status"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        val gene = device["gene"]?.jsonObject
        val geneStatus = gene?.get("status")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        val user = data["user"]?.jsonObject
        val userId = user?.get("id")?.jsonPrimitive?.content ?: ""
        return DevStatusResult(deviceStatus = deviceStatus, geneStatus = geneStatus, userId = userId)
    }

    suspend fun getDevHome(token: String, did: String, appType: String = "1,1"): DevHomeResult {
        val response = client.get("${ApiConfig.baseUrl}/ui/app/dev/home/1") {
            parameter("did", did)
            parameter("apply", "6")
            header("Authorization", token)
            header("ApplicationType", appType)
        }
        val body = bodyJson(response)
        if (body["code"]?.jsonPrimitive?.content?.toIntOrNull() != 0) return DevHomeResult()
        val data = body["data"] as? JsonObject ?: return DevHomeResult()
        val deviceShare = data["deviceShare"]?.jsonObject
        val shareUserId = deviceShare?.get("sid")?.jsonPrimitive?.content ?: ""
        return DevHomeResult(shareUserId = shareUserId)
    }

    private fun parseDevResult(raw: String): DevResult {
        val body = json.parseToJsonElement(raw).jsonObject
        checkSessionExpired(body)
        val code = body["code"]?.jsonPrimitive?.content?.toIntOrNull() ?: -1
        val msg = body["msg"]?.jsonPrimitive?.content ?: ""
        return DevResult(code == 0, code, msg)
    }

    // --- Score/Tasks ---
    suspend fun getMissionList(token: String): MissionListResult {
        val response = client.get("${ApiConfig.baseUrl}/acc/score/mission-lst") {
            header("Authorization", token)
            header("ApplicationType", "1,5")
        }
        val body = bodyJson(response)
        val data = body["data"] as? JsonObject ?: return MissionListResult(emptyList())
        val accScore = data["accScoreRsp"]?.jsonObject
        val validScore = accScore?.get("validScore")?.jsonPrimitive?.content?.toIntOrNull()
        val totalScore = accScore?.get("totalScore")?.jsonPrimitive?.content?.toIntOrNull()
        val weekMask = accScore?.get("daily")?.jsonObject?.get("week")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        val dailyRsp = data["dailyRSP"]?.jsonObject
        val dailyAdId = dailyRsp?.get("adId")?.jsonPrimitive?.content ?: ""
        val dailyScore = dailyRsp?.get("score")?.jsonPrimitive?.content?.toIntOrNull() ?: 5

        // Parse limits[] -> map of refId to daily completed count
        val limitsArray = accScore?.get("limits")?.jsonArray ?: emptyList()
        val dailyDoneMap = mutableMapOf<String, Int>()
        for (item in limitsArray) {
            val obj = item.jsonObject
            val refId = obj["refId"]?.jsonPrimitive?.content ?: ""
            val limit = obj["limit"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            if (refId.isNotEmpty()) dailyDoneMap[refId] = limit
        }

        val missions = data["missions"]?.jsonArray ?: return MissionListResult(emptyList(), validScore, weekMask, dailyAdId, dailyScore, totalScore)
        val seen = mutableSetOf<String>()
        val list = missions.mapNotNull { item ->
            val obj = item.jsonObject
            val refId = obj["refId"]?.jsonPrimitive?.content ?: ""
            if (refId.isBlank() || !seen.add(refId)) return@mapNotNull null
            val score = obj["score"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val limit = obj["limit"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            if (limit <= 0) return@mapNotNull null
            MissionDto(
                adId = refId,
                name = obj["name"]?.jsonPrimitive?.content ?: obj["title"]?.jsonPrimitive?.content ?: "任务",
                score = score,
                limit = limit,
                dailyCompleted = dailyDoneMap[refId] ?: 0
            )
        }

        // Add daily sign-in as first item
        val allMissions = if (dailyAdId.isNotBlank()) {
            listOf(MissionDto(adId = dailyAdId, name = "每日签到", score = dailyScore, limit = 1, isDailySignin = true)) + list
        } else {
            list
        }

        return MissionListResult(allMissions, validScore, weekMask, dailyAdId, dailyScore, totalScore)
    }

    suspend fun executeMission(token: String, uid: String, adId: String): DevResult {
        val sign = Signer.sign(adId, token, uid)
        val response = client.post("${ApiConfig.baseUrl}/acc/score/score-send?sign=$sign&s=true") {
            contentType(ContentType.Application.Json)
            setBody("""{"adId":"$adId","type":101}""")
            header("Authorization", token)
            header("ApplicationType", "1,5")
        }
        return parseDevResult(response.bodyAsText())
    }

    suspend fun signIn(token: String, uid: String, weekDay: Int, adId: String): DevResult {
        val sign = Signer.sign(adId, token, uid)
        val response = client.post("${ApiConfig.baseUrl}/acc/score/score-send?sign=$sign&s=true") {
            contentType(ContentType.Application.Json)
            setBody("""{"weekDay":$weekDay,"adId":"$adId"}""")
            header("Authorization", token)
            header("ApplicationType", "1,5")
        }
        return parseDevResult(response.bodyAsText())
    }

    suspend fun getScoreList(token: String, page: Int = 0, size: Int = 20): ScoreListResult {
        val response = client.get("${ApiConfig.baseUrl}/acc/score/score-lst") {
            parameter("page", page.toString())
            parameter("size", size.toString())
            parameter("hasCount", "1")
            header("Authorization", token)
            header("ApplicationType", "1,5")
        }
        val body = bodyJson(response)
        val data = body["data"] as? JsonArray ?: return ScoreListResult(emptyList(), 0)
        val total = body["size"]?.jsonPrimitive?.content?.toIntOrNull() ?: data.size
        val records = data.map { item ->
            val obj = item.jsonObject
            val nested = obj["data"]?.jsonObject
            val score = readScoreInt(obj, nested)
            val adId = nested?.get("adId")?.jsonPrimitive?.content ?: ""
            val isExpense = obj["type"]?.jsonPrimitive?.content?.toIntOrNull() == 107
                || nested?.containsKey("spend") == true
            ScoreDto(
                score = if (isExpense) -score else score,
                name = nested?.get("adName")?.jsonPrimitive?.content
                    ?: obj["msg"]?.jsonPrimitive?.content
                    ?: obj["typeName"]?.jsonPrimitive?.content
                    ?: "积分变动",
                time = obj["ctime"]?.jsonPrimitive?.content ?: "",
                adId = adId
            )
        }
        return ScoreListResult(records, total)
    }

    // --- Wallet & Bills ---
    suspend fun getWalletOwner(token: String, eid: String = "", all: Boolean = true): WalletOwnerResult {
        val response = client.get("${ApiConfig.baseUrl}/acc/wallet/owner") {
            if (eid.isNotEmpty()) parameter("eid", eid)
            parameter("all", all.toString())
            header("Authorization", token)
            header("ApplicationType", "1,1")
        }
        val body = bodyJson(response)
        if (body["code"]?.jsonPrimitive?.content?.toIntOrNull() != 0) return WalletOwnerResult()
        val data = body["data"] as? JsonObject ?: return WalletOwnerResult()
        val aw = data["aw"] as? JsonObject
        val active = aw?.let { parseWallet(it) }
        val activeEid = active?.eid ?: ""
        val eps = (data["eps"] as? JsonArray)?.mapNotNull { it as? JsonObject }?.map { parseWallet(it) } ?: emptyList()
        val charge = data["charge"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1
        val refund = data["refund"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1
        val rfd = data["rfdProg"] as? JsonObject
        val refundProgress = rfd?.let {
            RefundProgress(
                ctime = it["ctime"]?.jsonPrimitive?.content?.toLongOrNull() ?: -1L,
                count = it["count"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                total = it["total"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                fail = it["fail"]?.jsonPrimitive?.content?.toIntOrNull()
            )
        }
        return WalletOwnerResult(active, eps, activeEid, charge == 1, refund == 1, refundProgress)
    }

    suspend fun getWalletDetail(token: String, id: String, eid: String = ""): WalletAccount? {
        if (id.isEmpty()) return null
        val response = client.get("${ApiConfig.baseUrl}/acc/wallet/detail") {
            parameter("id", id)
            if (eid.isNotEmpty()) parameter("eid", eid)
            header("Authorization", token)
            header("ApplicationType", "1,1")
        }
        val body = bodyJson(response)
        if (body["code"]?.jsonPrimitive?.content?.toIntOrNull() != 0) return null
        val data = body["data"] as? JsonObject ?: return null
        return parseWallet(data)
    }

    private fun parseWallet(obj: JsonObject): WalletAccount {
        val ep = obj["ep"] as? JsonObject
        val setting = ep?.get("setting") as? JsonObject
        val owner = obj["owner"] as? JsonObject
        return WalletAccount(
            id = obj["id"]?.jsonPrimitive?.content ?: "",
            eid = ep?.get("id")?.jsonPrimitive?.content ?: "",
            ownerId = owner?.get("id")?.jsonPrimitive?.content ?: "",
            name = ep?.get("name")?.jsonPrimitive?.content ?: obj["name"]?.jsonPrimitive?.content ?: "",
            abbr = ep?.get("abbr")?.jsonPrimitive?.content ?: "",
            olCash = obj["olCash"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
            olGift = obj["olGift"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
            ofCash = obj["ofCash"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
            ofGift = obj["ofGift"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
            total = obj["total"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
            auth = obj["auth"]?.jsonPrimitive?.content?.toBoolean() ?: false,
            chargeEnabled = (setting?.get("olcharge")?.jsonPrimitive?.content?.toIntOrNull() ?: 1) == 1,
            refundEnabled = (setting?.get("olrefund")?.jsonPrimitive?.content?.toIntOrNull() ?: 1) == 1
        )
    }

    suspend fun getBillList(token: String, page: Int = 0, size: Int = 20, status: Int = 3): BillListResult {
        val response = client.get("${ApiConfig.baseUrl}/bill/lst-owner") {
            parameter("page", page.toString())
            parameter("size", size.toString())
            parameter("hasCount", (page == 0).toString())
            parameter("status", status.toString())
            header("Authorization", token)
            header("ApplicationType", "1,1")
        }
        val body = bodyJson(response)
        val data = body["data"] as? JsonArray ?: return BillListResult(emptyList(), 0)
        val total = body["size"]?.jsonPrimitive?.content?.toIntOrNull() ?: data.size
        val records = data.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val bm = (obj["dev"] as? JsonObject)?.get("bm") as? JsonObject
            BillRecord(
                id = obj["id"]?.jsonPrimitive?.content ?: "",
                cata = obj["cata"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                type = obj["type"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                msg = obj["msg"]?.jsonPrimitive?.content ?: "",
                status = obj["status"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                dir = obj["dir"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1,
                payment = obj["payment"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                time = obj["utime"]?.jsonPrimitive?.content?.toLongOrNull()
                    ?: obj["ctime"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                deviceType = bm?.get("dtype")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            )
        }
        return BillListResult(records, total)
    }

    suspend fun getRechargeProducts(token: String, eid: String): List<RechargeProduct> {
        if (eid.isEmpty()) return emptyList()
        val response = client.get("${ApiConfig.baseUrl}/prd/lst") {
            parameter("eid", eid)
            parameter("type", "1")
            parameter("status", "1")
            parameter("all", "false")
            parameter("did", "")
            parameter("page", "0")
            parameter("size", "100")
            parameter("hasCount", "false")
            header("Authorization", token)
            header("ApplicationType", "1,1")
        }
        val body = bodyJson(response)
        val data = body["data"] as? JsonArray ?: return emptyList()
        return data.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val id = obj["id"]?.jsonPrimitive?.content ?: ""
            if (id.isEmpty()) return@mapNotNull null
            RechargeProduct(
                id = id,
                name = obj["name"]?.jsonPrimitive?.content ?: "充值",
                price = obj["curPrice"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                originalPrice = obj["ogiPrice"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                description = obj["desc"]?.jsonPrimitive?.content ?: ""
            )
        }.distinctBy { "${it.name}|${it.price}|${it.originalPrice}" }
    }

    suspend fun createRechargeOrder(token: String, eid: String, ownerId: String, productId: String): String? {
        if (eid.isEmpty() || ownerId.isEmpty() || productId.isEmpty()) return null
        val payload = """{"cata":1,"contact":{"id":"$ownerId"},"ep":{"id":"$eid"},"note":"钱包充值","owner":{"id":"$ownerId"},"prds":[{"id":"$productId","count":1}]}"""
        val response = client.post("${ApiConfig.baseUrl}/bill/save") {
            contentType(ContentType.Application.Json)
            setBody(payload)
            header("Authorization", token)
            header("ApplicationType", "1,1")
        }
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        checkSessionExpired(body)
        if (body["code"]?.jsonPrimitive?.content?.toIntOrNull() != 0) return null
        return body["data"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() && it != "null" }
    }

    suspend fun prepayAlipay(token: String, orderId: String): String? {
        if (orderId.isEmpty()) return null
        val response = client.get("${ApiConfig.baseUrl}/trans/prepay/21") {
            parameter("id", orderId)
            header("Authorization", token)
            header("ApplicationType", "1,1")
        }
        val body = bodyJson(response)
        if (body["code"]?.jsonPrimitive?.content?.toIntOrNull() != 0) return null
        return body["data"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() && it != "null" }
    }

    // 钱包退款：POST /acc/wallet/refund，type=23（支付宝小程序，与官方 RefundActivity 一致）
    suspend fun refundWallet(token: String, eid: String): DevResult {
        if (eid.isEmpty()) return DevResult(false, -1, "钱包信息缺失")
        val payload = """{"eid":"$eid","type":23}"""
        val response = client.post("${ApiConfig.baseUrl}/acc/wallet/refund") {
            contentType(ContentType.Application.Json)
            setBody(payload)
            header("Authorization", token)
            header("ApplicationType", "1,1")
        }
        return parseDevResult(response.bodyAsText())
    }

    private fun readScoreInt(obj: JsonObject, nested: JsonObject?): Int {
        val keys = listOf("score", "spend", "changeScore", "change_score", "amount", "value", "num", "points")
        for (key in keys) {
            obj[key]?.jsonPrimitive?.content?.toIntOrNull()?.let { return it }
            nested?.get(key)?.jsonPrimitive?.content?.toIntOrNull()?.let { return it }
        }
        return 0
    }
}

data class LoginResult(val success: Boolean, val token: String = "", val uid: String = "", val eid: String = "", val error: String = "")
data class AccountInfoDto(val img: String = "", val name: String = "", val pn: String = "")
data class DeviceDto(val id: String, val name: String, val status: Int = 0, val geneStatus: Int = 0, val ownerId: String = "")
data class MasterResult(val accountId: String = "", val devices: List<DeviceDto> = emptyList())
data class DevHomeResult(val shareUserId: String = "")
data class DevStatusResult(val deviceStatus: Int, val geneStatus: Int, val userId: String = "")
data class DevResult(val success: Boolean, val code: Int, val message: String)
data class MissionDto(val adId: String, val name: String, val score: Int, val limit: Int, val dailyCompleted: Int = 0, val isDailySignin: Boolean = false)
data class MissionListResult(val missions: List<MissionDto>, val validScore: Int? = null, val weekMask: Int = 0, val dailyAdId: String = "", val dailyScore: Int = 5, val totalScore: Int? = null)
data class ScoreDto(val score: Int, val name: String, val time: String, val adId: String = "")
data class ScoreListResult(val records: List<ScoreDto>, val total: Int)
data class WalletOwnerResult(
    val active: WalletAccount? = null,
    val wallets: List<WalletAccount> = emptyList(),
    val activeEid: String = "",
    val chargeEnabled: Boolean = true,
    val refundEnabled: Boolean = true,
    val refundProgress: RefundProgress? = null
)
data class BillListResult(val records: List<BillRecord>, val total: Int)
