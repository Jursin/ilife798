package com.github.ilife798.data.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.ilife798.data.api.DevStatusResult
import com.github.ilife798.data.api.DevResult
import com.github.ilife798.data.api.IlifeApi
import com.github.ilife798.data.api.ScoreDto
import com.github.ilife798.data.model.Account
import com.github.ilife798.data.model.AccountInfo
import com.github.ilife798.data.model.AppState
import com.github.ilife798.data.model.BillRecord
import com.github.ilife798.data.model.Device
import com.github.ilife798.data.model.MissionInfo
import com.github.ilife798.data.model.PointsInfo
import com.github.ilife798.data.model.RechargeProduct
import com.github.ilife798.data.model.RefundProgress
import com.github.ilife798.data.model.ScoreRecord
import com.github.ilife798.data.model.SpendingStats
import com.github.ilife798.data.model.TaskRecord
import com.github.ilife798.data.model.ThemeMode
import com.github.ilife798.data.model.WalletAccount
import com.github.ilife798.toImageBitmap
import com.github.ilife798.AppStorage
import com.github.ilife798.StorageKeys
import com.github.ilife798.pay.AlipayPayResult
import com.github.ilife798.pay.payWithAlipay
import com.github.ilife798.showToast
import com.github.ilife798.util.currentTimeMillis
import com.github.ilife798.util.currentTimeFormatted
import com.github.ilife798.util.formatTimestamp
import com.github.ilife798.util.getDayOfWeek
import com.github.ilife798.util.getTodayStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val PAY_TYPE_WALLET = 91
private const val PAY_TYPE_ALIPAY = 21
private const val SCORE_PAGE_SIZE = 20
private const val BILL_PAGE_SIZE = 20
private const val BILL_STATUS = 3
private const val SPENDING_MAX_PAGES = 10

class AppViewModel : ViewModel() {

    private val api = IlifeApi()
    private val rateLimiter = TokenRateLimiter()
    private var taskJob: Job? = null
    private var lastMissionsLoadTime = 0L
    private var lastScoreLoadTime = 0L
    private var lastAccountLoadTime = 0L
    private var lastDeviceLoadTime = 0L
    private var lastBillLoadTime = 0L
    private var lastSpendingLoadTime = 0L

