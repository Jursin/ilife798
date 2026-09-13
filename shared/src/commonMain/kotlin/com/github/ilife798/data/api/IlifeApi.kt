package com.github.ilife798.data.api

import com.github.ilife798.data.model.BillRecord
import com.github.ilife798.data.model.DeviceGoods
import com.github.ilife798.data.model.DeviceOption
import com.github.ilife798.data.model.DeviceStartOptions
import com.github.ilife798.data.model.RechargeProduct
import com.github.ilife798.data.model.RefundProgress
import com.github.ilife798.data.model.WalletAccount
import com.github.ilife798.util.currentTimeMillis
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class IlifeApi(
    private val client: HttpClient = createHttpClient(),
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    // 请求体序列化：保留默认值字段，保证与服务端约定一致
    private val requestJson = Json { encodeDefaults = true }

    // 服务端在登录态失效时对所有需鉴权接口返回 code = -99
    var onSessionExpired: (() -> Unit)? = null

    // 接口返回非 0 业务码时回调服务端错误信息，供上层直接以 Toast 展示
    var onApiError: ((String) -> Unit)? = null

    private suspend fun bodyJson(
        response: HttpResponse,
        notifyExpiry: Boolean = true,
        reportError: Boolean = true,
    ): JsonObject {
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        if (notifyExpiry) checkSessionExpired(body)
        if (reportError) reportIfError(body)
        return body
    }

    private fun checkSessionExpired(body: JsonObject) {
        if (body.intOrNull("code") == -99) onSessionExpired?.invoke()
    }

    private fun reportIfError(body: JsonObject) {
        val code = body.int("code")
        if (code == 0 || code == -99) return
        emitApiError(body.strOrNull("msg"))
    }

    private fun emitApiError(msg: String?) {
        val text = msg?.trim().orEmpty()
        if (text.isNotEmpty()) onApiError?.invoke(text)
    }

    // 图形验证码
    suspend fun getCaptcha(key: String): ByteArray {
        val timestamp = currentTimeMillis()
        val response =
            client.get("${ApiConfig.baseUrl}/captcha/") {
                parameter("s", key)
                parameter("r", timestamp)
            }
        return response.bodyAsBytes()
    }

    fun newCaptchaKey(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..10).map { chars.random() }.joinToString("")
    }

    // 登录
    suspend fun sendSmsCode(
        phone: String,
        graphCode: String,
        captchaKey: String,
    ): String =
        try {
            val response =
                client.post("${ApiConfig.baseUrl}/acc/login/code") {
                    contentType(ContentType.Application.Json)
                    setBody(requestJson.encodeToString(SmsCodeRequest(phone, graphCode, captchaKey)))
                }
            response.bodyAsText()
        } catch (e: Exception) {
            """{"code":-1,"msg":"${e.message}"}"""
        }

    suspend fun login(
        phone: String,
        smsCode: String,
        isAlipay: Boolean,
    ): LoginResult {
        val appType = if (isAlipay) APP_TYPE_POINTS else APP_TYPE_DEVICE
        val response =
            client.post("${ApiConfig.baseUrl}/acc/login") {
                contentType(ContentType.Application.Json)
                setBody(requestJson.encodeToString(LoginRequest(authCode = smsCode, un = phone, cid = ApiConfig.cid)))
                header("ApplicationType", appType)
            }
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        val code = body.int("code", -1)
        if (code != 0) return LoginResult(false, error = body.str("msg", "登录失败"))
        val al = body.obj("data")?.obj("al")
        val token = al?.str("token") ?: ""
        val uid = al?.str("uid") ?: ""
        return LoginResult(true, token = token, uid = uid)
    }

    // 账号信息
    suspend fun getAccountInfo(
        token: String,
        notifyExpiry: Boolean = true,
    ): AccountInfoDto? {
        val response =
            client.get("${ApiConfig.baseUrl}/ui/app/master") {
                header("Authorization", token)
                header("ApplicationType", APP_TYPE_DEVICE)
            }
        val body = bodyJson(response, notifyExpiry, reportError = false)
        val account = body.obj("data")?.obj("account") ?: return null
        return AccountInfoDto(
            id = account.str("id"),
            img = account.str("img"),
            name = account.str("name"),
            pn = account.str("pn"),
        )
    }

    // 探测一条登录信息的有效性，用于手动导入校验；不触发会话失效登出
    suspend fun probeToken(token: String): TokenProbe {
        val appInfo =
            try {
                getAccountInfo(token, notifyExpiry = false)
            } catch (_: Exception) {
                null
            }
        val mainValid =
            try {
                val response =
                    client.get("${ApiConfig.baseUrl}/acc/score/mission-lst") {
                        header("Authorization", token)
                        header("ApplicationType", APP_TYPE_POINTS)
                    }
                bodyJson(response, notifyExpiry = false, reportError = false).intOrNull("code") == 0
            } catch (_: Exception) {
                false
            }
        return TokenProbe(appValid = appInfo != null, mainValid = mainValid, uid = appInfo?.id ?: "")
    }

    // 账号设置
    suspend fun setUseScore(
        token: String,
        useScore: Int = 1,
        appType: String = APP_TYPE_DEVICE,
    ): DevResult {
        val response =
            client.post("${ApiConfig.baseUrl}/acc/upt") {
                contentType(ContentType.Application.Json)
                setBody(requestJson.encodeToString(SetUseScoreRequest(useScore)))
                header("Authorization", token)
                header("ApplicationType", appType)
            }
        return parseDevResult(response.bodyAsText(), reportError = false)
    }

    // 设备
    suspend fun getMasterDevices(token: String): MasterResult {
        val response =
            client.get("${ApiConfig.baseUrl}/ui/app/master") {
                header("Authorization", token)
                header("ApplicationType", APP_TYPE_DEVICE)
            }
        val body = bodyJson(response, reportError = false)
        val data = body.obj("data") ?: return MasterResult()
        val accountId = data.obj("account")?.str("id") ?: ""
        val favos = data.arr("favos") ?: return MasterResult(accountId)
        val devices =
            favos.map { item ->
                val obj = item.jsonObject
                val gene = obj.obj("gene")
                val geneStatus = gene?.intOrNull("status") ?: 0
                val dtype = obj.obj("bm")?.intOrNull("dtype") ?: 0
                DeviceDto(
                    id = obj.str("id"),
                    name = obj.str("name"),
                    geneStatus = geneStatus,
                    dtype = dtype,
                )
            }
        return MasterResult(accountId, devices)
    }

    suspend fun devStart(
        token: String,
        did: String,
        appType: String = APP_TYPE_DEVICE,
        ptype: Int = 91,
        args: String = "",
        reportError: Boolean = true,
    ): DevResult {
        val response =
            client.get("${ApiConfig.baseUrl}/dev/start") {
                parameter("did", did)
                parameter("upgrade", "true")
                parameter("ptype", ptype.toString())
                parameter("args", args)
                parameter("rcp", "false")
                parameter("cnt", "1")
                header("Authorization", token)
                header("ApplicationType", appType)
            }
        return parseDevResult(response.bodyAsText(), reportError)
    }

    suspend fun devEnd(
        token: String,
        did: String,
        appType: String = APP_TYPE_DEVICE,
    ): DevResult {
        val response =
            client.get("${ApiConfig.baseUrl}/dev/end") {
                parameter("did", did)
                header("Authorization", token)
                header("ApplicationType", appType)
            }
        return parseDevResult(response.bodyAsText())
    }

    suspend fun devFavo(
        token: String,
        did: String,
        remove: Boolean,
    ): DevResult {
        val response =
            client.get("${ApiConfig.baseUrl}/dev/favo") {
                parameter("did", did)
                parameter("remove", if (remove) "1" else "0")
                header("Authorization", token)
                header("ApplicationType", APP_TYPE_DEVICE)
            }
        return parseDevResult(response.bodyAsText())
    }

    // 扫码：用二维码中解析出的 id 换取设备信息
    suspend fun qrUse(
        token: String,
        id: String,
    ): QrUseResult {
        val response =
            client.get("${ApiConfig.baseUrl}/qr/use") {
                parameter("id", id)
                header("Authorization", token)
                header("ApplicationType", APP_TYPE_DEVICE)
            }
        val body = bodyJson(response)
        val code = body.int("code", -1)
        val data = body.obj("data")
        val type = data?.obj("qr")?.intOrNull("type")
        val deviceId = data?.obj("dev")?.str("id") ?: ""
        return QrUseResult(success = code == 0, type = type, deviceId = deviceId)
    }

    suspend fun getDevStatus(
        token: String,
        did: String,
        appType: String = APP_TYPE_DEVICE,
    ): DevStatusResult? {
        val response =
            client.get("${ApiConfig.baseUrl}/ui/app/dev/status") {
                parameter("did", did)
                parameter("more", "false")
                header("Authorization", token)
                header("ApplicationType", appType)
            }
        val body = bodyJson(response, reportError = false)
        if (body.intOrNull("code") != 0) return null
        val device = body.obj("data")?.obj("device") ?: return null
        val geneStatus = device.obj("gene")?.intOrNull("status") ?: 0
        return DevStatusResult(deviceStatus = device.int("status"), geneStatus = geneStatus)
    }

    // 启动前获取设备可选项（模式/通道/货道），more=true
    suspend fun getDeviceStartOptions(
        token: String,
        did: String,
        appType: String = APP_TYPE_DEVICE,
    ): DeviceStartOptions {
        val response =
            client.get("${ApiConfig.baseUrl}/ui/app/dev/status") {
                parameter("did", did)
                parameter("more", "true")
                header("Authorization", token)
                header("ApplicationType", appType)
            }
        val body = bodyJson(response, reportError = false)
        if (body.intOrNull("code") != 0) return DeviceStartOptions()
        val device = body.obj("data")?.obj("device") ?: return DeviceStartOptions()
        val parts =
            device.obj("bm")?.arr("parts")?.mapNotNull { item ->
                val obj = item as? JsonObject ?: return@mapNotNull null
                DeviceOption(
                    mode = obj.int("mode", -1),
                    name = obj.str("name"),
                    rate = obj.double("rate"),
                    maxT = obj.int("maxT"),
                )
            } ?: emptyList()
        val subCount = device.arr("subs")?.size ?: 0
        val goods =
            device.obj("gs")?.arr("items")?.mapNotNull { item ->
                val obj = item as? JsonObject ?: return@mapNotNull null
                DeviceGoods(pos = obj.int("pos"), out = obj.int("out"))
            } ?: emptyList()
        return DeviceStartOptions(parts = parts, subCount = subCount, goods = goods)
    }

    private fun parseDevResult(
        raw: String,
        reportError: Boolean = true,
    ): DevResult {
        val body = json.parseToJsonElement(raw).jsonObject
        checkSessionExpired(body)
        val code = body.int("code", -1)
        val msg = body.str("msg")
        if (reportError && code != 0 && code != -99) emitApiError(msg)
        return DevResult(code == 0, code, msg)
    }

    // 积分与任务
    suspend fun getMissionList(token: String): MissionListResult {
        val response =
            client.get("${ApiConfig.baseUrl}/acc/score/mission-lst") {
                header("Authorization", token)
                header("ApplicationType", APP_TYPE_POINTS)
            }
        val body = bodyJson(response)
        val data = body.obj("data") ?: return MissionListResult(emptyList())
        val accScore = data.obj("accScoreRsp")
        val validScore = accScore?.intOrNull("validScore")
        val totalScore = accScore?.intOrNull("totalScore")
        val weekMask = accScore?.obj("daily")?.intOrNull("week") ?: 0
        val dailyRsp = data.obj("dailyRSP")
        val dailyAdId = dailyRsp?.str("adId") ?: ""
        val dailyScore = dailyRsp?.intOrNull("score") ?: 5

        // 官方 n0.b 语义：limits 中值为 -1 表示今日已达上限，按任务上限计入；负数一律视为已完成
        val limitsArray = accScore?.arr("limits") ?: emptyList()
        val dailyDoneMap = mutableMapOf<String, Int>()
        for (item in limitsArray) {
            val obj = item.jsonObject
            val refId = obj.str("refId")
            val limit = obj.int("limit")
            if (refId.isNotEmpty()) dailyDoneMap[refId] = limit
        }

        val missions =
            data.arr("missions") ?: return MissionListResult(
                emptyList(),
                validScore,
                weekMask,
                dailyAdId,
                dailyScore,
                totalScore,
            )
        val seen = mutableSetOf<String>()
        val list =
            missions.mapNotNull { item ->
                val obj = item.jsonObject
                val refId = obj.str("refId")
                if (refId.isBlank() || !seen.add(refId)) return@mapNotNull null
                val score = obj.int("score")
                val limit = obj.int("limit")
                if (limit <= 0) return@mapNotNull null
                val rawDone = dailyDoneMap[refId]
                MissionDto(
                    adId = refId,
                    name = obj.strOrNull("name") ?: obj.strOrNull("title") ?: "任务",
                    score = score,
                    limit = limit,
                    dailyCompleted =
                        when {
                            rawDone == null -> 0
                            rawDone < 0 -> limit
                            else -> rawDone
                        },
                )
            }

        val allMissions =
            if (dailyAdId.isNotBlank()) {
                listOf(MissionDto(adId = dailyAdId, name = "每日签到", score = dailyScore, limit = 1, isDailySignin = true)) + list
            } else {
                list
            }

        return MissionListResult(allMissions, validScore, weekMask, dailyAdId, dailyScore, totalScore)
    }

    suspend fun executeMission(
        token: String,
        uid: String,
        adId: String,
    ): DevResult {
        val sign = Signer.sign(adId, token, uid)
        val response =
            client.post("${ApiConfig.baseUrl}/acc/score/score-send?sign=$sign&s=true") {
                contentType(ContentType.Application.Json)
                setBody(requestJson.encodeToString(MissionExecRequest(adId)))
                header("Authorization", token)
                header("ApplicationType", APP_TYPE_POINTS)
            }
        return parseDevResult(response.bodyAsText(), reportError = false)
    }

    suspend fun signIn(
        token: String,
        uid: String,
        weekDay: Int,
        adId: String,
    ): DevResult {
        val sign = Signer.sign(adId, token, uid)
        val response =
            client.post("${ApiConfig.baseUrl}/acc/score/score-send?sign=$sign&s=true") {
                contentType(ContentType.Application.Json)
                setBody(requestJson.encodeToString(SignInRequest(weekDay, adId)))
                header("Authorization", token)
                header("ApplicationType", APP_TYPE_POINTS)
            }
        return parseDevResult(response.bodyAsText(), reportError = false)
    }

    suspend fun getScoreList(
        token: String,
        page: Int = 0,
        size: Int = 20,
        src: Int? = null,
    ): ScoreListResult {
        val response =
            client.get("${ApiConfig.baseUrl}/acc/score/score-lst") {
                parameter("page", page.toString())
                parameter("size", size.toString())
                parameter("hasCount", "1")
                if (src != null) parameter("src", src.toString())
                header("Authorization", token)
                header("ApplicationType", APP_TYPE_POINTS)
            }
        val body = bodyJson(response)
        val data = body.arr("data") ?: return ScoreListResult(emptyList(), 0)
        val total = body.intOrNull("size") ?: data.size
        val records =
            data.map { item ->
                val obj = item.jsonObject
                val nested = obj.obj("data")
                val score = readScoreInt(obj, nested)
                val adId = nested?.str("adId") ?: ""
                val isExpense = obj.intOrNull("type") == 107 || nested?.containsKey("spend") == true
                ScoreDto(
                    score = if (isExpense) -score else score,
                    name =
                        nested?.strOrNull("adName")
                            ?: obj.strOrNull("msg")
                            ?: obj.strOrNull("typeName")
                            ?: "积分变动",
                    time = obj.str("ctime"),
                    adId = adId,
                )
            }
        return ScoreListResult(records, total)
    }

    // 钱包与账单
    suspend fun getWalletOwner(
        token: String,
        all: Boolean = true,
    ): WalletOwnerResult {
        val response =
            client.get("${ApiConfig.baseUrl}/acc/wallet/owner") {
                parameter("all", all.toString())
                header("Authorization", token)
                header("ApplicationType", APP_TYPE_DEVICE)
            }
        val body = bodyJson(response)
        if (body.intOrNull("code") != 0) return WalletOwnerResult()
        val data = body.obj("data") ?: return WalletOwnerResult()
        val active = data.obj("aw")?.let { parseWallet(it) }
        val eps = data.arr("eps")?.filterIsInstance<JsonObject>()?.map { parseWallet(it) } ?: emptyList()
        val refundProgress =
            data.obj("rfdProg")?.let {
                RefundProgress(
                    ctime = it.long("ctime", -1L),
                    count = it.int("count"),
                    total = it.double("total"),
                    fail = it.intOrNull("fail"),
                )
            }
        return WalletOwnerResult(active, eps, refundProgress)
    }

    suspend fun getWalletDetail(
        token: String,
        id: String,
        eid: String = "",
    ): WalletAccount? {
        if (id.isEmpty()) return null
        val response =
            client.get("${ApiConfig.baseUrl}/acc/wallet/detail") {
                parameter("id", id)
                if (eid.isNotEmpty()) parameter("eid", eid)
                header("Authorization", token)
                header("ApplicationType", APP_TYPE_DEVICE)
            }
        val body = bodyJson(response)
        if (body.intOrNull("code") != 0) return null
        val data = body.obj("data") ?: return null
        return parseWallet(data)
    }

    private fun parseWallet(obj: JsonObject): WalletAccount {
        val ep = obj.obj("ep")
        val setting = ep?.obj("setting")
        val owner = obj.obj("owner")
        return WalletAccount(
            id = obj.str("id"),
            eid = ep?.str("id") ?: "",
            ownerId = owner?.str("id") ?: "",
            name = ep?.strOrNull("name") ?: obj.strOrNull("name") ?: "",
            olCash = obj.double("olCash"),
            olGift = obj.double("olGift"),
            ofCash = obj.double("ofCash"),
            ofGift = obj.double("ofGift"),
            total = obj.double("total"),
            auth = obj.bool("auth"),
            chargeEnabled = (setting?.intOrNull("olcharge") ?: 1) == 1,
            refundEnabled = (setting?.intOrNull("olrefund") ?: 1) == 1,
        )
    }

    suspend fun getBillList(
        token: String,
        page: Int = 0,
        size: Int = 20,
        status: Int = 3,
    ): BillListResult {
        val response =
            client.get("${ApiConfig.baseUrl}/bill/lst-owner") {
                parameter("page", page.toString())
                parameter("size", size.toString())
                parameter("hasCount", (page == 0).toString())
                parameter("status", status.toString())
                header("Authorization", token)
                header("ApplicationType", APP_TYPE_DEVICE)
            }
        val body = bodyJson(response)
        val data = body.arr("data") ?: return BillListResult(emptyList(), 0)
        val total = body.intOrNull("size") ?: data.size
        val records =
            data.mapNotNull { item ->
                val obj = item as? JsonObject ?: return@mapNotNull null
                BillRecord(
                    id = obj.str("id"),
                    cata = obj.int("cata"),
                    type = obj.int("type"),
                    msg = obj.str("msg"),
                    status = obj.int("status"),
                    dir = obj.int("dir", 1),
                    payment = obj.double("payment"),
                    time = obj.longOrNull("utime") ?: obj.longOrNull("ctime") ?: 0L,
                )
            }
        return BillListResult(records, total)
    }

    suspend fun getRechargeProducts(
        token: String,
        eid: String,
    ): List<RechargeProduct> {
        if (eid.isEmpty()) return emptyList()
        val response =
            client.get("${ApiConfig.baseUrl}/prd/lst") {
                parameter("eid", eid)
                parameter("type", "1")
                parameter("status", "1")
                parameter("all", "false")
                parameter("did", "")
                parameter("page", "0")
                parameter("size", "100")
                parameter("hasCount", "false")
                header("Authorization", token)
                header("ApplicationType", APP_TYPE_DEVICE)
            }
        val body = bodyJson(response)
        val data = body.arr("data") ?: return emptyList()
        return data
            .mapNotNull { item ->
                val obj = item as? JsonObject ?: return@mapNotNull null
                val id = obj.str("id")
                if (id.isEmpty()) return@mapNotNull null
                RechargeProduct(
                    id = id,
                    name = obj.str("name", "充值"),
                    price = obj.double("curPrice"),
                    originalPrice = obj.double("ogiPrice"),
                )
            }.distinctBy { "${it.name}|${it.price}|${it.originalPrice}" }
    }

    suspend fun createRechargeOrder(
        token: String,
        eid: String,
        ownerId: String,
        productId: String,
    ): String? {
        if (eid.isEmpty() || ownerId.isEmpty() || productId.isEmpty()) return null
        val order =
            CreateRechargeOrderRequest(
                contact = EndpointRef(ownerId),
                ep = EndpointRef(eid),
                owner = EndpointRef(ownerId),
                prds = listOf(ProductRef(productId, count = 1)),
            )
        val response =
            client.post("${ApiConfig.baseUrl}/bill/save") {
                contentType(ContentType.Application.Json)
                setBody(requestJson.encodeToString(order))
                header("Authorization", token)
                header("ApplicationType", APP_TYPE_DEVICE)
            }
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        checkSessionExpired(body)
        if (body.intOrNull("code") != 0) {
            emitApiError(body.strOrNull("msg"))
            return null
        }
        return body.strOrNull("data")?.takeIf { it.isNotEmpty() && it != "null" }
    }

    suspend fun prepayAlipay(
        token: String,
        orderId: String,
    ): String? {
        if (orderId.isEmpty()) return null
        val response =
            client.get("${ApiConfig.baseUrl}/trans/prepay/21") {
                parameter("id", orderId)
                header("Authorization", token)
                header("ApplicationType", APP_TYPE_DEVICE)
            }
        val body = bodyJson(response)
        if (body.intOrNull("code") != 0) return null
        return body.strOrNull("data")?.takeIf { it.isNotEmpty() && it != "null" }
    }

    // 钱包退款：POST /acc/wallet/refund，type=23（支付宝小程序，与官方 RefundActivity 一致）
    suspend fun refundWallet(
        token: String,
        eid: String,
    ): DevResult {
        if (eid.isEmpty()) return DevResult(false, -1, "钱包信息缺失")
        val response =
            client.post("${ApiConfig.baseUrl}/acc/wallet/refund") {
                contentType(ContentType.Application.Json)
                setBody(requestJson.encodeToString(RefundWalletRequest(eid)))
                header("Authorization", token)
                header("ApplicationType", APP_TYPE_DEVICE)
            }
        return parseDevResult(response.bodyAsText())
    }

    // 积分兑换：将积分兑换为指定钱包余额，成功返回账单号用于核验结果
    suspend fun exchangeScore(
        token: String,
        endpointId: String,
        score: Int,
    ): String? {
        if (endpointId.isEmpty() || score <= 0) return null
        val response =
            client.post("${ApiConfig.baseUrl}/acc/score/score-use") {
                contentType(ContentType.Application.Json)
                setBody(requestJson.encodeToString(ExchangeScoreRequest(EndpointRef(endpointId), score)))
                header("Authorization", token)
                header("ApplicationType", APP_TYPE_DEVICE)
            }
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        checkSessionExpired(body)
        if (body.intOrNull("code") != 0) {
            emitApiError(body.strOrNull("msg"))
            return null
        }
        return body.obj("data")?.strOrNull("sn")?.takeIf { it.isNotEmpty() && it != "null" }
    }

    // 核验兑换账单是否已完成（status=3）
    suspend fun isExchangeBillCompleted(
        token: String,
        billId: String,
    ): Boolean {
        if (billId.isEmpty()) return false
        val response =
            client.get("${ApiConfig.baseUrl}/bill/view-full") {
                parameter("id", billId)
                header("Authorization", token)
                header("ApplicationType", APP_TYPE_DEVICE)
            }
        val body = bodyJson(response, reportError = false)
        if (body.intOrNull("code") != 0) return false
        val bill = body.obj("data")?.obj("bill") ?: return false
        if (bill.str("id") != billId) return false
        return bill.intOrNull("status") == 3
    }

    private fun readScoreInt(
        obj: JsonObject,
        nested: JsonObject?,
    ): Int {
        val keys = listOf("score", "spend", "changeScore", "change_score", "amount", "value", "num", "points")
        for (key in keys) {
            obj.intOrNull(key)?.let { return it }
            nested?.intOrNull(key)?.let { return it }
        }
        return 0
    }
}