    var state by mutableStateOf(AppState())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var pollingDeviceId by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        api.onSessionExpired = { handleSessionExpired() }
        loadSavedAccount()
    }

    private fun handleSessionExpired() {
        if (state.account.token.isEmpty() && state.account.appToken.isEmpty()) return
        showToast("登录状态失效，请重新登录")
        logout()
    }

    private fun loadSavedAccount() {
        try {
            val storage = AppStorage.instance
            val isLoggedIn = storage.getBoolean(StorageKeys.IS_LOGGED_IN)
            if (isLoggedIn) {
                val account = Account(
                    phone = storage.getString(StorageKeys.PHONE) ?: "",
                    isLoggedIn = true,
                    pointsLoginDone = storage.getBoolean(StorageKeys.POINTS_LOGIN_DONE),
                    uid = storage.getString(StorageKeys.UID) ?: "",
                    eid = storage.getString(StorageKeys.EID) ?: "",
                    token = storage.getString(StorageKeys.TOKEN) ?: "",
                    appToken = storage.getString(StorageKeys.APP_TOKEN) ?: ""
                )
                state = state.copy(account = account)
                checkLoginStatus()
            }
        } catch (_: Exception) {}
    }

    private fun checkLoginStatus() {
        val token = state.account.appToken.ifEmpty { state.account.token }
        if (token.isEmpty()) return
        viewModelScope.launch {
            try {
                val result = api.getAccountInfo(token)
                if (result != null) {
                    loadDeviceInfo()
                    loadAccountInfo()
                    loadScoreInfo()
                } else {
                    handleSessionExpired()
                }
            } catch (_: Exception) {
                handleSessionExpired()
            }
        }
    }

    private fun saveAccount() {
        try {
            val storage = AppStorage.instance
            val account = state.account
            storage.saveBoolean(StorageKeys.IS_LOGGED_IN, account.isLoggedIn)
            storage.saveString(StorageKeys.PHONE, account.phone)
            storage.saveBoolean(StorageKeys.POINTS_LOGIN_DONE, account.pointsLoginDone)
            storage.saveString(StorageKeys.UID, account.uid)
            storage.saveString(StorageKeys.EID, account.eid)
            storage.saveString(StorageKeys.TOKEN, account.token)
            storage.saveString(StorageKeys.APP_TOKEN, account.appToken)
        } catch (_: Exception) {}
    }

    // 账号信息页手动填写凭据（如导入官方 token/uid）
    fun updateAccountField(field: String, value: String) {
        val acc = state.account
        val updated = when (field) {
            "appToken" -> acc.copy(appToken = value, isLoggedIn = true)
            "token" -> acc.copy(token = value, isLoggedIn = true, pointsLoginDone = true)
            else -> return
        }
        state = state.copy(account = updated)
        saveAccount()
        if (field == "appToken" || field == "token") {
            lastDeviceLoadTime = 0L
            lastScoreLoadTime = 0L
            lastAccountLoadTime = 0L
            loadDeviceInfo()
            loadAccountInfo()
            loadScoreInfo()
        }
    }

    // --- Captcha ---
    var captchaKey by mutableStateOf("")
        private set

    var captchaImage by mutableStateOf<ImageBitmap?>(null)
        private set

    var captchaLoaded by mutableStateOf(false)
        private set

    fun loadCaptcha() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            smsSent = false
            try {
                val key = api.newCaptchaKey()
                captchaKey = key
                val bytes = api.getCaptcha(key)
                captchaImage = bytes.toImageBitmap()
                captchaLoaded = true
            } catch (e: Exception) {
                errorMessage = "获取图形验证码失败: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // --- Login ---
    var smsSent by mutableStateOf(false)
        private set

    var loginType by mutableStateOf<String?>(null)
        private set

    fun sendSmsCode(phone: String, graphCode: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = api.sendSmsCode(phone, graphCode, captchaKey)
                val json = Json { ignoreUnknownKeys = true }
                val obj = json.parseToJsonElement(response).jsonObject
                val code = obj["code"]?.jsonPrimitive?.content?.toIntOrNull() ?: -1
                if (code == 0) {
                    smsSent = true
                } else {
                    errorMessage = obj["msg"]?.jsonPrimitive?.content ?: "发送验证码失败"
                }
            } catch (e: Exception) {
                errorMessage = "发送验证码失败: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun resetCaptcha() {
        captchaKey = ""
        captchaImage = null
        captchaLoaded = false
        smsSent = false
    }

    fun login(phone: String, smsCode: String, isAlipay: Boolean) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            loginType = if (isAlipay) "alipay" else "app"
            try {
                val result = api.login(phone, smsCode, isAlipay)
                if (result.success) {
                    val prev = state.account
                    state = state.copy(
                        account = prev.copy(
                            phone = phone,
                            isLoggedIn = true,
                            pointsLoginDone = if (isAlipay) true else prev.pointsLoginDone,
                            uid = result.uid.ifEmpty { prev.uid },
                            eid = result.eid.ifEmpty { prev.eid },
                            token = if (isAlipay) result.token else prev.token,
                            appToken = if (!isAlipay) result.token else prev.appToken
                        )
                    )
                    loadDeviceInfo()
                    loadAccountInfo()
                    if (isAlipay) loadScoreInfo()
                    saveAccount()
                } else {
                    errorMessage = result.error
                }
            } catch (e: Exception) {
                errorMessage = "登录失败: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun logout() {
        state = state.copy(
            account = Account(),
            devices = emptyList(),
            points = PointsInfo(),
            accountInfo = AccountInfo(),
            scoreRecords = emptyList(),
            wallets = emptyList(),
            activeWalletId = "",
            billRecords = emptyList(),
            taskRecords = emptyList(),
            taskLogs = emptyList(),
            taskCompleted = false
        )
        missions = emptyList()
        try {
            val storage = AppStorage.instance
            storage.saveBoolean(StorageKeys.IS_LOGGED_IN, false)
            storage.saveString(StorageKeys.PHONE, "")
            storage.saveBoolean(StorageKeys.POINTS_LOGIN_DONE, false)
            storage.saveString(StorageKeys.UID, "")
            storage.saveString(StorageKeys.EID, "")
            storage.saveString(StorageKeys.TOKEN, "")
            storage.saveString(StorageKeys.APP_TOKEN, "")
        } catch (_: Exception) {}
    }

    // --- Devices ---
    fun loadDeviceInfo() {
        val useApp = state.account.appToken.isNotEmpty()
        val token = if (useApp) state.account.appToken else state.account.token
        val appType = if (useApp) "1,1" else "1,5"
        if (token.isEmpty()) return
        val now = currentTimeMillis()
        if (now - lastDeviceLoadTime < 5000) return
        lastDeviceLoadTime = now
        viewModelScope.launch {
            try {
                val master = api.getMasterDevices(token)
                // 账号已切换/退出登录，丢弃过期结果
                if (state.account.token != token && state.account.appToken != token) return@launch
                // 登录态失效：主接口不再返回账户数据
                if (master.accountId.isEmpty() && master.devices.isEmpty()) {
                    handleSessionExpired()
                    return@launch
                }
                val devices = master.devices
                val updatedDevices = devices.map { dto ->
                    val realStatus = try { api.getDevStatus(token, dto.id, appType) } catch (_: Exception) { null }
                    val home = try { api.getDevHome(token, dto.id, appType) } catch (_: Exception) { null }
                    Device(
                        id = dto.id,
                        name = dto.name,
                        status = dto.status,
                        geneStatus = realStatus?.geneStatus ?: dto.geneStatus,
                        deviceStatus = realStatus?.deviceStatus ?: 1,
                        ownerId = dto.ownerId,
                        shareUserId = home?.shareUserId ?: ""
                    )
                }
                state = state.copy(
                    devices = updatedDevices,
                    account = state.account.copy(uid = master.accountId.ifEmpty { state.account.uid })
                )
            } catch (_: Exception) {}
        }
    }

    fun loadAccountInfo() {
        val token = state.account.appToken.ifEmpty { state.account.token }
        if (token.isEmpty()) return
        val now = currentTimeMillis()
        if (now - lastAccountLoadTime < 5000) return
        lastAccountLoadTime = now
        viewModelScope.launch {
            try {
                val info = api.getAccountInfo(token)
                if (state.account.appToken != token && state.account.token != token) return@launch
                if (info == null) {
                    handleSessionExpired()
                    return@launch
                }
                val newInfo = AccountInfo(img = info.img, name = info.name, pn = info.pn)
                if (state.accountInfo != newInfo) {
                    state = state.copy(accountInfo = newInfo)
                }
            } catch (_: Exception) {}
        }
    }

    fun addDevice(id: String) {
        val token = state.account.appToken.ifEmpty { state.account.token }
        if (token.isEmpty()) return
        viewModelScope.launch {
            try {
                api.devFavo(token, id, remove = false)
                loadDeviceInfo()
            } catch (_: Exception) {}
        }
    }

    fun removeDevice(deviceId: String) {
        val token = state.account.appToken.ifEmpty { state.account.token }
        if (token.isEmpty()) return
        viewModelScope.launch {
            try {
                api.devFavo(token, deviceId, remove = true)
                loadDeviceInfo()
            } catch (_: Exception) {}
        }
    }

    fun toggleDeviceRunning(deviceId: String) {
        val useApp = state.account.appToken.isNotEmpty()
        val token = if (useApp) state.account.appToken else state.account.token
        val appType = if (useApp) "1,1" else "1,5"
        if (token.isEmpty()) return
        pollingDeviceId = deviceId
        viewModelScope.launch {
            try {
                val currentStatus = api.getDevStatus(token, deviceId, appType)
                println("DEV: toggle $deviceId currentGene=${currentStatus?.geneStatus} currentDevice=${currentStatus?.deviceStatus}")
                val result = if (currentStatus?.geneStatus == 99) {
                    api.setUseScore(token, useScore = 1, appType = appType)
                    // 优先钱袋支付（91），余额不足等失败时自动切换支付宝免密支付（21）
                    val wallet = api.devStart(token, deviceId, appType, ptype = PAY_TYPE_WALLET)
                    if (wallet.success) {
                        wallet
                    } else {
                        println("DEV: wallet start failed code=${wallet.code} msg=${wallet.message}, fallback to alipay")
                        api.devStart(token, deviceId, appType, ptype = PAY_TYPE_ALIPAY)
                    }
                } else {
                    api.devEnd(token, deviceId, appType)
                }
                if (!result.success) {
                    showToast(result.message.ifEmpty { "操作失败" })
                    pollingDeviceId = null
                    return@launch
                }
                // 第一轮：5次 × 2.5秒（官方参数）
                var newStatus: DevStatusResult? = null
                for (i in 1..5) {
                    delay(2500.milliseconds)
                    newStatus = api.getDevStatus(token, deviceId, appType)
                    println("DEV: poll1 $i $deviceId gene=${newStatus?.geneStatus} device=${newStatus?.deviceStatus}")
                    if (newStatus?.geneStatus != currentStatus?.geneStatus) break
                }
                // 第二轮（如需）：25次 × 5秒
                if (newStatus?.geneStatus == currentStatus?.geneStatus) {
                    for (i in 1..25) {
                        delay(5000.milliseconds)
                        newStatus = api.getDevStatus(token, deviceId, appType)
                        println("DEV: poll2 $i $deviceId gene=${newStatus?.geneStatus} device=${newStatus?.deviceStatus}")
                        if (newStatus?.geneStatus != currentStatus?.geneStatus) break
                    }
                }
                state = state.copy(
                    devices = state.devices.map {
                        if (it.id == deviceId) it.copy(
                            geneStatus = newStatus?.geneStatus ?: it.geneStatus,
                            deviceStatus = newStatus?.deviceStatus ?: it.deviceStatus
                        ) else it
                    }
                )
            } catch (e: Exception) {
                println("DEV: toggle error: ${e.message}")
                showToast("操作失败：${e.message}")
            }
            pollingDeviceId = null
        }
    }

    // --- Score/Tasks ---
    var missions by mutableStateOf<List<MissionInfo>>(emptyList())
        private set

    var scoreLoadingMore by mutableStateOf(false)
        private set
    var scoreHasMore by mutableStateOf(false)
        private set
    private var scorePage = 0
    private var scoreLoadToken = ""

    private fun toScoreRecord(dto: ScoreDto) = ScoreRecord(score = dto.score, name = dto.name, time = dto.time)

    private fun loadScoreFirstPage(token: String) {
        viewModelScope.launch {
            try {
                val result = api.getScoreList(token, page = 0, size = SCORE_PAGE_SIZE)
                if (state.account.token != token) return@launch
                scoreLoadToken = token
                scorePage = 0
                scoreHasMore = result.total > result.records.size
                state = state.copy(scoreRecords = result.records.map(::toScoreRecord))
            } catch (_: Exception) {}
        }
    }

    fun loadScoreInfo() {
        val token = state.account.token
        if (token.isEmpty()) return
        val now = currentTimeMillis()
        if (now - lastScoreLoadTime < 5000) return
        lastScoreLoadTime = now
        loadScoreFirstPage(token)
        loadScoreSummary()
    }

    fun loadScoreInfoNoCooldown() {
        val token = state.account.token
        if (token.isEmpty()) return
        loadScoreFirstPage(token)
    }

    fun loadMoreScores() {
        val token = state.account.token
        if (token.isEmpty() || scoreLoadingMore || !scoreHasMore || scoreLoadToken != token) return
        scoreLoadingMore = true
        viewModelScope.launch {
            try {
                val nextPage = scorePage + 1
                val result = api.getScoreList(token, page = nextPage, size = SCORE_PAGE_SIZE)
                if (state.account.token != token) return@launch
                scorePage = nextPage
                val seen = state.scoreRecords.map { "${it.time}|${it.score}|${it.name}" }.toMutableSet()
                val appended = result.records.map(::toScoreRecord)
                    .filter { seen.add("${it.time}|${it.score}|${it.name}") }
                state = state.copy(scoreRecords = state.scoreRecords + appended)
                scoreHasMore = (scorePage + 1) * SCORE_PAGE_SIZE < result.total
            } catch (_: Exception) {
            } finally {
                scoreLoadingMore = false
            }
        }
    }

    // 积分概览
    fun loadScoreSummary() {
        val token = state.account.token
        if (token.isEmpty()) return
        viewModelScope.launch {
            try {
                val result = api.getMissionList(token)
                if (state.account.token != token) return@launch
                state = state.copy(
                    points = state.points.copy(
                        available = result.validScore ?: state.points.available,
                        total = result.totalScore ?: state.points.total
                    )
                )
            } catch (_: Exception) {}
        }
    }

    fun refreshScorePage() {
        loadScoreSummary()
        loadScoreInfoNoCooldown()
    }

    // --- Wallet & Bills ---
    var billLoadingMore by mutableStateOf(false)
        private set
    var billHasMore by mutableStateOf(false)
        private set
    private var billPage = 0
    private var billLoadToken = ""

    var refundProgress by mutableStateOf<RefundProgress?>(null)
        private set

    private fun walletToken(): String = state.account.appToken.ifEmpty { state.account.token }

    fun loadWallet() {
        val token = walletToken()
        if (token.isEmpty()) return
        viewModelScope.launch {
            try {
                val result = api.getWalletOwner(token, all = true)
                if (walletToken() != token) return@launch
                val active = result.active
                val merged = if (active != null && result.wallets.none { it.id == active.id }) {
                    listOf(active) + result.wallets
                } else {
                    result.wallets
                }
                val wallets = merged.distinctBy { it.id.ifEmpty { it.eid } }
                val activeId = active?.id?.takeIf { it.isNotEmpty() }
                    ?: wallets.firstOrNull()?.id ?: ""
                state = state.copy(wallets = wallets, activeWalletId = activeId)
                refundProgress = result.refundProgress
                wallets.firstOrNull { it.id == activeId }?.let { loadWalletDetail(it) }
            } catch (_: Exception) {}
        }
    }

    private fun loadWalletDetail(wallet: WalletAccount) {
        val token = walletToken()
        if (token.isEmpty()) return
        viewModelScope.launch {
            try {
                val detail = api.getWalletDetail(token, wallet.id, wallet.eid) ?: return@launch
                if (walletToken() != token) return@launch
                state = state.copy(
                    wallets = state.wallets.map {
                        if (it.id == wallet.id) {
                            it.copy(
                                name = detail.name.ifEmpty { it.name },
                                total = detail.total,
                                olCash = detail.olCash,
                                olGift = detail.olGift,
                                ofCash = detail.ofCash,
                                ofGift = detail.ofGift,
                                auth = detail.auth
                            )
                        } else it
                    }
                )
            } catch (_: Exception) {}
        }
    }

    fun selectWallet(id: String) {
        if (state.activeWalletId == id) return
        rechargeProducts = emptyList()
        state = state.copy(activeWalletId = id)
        state.wallets.firstOrNull { it.id == id }?.let { loadWalletDetail(it) }
    }

    private fun loadBillFirstPage(token: String) {
        viewModelScope.launch {
            try {
                val result = api.getBillList(token, page = 0, size = BILL_PAGE_SIZE, status = BILL_STATUS)
                if (walletToken() != token) return@launch
                billLoadToken = token
                billPage = 0
                billHasMore = result.total > result.records.size
                state = state.copy(billRecords = result.records)
            } catch (_: Exception) {}
        }
    }

    fun loadBills() {
        val token = walletToken()
        if (token.isEmpty()) return
        val now = currentTimeMillis()
        if (now - lastBillLoadTime < 5000) return
        lastBillLoadTime = now
        loadBillFirstPage(token)
    }

    fun loadBillsNoCooldown() {
        val token = walletToken()
        if (token.isEmpty()) return
        loadBillFirstPage(token)
    }

    fun loadMoreBills() {
        val token = walletToken()
        if (token.isEmpty() || billLoadingMore || !billHasMore || billLoadToken != token) return
        billLoadingMore = true
        viewModelScope.launch {
            try {
                val nextPage = billPage + 1
                val result = api.getBillList(token, page = nextPage, size = BILL_PAGE_SIZE, status = BILL_STATUS)
                if (walletToken() != token) return@launch
                billPage = nextPage
                val seen = state.billRecords.map { it.id }.toMutableSet()
                val appended = result.records.filter { it.id.isEmpty() || seen.add(it.id) }
                state = state.copy(billRecords = state.billRecords + appended)
                billHasMore = (billPage + 1) * BILL_PAGE_SIZE < result.total
            } catch (_: Exception) {
            } finally {
                billLoadingMore = false
            }
        }
    }

    fun refreshBillPage() {
        loadWallet()
        loadBillsNoCooldown()
    }

    // --- Spending stats (home) ---
    var spendingStats by mutableStateOf(SpendingStats())
        private set

    fun loadSpendingStats() {
        val token = walletToken()
        if (token.isEmpty()) return
        val now = currentTimeMillis()
        if (now - lastSpendingLoadTime < 5000) return
        lastSpendingLoadTime = now
        viewModelScope.launch {
            try {
                val monthPrefix = currentTimeFormatted("yyyy-MM")
                val todayKey = currentTimeFormatted("yyyy-MM-dd")
                val yesterdayKey = formatTimestamp(getTodayStart(now) - 86_400_000L, "yyyy-MM-dd")
                var today = 0.0
                var yesterday = 0.0
                val monthDaySums = mutableMapOf<String, Double>()
                var page = 0
                while (page < SPENDING_MAX_PAGES) {
                    val result = api.getBillList(token, page = page, size = BILL_PAGE_SIZE, status = BILL_STATUS)
                    if (walletToken() != token) return@launch
                    if (result.records.isEmpty()) break
                    for (record in result.records) {
                        if (record.dir != 1 || record.payment <= 0.0 || record.cata == 1) continue
                        val key = formatTimestamp(record.time, "yyyy-MM-dd")
                        when (key) {
                            todayKey -> today += record.payment
                            yesterdayKey -> yesterday += record.payment
                        }
                        if (key.startsWith(monthPrefix)) {
                            monthDaySums[key] = (monthDaySums[key] ?: 0.0) + record.payment
                        }
                    }
                    val oldest = result.records.minOfOrNull { it.time } ?: break
                    if (formatTimestamp(oldest, "yyyy-MM") < monthPrefix) break
                    if (result.records.size < BILL_PAGE_SIZE) break
                    page++
                }
                val spendingDays = monthDaySums.count { it.value > 0.0 }
                val monthTotal = monthDaySums.values.sum()
                spendingStats = SpendingStats(
                    today = today,
                    yesterday = yesterday,
                    monthAverage = if (spendingDays > 0) monthTotal / spendingDays else 0.0
                )
            } catch (_: Exception) {}
        }
    }

    // --- Recharge ---
    var rechargeProducts by mutableStateOf<List<RechargeProduct>>(emptyList())
        private set
    var rechargeLoading by mutableStateOf(false)
        private set
    var rechargePaying by mutableStateOf(false)
        private set

    fun loadRechargeProducts() {
        val token = walletToken()
        val wallet = state.wallets.firstOrNull { it.id == state.activeWalletId }
        if (token.isEmpty() || wallet == null || wallet.eid.isEmpty()) return
        rechargeLoading = true
        viewModelScope.launch {
            try {
                val products = api.getRechargeProducts(token, wallet.eid)
                if (walletToken() != token || state.activeWalletId != wallet.id) return@launch
                rechargeProducts = products
            } catch (_: Exception) {
            } finally {
                rechargeLoading = false
            }
        }
    }

    suspend fun submitRecharge(product: RechargeProduct): AlipayPayResult {
        val token = walletToken()
        val wallet = state.wallets.firstOrNull { it.id == state.activeWalletId }
        if (token.isEmpty() || wallet == null || wallet.eid.isEmpty() || wallet.ownerId.isEmpty()) {
            return AlipayPayResult(false, "", "钱包信息缺失，请重新进入")
        }
        if (rechargePaying) return AlipayPayResult(false, "", "正在支付中，请稍候")
        rechargePaying = true
        return try {
            val orderId = api.createRechargeOrder(token, wallet.eid, wallet.ownerId, product.id)
                ?: return AlipayPayResult(false, "", "创建充值订单失败")
            val orderInfo = api.prepayAlipay(token, orderId)
                ?: return AlipayPayResult(false, "", "获取支付参数失败")
            val result = payWithAlipay(orderInfo)
            if (result.success) {
                loadWallet()
                loadBillsNoCooldown()
            }
            result
        } catch (e: Exception) {
            AlipayPayResult(false, "", "支付失败：${e.message}")
        } finally {
            rechargePaying = false
        }
    }

    // --- Refund ---
    var refundSubmitting by mutableStateOf(false)
        private set

    suspend fun submitRefund(): DevResult {
        val token = walletToken()
        val wallet = state.wallets.firstOrNull { it.id == state.activeWalletId }
            ?: return DevResult(false, -1, "钱包信息缺失，请重新进入")
        if (wallet.eid.isEmpty()) return DevResult(false, -1, "钱包信息缺失，请重新进入")
        if (wallet.refundable <= 0.0) return DevResult(false, -1, "可退金额为0，无法退款")
        if (refundSubmitting) return DevResult(false, -1, "正在提交退款，请稍候")
        refundSubmitting = true
        return try {
            val result = api.refundWallet(token, wallet.eid)
            if (result.success) {
                loadWallet()
                loadBillsNoCooldown()
            }
            result
        } catch (e: Exception) {
            DevResult(false, -1, "退款失败：${e.message}")
        } finally {
            refundSubmitting = false
        }
    }

    fun loadMissions() {
        val token = state.account.token
        if (token.isEmpty()) return
        val now = currentTimeMillis()
        if (now - lastMissionsLoadTime < 5000) return
        lastMissionsLoadTime = now
        viewModelScope.launch {
            try {
                val result = api.getMissionList(token)

                // Count today's completions from score-lst
                val todayStart = com.github.ilife798.util.getTodayStart(currentTimeMillis())
                val scores = try { api.getScoreList(token).records } catch (_: Exception) { emptyList() }
                val todayDoneFromScore = mutableMapOf<String, Int>()
                for (score in scores) {
                    if (score.score > 0 && score.adId.isNotEmpty()) {
                        val timeMs = score.time.toLongOrNull() ?: continue
                        if (timeMs >= todayStart) {
                            todayDoneFromScore[score.adId] = (todayDoneFromScore[score.adId] ?: 0) + 1
                        }
                    }
                }

                // Merge: use limits[] first, supplement with score-lst counts
                val mergedDone = mutableMapOf<String, Int>()
                for (m in result.missions) {
                    val fromLimits = m.dailyCompleted
                    val fromScore = todayDoneFromScore[m.adId] ?: 0
                    mergedDone[m.adId] = maxOf(fromLimits, fromScore)
                }

                // 账号已切换/退出登录，丢弃过期结果
                if (state.account.token != token) return@launch

                missions = result.missions.map {
                    MissionInfo(
                        adId = it.adId,
                        name = it.name,
                        score = it.score,
                        limit = it.limit,
                        dailyCompleted = mergedDone[it.adId] ?: 0,
                        isDailySignin = it.isDailySignin
                    )
                }
                state = state.copy(
                    weekMask = result.weekMask,
                    points = state.points.copy(
                        available = result.validScore ?: state.points.available,
                        total = result.totalScore ?: state.points.total
                    )
                )
            } catch (_: Exception) {}
        }
    }

    fun runAllTasks() {
        if (state.taskCompleted || isLoading) return
        val token = state.account.token
        val uid = state.account.uid
        if (token.isEmpty() || uid.isEmpty()) {
            errorMessage = "需要积分登录才能执行任务"
            return
        }
        lastMissionsLoadTime = 0L
        lastScoreLoadTime = 0L
        taskJob = viewModelScope.launch {
            isLoading = true
            errorMessage = null
            state = state.copy(taskLogs = emptyList())
            rateLimiter.clear()
            try {
                addTaskLog("正在加载任务列表...")
                val result = api.getMissionList(token)
                if (result.validScore != null || result.totalScore != null) {
                    state = state.copy(
                        points = state.points.copy(
                            available = result.validScore ?: state.points.available,
                            total = result.totalScore ?: state.points.total
                        )
                    )
                }

                // Count today's completions from score-lst
                val todayStart = com.github.ilife798.util.getTodayStart(currentTimeMillis())
                val scores = try { api.getScoreList(token).records } catch (_: Exception) { emptyList() }
                val todayDoneFromScore = mutableMapOf<String, Int>()
                for (score in scores) {
                    if (score.score > 0 && score.adId.isNotEmpty()) {
                        val timeMs = score.time.toLongOrNull() ?: continue
                        if (timeMs >= todayStart) {
                            todayDoneFromScore[score.adId] = (todayDoneFromScore[score.adId] ?: 0) + 1
                        }
                    }
                }

                val missionList = result.missions.map {
                    val fromLimits = it.dailyCompleted
                    val fromScore = todayDoneFromScore[it.adId] ?: 0
                    MissionInfo(
                        adId = it.adId,
                        name = it.name,
                        score = it.score,
                        limit = it.limit,
                        dailyCompleted = maxOf(fromLimits, fromScore),
                        isDailySignin = it.isDailySignin
                    )
                }
                missions = missionList
                var gained = 0

                // Daily sign-in
                if (result.dailyAdId.isNotBlank()) {
                    val weekDay = getDayOfWeek()
                    val alreadySigned = (result.weekMask and (1 shl (weekDay - 1))) != 0
                    if (alreadySigned) {
                        addTaskLog("每日签到: 今日已签到")
                    } else {
                        val delay = rateLimiter.reserveDelay(token, currentTimeMillis())
                        if (delay > 0) {
                            addTaskLog("等待 ${delay / 1000} 秒...")
                            delay(delay.milliseconds)
                        }
                        addTaskLog("每日签到: 执行中...")
                        val signResult = api.signIn(token, uid, weekDay, result.dailyAdId)
                        if (signResult.success) {
                            gained += result.dailyScore
                            addTaskLog("每日签到: 成功 +${result.dailyScore}分")
                        } else if (signResult.code == -98) {
                            addTaskLog("每日签到: 请求频繁，等待60秒重试...")
                            delay(60.seconds)
                            val retryResult = api.signIn(token, uid, weekDay, result.dailyAdId)
                            if (retryResult.success) {
                                gained += result.dailyScore
                                addTaskLog("每日签到: 成功 +${result.dailyScore}分")
                            } else {
                                addTaskLog("每日签到: 失败 ${retryResult.message}")
                            }
                        } else {
                            addTaskLog("每日签到: 失败 ${signResult.message}")
                        }
                    }
                }

                // Missions - use dailyCompleted from limits[] instead of score-lst
                val validMissions = missionList.filter { !it.isDailySignin && it.limit > 0 && it.score > 0 }
                addTaskLog("共 ${validMissions.size} 个可执行任务")
                var executed = 0
                for (mission in validMissions) {
                    if (!isActive) break
                    val completed = mission.dailyCompleted
                    val maxCount = mission.limit
                    if (completed >= maxCount) {
                        addTaskLog("[${mission.name}] 已完成 $completed/$maxCount，跳过")
                        continue
                    }
                    for (round in (completed + 1)..maxCount) {
                        if (!isActive) break
                        val delay = rateLimiter.reserveDelay(token, currentTimeMillis())
                        if (delay > 0) {
                            delay(delay.milliseconds)
                        }
                        addTaskLog("[${mission.name}] ($round/$maxCount) 执行中...")
                        var execResult = api.executeMission(token, uid, mission.adId)
                        if (execResult.code == -98) {
                            addTaskLog("[${mission.name}] 请求频繁，等待60秒重试...")
                            delay(60.seconds)
                            execResult = api.executeMission(token, uid, mission.adId)
                        }
                        if (execResult.success) {
                            executed++
                            gained += mission.score
                            addTaskLog("[${mission.name}] ($round/$maxCount) 成功 +${mission.score}分")
                            // Update dailyCompleted immediately so UI reflects it
                            missions = missions.map {
                                if (it.adId == mission.adId) it.copy(dailyCompleted = it.dailyCompleted + 1) else it
                            }
                        } else if (execResult.code == -98) {
                            addTaskLog("[${mission.name}] 请求频繁，跳过剩余任务")
                            break
                        } else {
                            addTaskLog("[${mission.name}] ($round/$maxCount) 失败: ${execResult.message}")
                        }
                        delay(30.seconds)
                    }
                }
                if (isActive) {
                    addTaskLog("任务完成，共获得 $gained 积分")
                    val record = TaskRecord(
                        timestamp = currentTimeMillis(),
                        message = "完成任务，获得 $gained 积分"
                    )
                    state = state.copy(
                        taskCompleted = true,
                        taskRecords = state.taskRecords + record,
                        points = state.points.copy(available = (state.points.available ?: 0) + gained)
                    )
                    loadScoreInfo()
                }
            } catch (_: kotlinx.coroutines.CancellationException) {
                addTaskLog("任务已停止")
            } catch (e: Exception) {
                addTaskLog("任务执行异常: ${e.message}")
                errorMessage = "任务执行失败: ${e.message}"
            } finally {
                isLoading = false
                taskJob = null
            }
        }
    }

    fun stopTasks() {
        addTaskLog("正在停止...")
        taskJob?.cancel()
        taskJob = null
        isLoading = false
    }

    fun setTaskCompleted() {
        state = state.copy(taskCompleted = true)
    }

    private fun addTaskLog(message: String) {
        val time = currentTimeFormatted("HH:mm:ss")
        state = state.copy(taskLogs = state.taskLogs + "[$time] $message")
    }

    // --- Settings ---
    fun setDynamicColor(enabled: Boolean) {
        state = state.copy(dynamicColor = enabled)
    }

    fun setFloatingNav(enabled: Boolean) {
        state = state.copy(floatingNav = enabled)
    }

    fun setAppBlur(enabled: Boolean) {
        state = state.copy(appBlur = enabled)
    }

    fun setPredictiveBackEnabled(enabled: Boolean) {
        state = state.copy(predictiveBackEnabled = enabled)
    }

    fun setThemeMode(mode: ThemeMode) {
        state = state.copy(themeMode = mode)
    }

    fun clearError() {
        errorMessage = null
    }

    fun forceRefresh() {
        lastDeviceLoadTime = 0L
        lastScoreLoadTime = 0L
        lastMissionsLoadTime = 0L
        lastAccountLoadTime = 0L
        loadDeviceInfo()
        loadAccountInfo()
        loadScoreInfo()
    }
}

private class TokenRateLimiter(private val minIntervalMs: Long = 30_000L) {
    private val nextAllowed = mutableMapOf<String, Long>()

    fun reserveDelay(token: String, now: Long): Long {
        val allowedAt = nextAllowed[token] ?: now
        val scheduledAt = maxOf(now, allowedAt)
        nextAllowed[token] = scheduledAt + minIntervalMs
        return scheduledAt - now
    }

    fun clear() {
        nextAllowed.clear()
    }
}
