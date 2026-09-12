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
import com.github.ilife798.data.model.DEFAULT_SEED_COLOR
import com.github.ilife798.data.model.Device
import com.github.ilife798.data.model.DevicePendingStart
import com.github.ilife798.data.model.DeviceStartOptions
import com.github.ilife798.data.model.HomeDeviceType
import com.github.ilife798.data.model.MissionInfo
import com.github.ilife798.data.model.PointsInfo
import com.github.ilife798.data.model.PaletteStyle
import com.github.ilife798.data.model.RechargeProduct
import com.github.ilife798.data.model.RefundProgress
import com.github.ilife798.data.model.ScoreFilter
import com.github.ilife798.data.model.ScoreRecord
import com.github.ilife798.data.model.SpendingStats
import com.github.ilife798.data.model.TaskRecord
import com.github.ilife798.data.model.ThemeMode
import com.github.ilife798.data.model.WalletAccount
import com.github.ilife798.AppLifecycle
import com.github.ilife798.DeviceTile
import com.github.ilife798.RunNotifications
import com.github.ilife798.getAppVersion
import com.github.ilife798.toImageBitmap
import com.github.ilife798.AppStorage
import com.github.ilife798.StorageKeys
import com.github.ilife798.pay.AlipayPayResult
import com.github.ilife798.pay.payWithAlipay
import com.github.ilife798.logDebug
import com.github.ilife798.showToast
import com.github.ilife798.dismissToast
import com.github.ilife798.util.QrCodeParser
import com.github.ilife798.util.currentTimeMillis
import com.github.ilife798.util.currentTimeFormatted
import com.github.ilife798.util.formatTimestamp
import com.github.ilife798.util.getDayOfWeek
import com.github.ilife798.util.getTodayStart
import com.github.ilife798.update.AppUpdate
import com.github.ilife798.update.UpdateDialogState
import com.github.ilife798.update.canInstallPackages
import com.github.ilife798.update.cancelUpdateProgressNotification
import com.github.ilife798.update.currentAbis
import com.github.ilife798.update.deleteDownloadedApk
import com.github.ilife798.update.downloadApkToFile
import com.github.ilife798.update.downloadedApkPathIfValid
import com.github.ilife798.update.installApk
import com.github.ilife798.update.openInstallPermissionSettings
import com.github.ilife798.update.requestNotificationPermission
import com.github.ilife798.update.showUpdateProgressNotification
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

// 无模式/多通道的设备类型（水表/淋浴/直饮水），启动时无需拉取可选项
private val SIMPLE_DEVICE_TYPES = setOf(5, 6, 8)

// 二维码类型
private const val QR_TYPE_DEVICE = 3
private const val QR_TYPE_DOOR_LOCK = 8

class AppViewModel : ViewModel() {

    // HttpClient 构建较重，延迟到首次网络调用时再创建，避免阻塞首帧渲染
    private val api by lazy {
        IlifeApi().apply {
            onSessionExpired = { handleSessionExpired() }
            onApiError = { showToast(it) }
        }
    }
    private val rateLimiter = TokenRateLimiter()

    private fun logError(scope: String, e: Exception) {
        logDebug("ILife798", "$scope: ${e.message}")
    }

    private fun isCurrentToken(token: String): Boolean =
        token.isNotEmpty() && (state.account.token == token || state.account.appToken == token)
    private var taskJob: Job? = null
    private var lastMissionsLoadTime = 0L
    private var lastScoreLoadTime = 0L
    private var lastAccountLoadTime = 0L
    private var lastDeviceLoadTime = 0L
    private var lastSpendingLoadTime = 0L

    var state by mutableStateOf(AppState())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var pollingDeviceId by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    // --- 检查更新 ---
    var updateDialog by mutableStateOf<UpdateDialogState>(UpdateDialogState.None)
        private set

    var githubProxyUrl by mutableStateOf("")
        private set

    var developerMode by mutableStateOf(false)
        private set

    private var pendingUpdateUrl = ""
    private var pendingUpdateSha256: String? = null
    private var pendingUpdateSize = 0L
    private var downloadedApkPath: String? = null
    private var checkingUpdate = false
    private var downloadingUpdate = false
    private var progressDialogDismissed = false
    private var lastUpdateProgress = 0f
    private var updateJob: Job? = null

    init {
        loadSettings()
        loadHomeDeviceTypePref()
        loadSavedAccount()
        AppLifecycle.onResumed = { onAppResumed() }
    }

    private fun loadSettings() {
        try {
            val storage = AppStorage.instance
            state = state.copy(
                dynamicColor = storage.getBoolean(StorageKeys.DYNAMIC_COLOR, true),
                customColor = storage.getBoolean(StorageKeys.CUSTOM_COLOR, false),
                paletteStyle = PaletteStyle.entries.firstOrNull { it.name == storage.getString(StorageKeys.PALETTE_STYLE) }
                    ?: PaletteStyle.TonalSpot,
                seedColor = storage.getInt(StorageKeys.SEED_COLOR, DEFAULT_SEED_COLOR),
                floatingNav = storage.getBoolean(StorageKeys.FLOATING_NAV, false),
                appBlur = storage.getBoolean(StorageKeys.APP_BLUR, true),
                predictiveBackEnabled = storage.getBoolean(StorageKeys.PREDICTIVE_BACK, true),
                themeMode = ThemeMode.entries.firstOrNull { it.name == storage.getString(StorageKeys.THEME_MODE) }
                    ?: ThemeMode.System
            )
            githubProxyUrl = storage.getString(StorageKeys.GITHUB_PROXY) ?: ""
            developerMode = storage.getBoolean(StorageKeys.DEVELOPER_MODE, false)
        } catch (e: Exception) {
            logError("loadSettings", e)
        }
    }

    private fun handleSessionExpired() {
        if (state.account.token.isEmpty() && state.account.appToken.isEmpty()) return
        viewModelScope.launch {
            if (state.account.token.isEmpty() && state.account.appToken.isEmpty()) return@launch
            showToast("登录状态失效，请重新登录")
            clearLoginState()
        }
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
        } catch (e: Exception) {
            logError("loadSavedAccount", e)
        }
    }

    private fun checkLoginStatus() {
        val token = state.account.appToken.ifEmpty { state.account.token }
        if (token.isEmpty()) return
        viewModelScope.launch {
            try {
                // 首次在此构建 HttpClient，放到后台线程，避免阻塞首帧
                val result = withContext(Dispatchers.Default) { api.getAccountInfo(token) }
                if (result != null) {
                    loadDeviceInfo()
                    loadAccountInfo()
                    loadScoreInfo()
                } else {
                    handleSessionExpired()
                }
            } catch (e: Exception) {
                logError("checkLoginStatus", e)
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
        } catch (e: Exception) {
            logError("saveAccount", e)
        }
    }

    // 账号信息页手动填写凭据（：校验有效性、渠道与同一账户
    fun updateAccountField(field: String, value: String) {
        val input = value.trim()
        if (input.isEmpty()) return
        viewModelScope.launch {
            val probe = try {
                api.probeToken(input)
            } catch (e: Exception) {
                logError("probeToken", e)
                null
            }
            if (probe == null || (!probe.appValid && !probe.mainValid)) {
                showToast("登录信息无效，未保存")
                return@launch
            }
            // 自动纠正到正确的槽位：仅当目标渠道不可用、另一渠道可用时兜底纠正
            var target = field
            if (field == "appToken" && !probe.appValid && probe.mainValid) {
                target = "token"
                showToast("检测为积分登录信息，已填入积分任务")
            } else if (field == "token" && !probe.mainValid && probe.appValid) {
                target = "appToken"
                showToast("检测为设备登录信息，已填入设备控制")
            }
            // 同一账户校验：与另一槽已有凭据的 uid 比对
            val otherToken = if (target == "appToken") state.account.token else state.account.appToken
            if (otherToken.isNotEmpty()) {
                val otherUid = try {
                    api.getAccountInfo(otherToken, notifyExpiry = false)?.id ?: ""
                } catch (e: Exception) {
                    logError("verifyOtherToken", e)
                    ""
                }
                if (probe.uid.isNotEmpty() && otherUid.isNotEmpty() && probe.uid != otherUid) {
                    showToast("两条登录信息不属于同一账户，未保存")
                    return@launch
                }
            }
            val acc = state.account
            val updated = when (target) {
                "appToken" -> acc.copy(appToken = input, isLoggedIn = true, uid = probe.uid.ifEmpty { acc.uid })
                "token" -> acc.copy(token = input, isLoggedIn = true, pointsLoginDone = true, uid = probe.uid.ifEmpty { acc.uid })
                else -> return@launch
            }
            // 凭据变更等于切换身份，先清掉上一个身份残留的数据与冷却计时，再加载
            clearAccountData()
            state = state.copy(account = updated)
            saveAccount()
            loadDeviceInfo()
            loadAccountInfo()
            loadScoreInfo()
            loadSpendingStats()
            loadMissions()
            showToast("登录信息已保存")
        }
    }

    // 账号信息页清空指定渠道的凭据（长按编辑时使用）
    fun clearAccountField(field: String) {
        val acc = state.account
        val updated = when (field) {
            "appToken" -> acc.copy(appToken = "")
            "token" -> acc.copy(token = "", pointsLoginDone = false)
            else -> return
        }
        val stillLoggedIn = updated.appToken.isNotEmpty() || updated.token.isNotEmpty()
        clearAccountData()
        state = state.copy(account = updated.copy(isLoggedIn = stillLoggedIn))
        saveAccount()
        loadDeviceInfo(force = true)
        loadAccountInfo()
        loadScoreInfo()
        loadSpendingStats()
        loadMissions()
        showToast(if (field == "appToken") "已清空设备控制" else "已清空积分任务")
    }

    // 验证码
    var captchaKey by mutableStateOf("")
        private set

    var captchaImage by mutableStateOf<ImageBitmap?>(null)
        private set

    var captchaLoaded by mutableStateOf(false)
        private set

    fun loadCaptcha(clearError: Boolean = true) {
        viewModelScope.launch {
            isLoading = true
            if (clearError) errorMessage = null
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

    // 登录
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
                    showToast("验证码已发送")
                } else {
                    val msg = obj["msg"]?.jsonPrimitive?.content ?: "发送验证码失败"
                    errorMessage = msg
                    showToast(msg)
                    loadCaptcha(clearError = false)
                }
            } catch (e: Exception) {
                val msg = "发送验证码失败: ${e.message}"
                errorMessage = msg
                showToast(msg)
                loadCaptcha(clearError = false)
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
                    clearAccountData()
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
                    loadSpendingStats()
                    loadMissions()
                    resetCaptcha()
                    loginType = null
                    saveAccount()
                    showToast("登录成功")
                } else {
                    errorMessage = result.error
                    showToast(result.error.ifEmpty { "登录失败" })
                }
            } catch (e: Exception) {
                val msg = "登录失败: ${e.message}"
                errorMessage = msg
                showToast(msg)
            } finally {
                isLoading = false
            }
        }
    }

    private fun clearAccountData() {
        taskJob?.cancel()
        taskJob = null
        pollingDeviceId = null
        missions = emptyList()
        spendingStats = SpendingStats()
        rechargeProducts = emptyList()
        refundProgress = null
        scoreLists = emptyScoreLists()
        scoreFilter = ScoreFilter.All
        billHasMore = false
        billLoadingMore = false
        billPage = 0
        billLoadToken = ""
        billStatus = BILL_STATUS
        billCache = mutableMapOf()
        lastMissionsLoadTime = 0L
        lastScoreLoadTime = 0L
        lastAccountLoadTime = 0L
        lastDeviceLoadTime = 0L
        lastSpendingLoadTime = 0L
        state = state.copy(
            devices = emptyList(),
            points = PointsInfo(),
            accountInfo = AccountInfo(),
            wallets = emptyList(),
            activeWalletId = "",
            billRecords = emptyList(),
            taskRecords = emptyList(),
            taskLogs = emptyList(),
            taskCompleted = false,
            weekMask = 0
        )
    }

    fun logout() {
        clearLoginState()
        showToast("已退出登录")
    }

    private fun clearLoginState() {
        clearAccountData()
        resetCaptcha()
        loginType = null
        isLoading = false
        errorMessage = null
        state = state.copy(account = Account())
        try {
            val storage = AppStorage.instance
            storage.saveBoolean(StorageKeys.IS_LOGGED_IN, false)
            storage.saveString(StorageKeys.PHONE, "")
            storage.saveBoolean(StorageKeys.POINTS_LOGIN_DONE, false)
            storage.saveString(StorageKeys.UID, "")
            storage.saveString(StorageKeys.EID, "")
            storage.saveString(StorageKeys.TOKEN, "")
            storage.saveString(StorageKeys.APP_TOKEN, "")
        } catch (e: Exception) {
            logError("logout", e)
        }
    }

    // 选择的设备类型，默认取已收藏设备的类型（按 洗烘/吹风/饮水/淋浴 顺序取最上），
    // 用户手动选择后持久化，下次进入或重启沿用
    var homeDeviceType by mutableStateOf(HomeDeviceType.Shower)
        private set
    private var homeDeviceTypeExplicit = false

    private fun loadHomeDeviceTypePref() {
        try {
            val saved = AppStorage.instance.getString(StorageKeys.HOME_DEVICE_TYPE)
            val type = HomeDeviceType.entries.firstOrNull { it.name == saved } ?: return
            homeDeviceType = type
            homeDeviceTypeExplicit = true
        } catch (e: Exception) {
            logError("loadHomeDeviceTypePref", e)
        }
    }

    fun selectHomeDeviceType(type: HomeDeviceType) {
        if (homeDeviceType == type && homeDeviceTypeExplicit) return
        homeDeviceType = type
        homeDeviceTypeExplicit = true
        try {
            AppStorage.instance.saveString(StorageKeys.HOME_DEVICE_TYPE, type.name)
        } catch (e: Exception) {
            logError("selectHomeDeviceType", e)
        }
    }

    private fun inferHomeDeviceType(devices: List<Device>) {
        if (homeDeviceTypeExplicit) return
        val dtypes = devices.map { it.dtype }.filter { it != 0 }.toSet()
        if (dtypes.isEmpty()) return
        HomeDeviceType.entries.firstOrNull { it.deviceType in dtypes }
            ?.let { homeDeviceType = it }
    }

    // 设备上是否已存在快捷设置图块，用于隐藏首页引导提示
    var homeTileCreated by mutableStateOf(DeviceTile.isTileAdded())
        private set

    fun markHomeTileCreated() {
        homeTileCreated = true
        DeviceTile.markTileAdded()
    }

    fun loadDeviceInfo(force: Boolean = false): Job? {
        val useApp = state.account.appToken.isNotEmpty()
        val token = if (useApp) state.account.appToken else state.account.token
        val appType = if (useApp) "1,1" else "1,5"
        if (token.isEmpty()) return null
        val now = currentTimeMillis()
        if (!force && now - lastDeviceLoadTime < 5000) return null
        lastDeviceLoadTime = now
        return viewModelScope.launch {
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
                    val realStatus = try { api.getDevStatus(token, dto.id, appType) } catch (e: Exception) { logError("loadDeviceInfo.getDevStatus", e); null }
                    Device(
                        id = dto.id,
                        name = dto.name,
                        status = dto.status,
                        geneStatus = realStatus?.geneStatus ?: dto.geneStatus,
                        deviceStatus = realStatus?.deviceStatus ?: 1,
                        dtype = dto.dtype
                    )
                }
                state = state.copy(
                    devices = updatedDevices,
                    account = state.account.copy(uid = master.accountId.ifEmpty { state.account.uid })
                )
                inferHomeDeviceType(updatedDevices)
            } catch (e: Exception) {
                logError("loadDeviceInfo", e)
            }
        }
    }

    fun loadAccountInfo(): Job? {
        val token = state.account.appToken.ifEmpty { state.account.token }
        if (token.isEmpty()) return null
        val now = currentTimeMillis()
        if (now - lastAccountLoadTime < 5000) return null
        lastAccountLoadTime = now
        return viewModelScope.launch {
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
            } catch (e: Exception) {
                logError("loadAccountInfo", e)
            }
        }
    }

    fun addDevice(id: String) {
        val token = state.account.appToken.ifEmpty { state.account.token }
        if (token.isEmpty()) return
        viewModelScope.launch {
            try {
                val result = api.devFavo(token, id, remove = false)
                if (result.success) {
                    showToast("设备已添加")
                    loadDeviceInfo(force = true)?.join()
                    // 添加成功后自动切换到该设备对应的类型
                    state.devices.firstOrNull { it.id == id }
                        ?.let { HomeDeviceType.fromDeviceType(it.dtype) }
                        ?.let { selectHomeDeviceType(it) }
                }
            } catch (e: Exception) {
                logError("addDevice", e)
                showToast("添加失败：${e.message}")
            }
        }
    }

    // 扫码解析出的设备编号，供 DeviceAddPage 回填输入框
    var scannedDeviceId by mutableStateOf<String?>(null)
        private set

    // 扫码结果在 viewModelScope 中解析，避免随扫码页/添加页离开 composition 被取消
    fun submitScannedRaw(raw: String) {
        viewModelScope.launch {
            val deviceId = resolveScanDeviceId(raw) ?: return@launch
            scannedDeviceId = deviceId
        }
    }

    fun consumeScannedDeviceId() {
        scannedDeviceId = null
    }

    // 解析扫码内容：路径形式直接就是设备编号，?id= 形式经 /qr/use 换取设备编号
    private suspend fun resolveScanDeviceId(raw: String): String? {
        val token = state.account.appToken.ifEmpty { state.account.token }
        if (token.isEmpty()) {
            showToast("请先登录")
            return null
        }
        val parsed = QrCodeParser.parse(raw)
        parsed.deviceId?.takeIf { it.isNotEmpty() }?.let { return it }
        val qrId = parsed.qrId
        if (qrId.isNullOrEmpty()) {
            showToast("无法识别二维码内容")
            return null
        }
        return try {
            val result = api.qrUse(token, qrId)
            if (!result.success) return null
            if (result.type != QR_TYPE_DEVICE && result.type != QR_TYPE_DOOR_LOCK) {
                showToast("暂不支持该类型二维码")
                return null
            }
            val deviceId = result.deviceId
            if (deviceId.isEmpty()) {
                showToast("二维码中不包含设备信息")
                return null
            }
            deviceId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logError("resolveScanDeviceId", e)
            showToast("扫码解析失败：${e.message}")
            null
        }
    }

    fun removeDevice(deviceId: String) {
        val token = state.account.appToken.ifEmpty { state.account.token }
        if (token.isEmpty()) return
        viewModelScope.launch {
            try {
                val result = api.devFavo(token, deviceId, remove = true)
                if (result.success) {
                    showToast("设备已删除")
                    loadDeviceInfo(force = true)
                }
            } catch (e: Exception) {
                logError("removeDevice", e)
                showToast("删除失败：${e.message}")
            }
        }
    }

    // 启动前查询设备可选项；有选项则交由 UI 弹窗选择，无选项直接启动
    var pendingStart by mutableStateOf<DevicePendingStart?>(null)
        private set

    // 快捷设置图块点击后待处理的设备编号，由首页消费并触发启动按钮逻辑
    var pendingExternalDeviceId by mutableStateOf<String?>(null)
        private set

    fun requestExternalDeviceStart(deviceId: String) {
        pendingExternalDeviceId = deviceId
    }

    fun consumeExternalDeviceStart() {
        pendingExternalDeviceId = null
    }

    fun prepareStartDevice(device: Device) {
        val name = device.name.ifEmpty { device.id }
        // 水表(5)/淋浴(6)/直饮水(8) 无模式/多通道，免请求直接弹确认
        if (device.dtype in SIMPLE_DEVICE_TYPES) {
            pendingStart = DevicePendingStart(device.id, name, DeviceStartOptions())
            return
        }
        val useApp = state.account.appToken.isNotEmpty()
        val token = if (useApp) state.account.appToken else state.account.token
        val appType = if (useApp) "1,1" else "1,5"
        if (token.isEmpty()) return
        viewModelScope.launch {
            val options = try {
                api.getDeviceStartOptions(token, device.id, appType)
            } catch (e: Exception) {
                logError("getDeviceStartOptions", e)
                DeviceStartOptions()
            }
            pendingStart = DevicePendingStart(device.id, name, options)
        }
    }

    fun cancelPendingStart() {
        pendingStart = null
    }

    fun confirmPendingStart(selectedIndex: Int) {
        val pending = pendingStart ?: return
        pendingStart = null
        toggleDeviceRunning(pending.deviceId, buildStartArgs(pending.options, selectedIndex))
    }

    // 组装 /dev/start 的 args：优先模式(parts)，其次通道(subs)
    private fun buildStartArgs(options: DeviceStartOptions, selectedIndex: Int): String {
        if (options.parts.isNotEmpty()) {
            val part = options.parts.getOrNull(selectedIndex) ?: return ""
            val ext = options.goods.joinToString(",") { """{"pos":${it.pos},"out":${it.out}}""" }
            return """{"mode":${part.mode},"ext":[$ext]}"""
        }
        if (options.subCount > 1) {
            return """{"pos":$selectedIndex,"mode":null}"""
        }
        return ""
    }

    fun toggleDeviceRunning(deviceId: String, args: String = "", forceStop: Boolean = false) {
        val useApp = state.account.appToken.isNotEmpty()
        val token = if (useApp) state.account.appToken else state.account.token
        val appType = if (useApp) "1,1" else "1,5"
        if (token.isEmpty()) return
        val deviceName = state.devices.firstOrNull { it.id == deviceId }?.name?.ifEmpty { deviceId } ?: deviceId
        pollingDeviceId = deviceId
        viewModelScope.launch {
            try {
                if (!isCurrentToken(token)) return@launch
                val currentStatus = if (forceStop) null else api.getDevStatus(token, deviceId, appType)
                val starting = !forceStop && currentStatus?.geneStatus == 99
                val result = if (starting) {
                    requestNotificationPermission()
                    api.setUseScore(token, useScore = 1, appType = appType)
                    // 优先钱袋支付（91），余额不足等失败时自动切换支付宝免密支付（21）
                    val wallet = api.devStart(token, deviceId, appType, ptype = PAY_TYPE_WALLET, args = args, reportError = false)
                    if (wallet.success) {
                        wallet
                    } else {
                        api.devStart(token, deviceId, appType, ptype = PAY_TYPE_ALIPAY, args = args)
                    }
                } else {
                    api.devEnd(token, deviceId, appType)
                }
                if (!result.success) return@launch
                showToast(if (starting) "设备已启动" else "设备已停止")
                RunNotifications.updateDevice(deviceId, deviceName, starting)
                // 第一轮：5次 × 2.5秒
                var newStatus: DevStatusResult? = null
                var attempt = 0
                while (attempt < 5) {
                    attempt++
                    delay(2500.milliseconds)
                    if (!isCurrentToken(token)) return@launch
                    newStatus = api.getDevStatus(token, deviceId, appType)
                    if (newStatus?.geneStatus != currentStatus?.geneStatus) break
                }
                // 第二轮（如需）：25次 × 5秒
                if (newStatus?.geneStatus == currentStatus?.geneStatus) {
                    attempt = 0
                    while (attempt < 25) {
                        attempt++
                        delay(5000.milliseconds)
                        if (!isCurrentToken(token)) return@launch
                        newStatus = api.getDevStatus(token, deviceId, appType)
                        if (newStatus?.geneStatus != currentStatus?.geneStatus) break
                    }
                }
                if (!isCurrentToken(token)) return@launch
                state = state.copy(
                    devices = state.devices.map {
                        if (it.id == deviceId) it.copy(
                            geneStatus = newStatus?.geneStatus ?: it.geneStatus,
                            deviceStatus = newStatus?.deviceStatus ?: it.deviceStatus
                        ) else it
                    }
                )
                val isRunning = newStatus?.geneStatus?.let { it != 99 } ?: (currentStatus?.geneStatus?.let { it != 99 } ?: starting)
                // 启动后以轮询结果校正，停止时已立即上报“已停止”
                if (starting) {
                    RunNotifications.updateDevice(deviceId, deviceName, isRunning)
                }
            } catch (e: Exception) {
                showToast("操作失败：${e.message}")
            } finally {
                if (pollingDeviceId == deviceId) pollingDeviceId = null
            }
        }
    }

    // 通知“停止”按钮：强制停止对应设备（不依赖当前状态判断）
    fun stopDevice(deviceId: String) {
        toggleDeviceRunning(deviceId, forceStop = true)
    }

    // 积分/任务
    var missions by mutableStateOf<List<MissionInfo>>(emptyList())
        private set

    private data class ScoreListState(
        val records: List<ScoreRecord> = emptyList(),
        val page: Int = 0,
        val hasMore: Boolean = false,
        val loadingMore: Boolean = false,
        val loaded: Boolean = false
    )

    private var scoreLists by mutableStateOf(
        ScoreFilter.entries.associateWith { ScoreListState() }
    )

    var scoreFilter by mutableStateOf(ScoreFilter.All)
        private set

    val scoreRecords: List<ScoreRecord>
        get() = scoreLists[scoreFilter]?.records ?: emptyList()
    val scoreHasMore: Boolean
        get() = scoreLists[scoreFilter]?.hasMore ?: false
    val scoreLoadingMore: Boolean
        get() = scoreLists[scoreFilter]?.loadingMore ?: false

    private fun toScoreRecord(dto: ScoreDto) = ScoreRecord(score = dto.score, name = dto.name, time = dto.time)

    // 积分数据读取优先用设备登录凭据（appToken）
    private fun scoreToken(): String = state.account.appToken.ifEmpty { state.account.token }

    private fun emptyScoreLists(): Map<ScoreFilter, ScoreListState> =
        ScoreFilter.entries.associateWith { ScoreListState() }

    private fun loadScoreFirstPage(filter: ScoreFilter): Job? {
        val token = scoreToken()
        if (token.isEmpty()) return null
        val src = filter.src
        return viewModelScope.launch {
            try {
                val result = api.getScoreList(token, page = 0, size = SCORE_PAGE_SIZE, src = src)
                if (scoreToken() != token) return@launch
                scoreLists = scoreLists + (filter to ScoreListState(
                    records = result.records.map(::toScoreRecord),
                    page = 0,
                    hasMore = result.total > result.records.size,
                    loaded = true
                ))
            } catch (e: Exception) {
                logError("loadScoreFirstPage", e)
            }
        }
    }

    fun loadScoreInfo() {
        val token = scoreToken()
        if (token.isEmpty()) return
        val now = currentTimeMillis()
        if (now - lastScoreLoadTime < 5000) return
        lastScoreLoadTime = now
        loadScoreFirstPage(scoreFilter)
        loadScoreSummary()
    }

    fun loadScoreInfoNoCooldown(): Job? {
        return loadScoreFirstPage(scoreFilter)
    }

    fun selectScoreFilter(filter: ScoreFilter) {
        if (scoreFilter == filter) return
        scoreFilter = filter
        // 分类已缓存则直接展示；未缓存（如首次加载失败）时补一次请求
        if (scoreLists[filter]?.loaded != true) loadScoreFirstPage(filter)
    }

    fun loadMoreScores() {
        val filter = scoreFilter
        val current = scoreLists[filter] ?: return
        val token = scoreToken()
        if (token.isEmpty() || current.loadingMore || !current.hasMore || !current.loaded) return
        scoreLists = scoreLists + (filter to current.copy(loadingMore = true))
        val src = filter.src
        viewModelScope.launch {
            try {
                val nextPage = current.page + 1
                val result = api.getScoreList(token, page = nextPage, size = SCORE_PAGE_SIZE, src = src)
                if (scoreToken() != token) return@launch
                val latest = scoreLists[filter] ?: return@launch
                val seen = latest.records.map { "${it.time}|${it.score}|${it.name}" }.toMutableSet()
                val appended = result.records.map(::toScoreRecord)
                    .filter { seen.add("${it.time}|${it.score}|${it.name}") }
                scoreLists = scoreLists + (filter to latest.copy(
                    records = latest.records + appended,
                    page = nextPage,
                    hasMore = (nextPage + 1) * SCORE_PAGE_SIZE < result.total,
                    loadingMore = false
                ))
            } catch (e: Exception) {
                logError("loadMoreScores", e)
                val latest = scoreLists[filter]
                if (latest != null) {
                    scoreLists = scoreLists + (filter to latest.copy(loadingMore = false))
                }
            }
        }
    }

    // 积分概览
    fun loadScoreSummary(): Job? {
        val token = scoreToken()
        if (token.isEmpty()) return null
        return viewModelScope.launch {
            try {
                val result = api.getMissionList(token)
                if (scoreToken() != token) return@launch
                state = state.copy(
                    points = state.points.copy(
                        available = result.validScore ?: state.points.available,
                        total = result.totalScore ?: state.points.total
                    )
                )
            } catch (e: Exception) {
                logError("loadScoreSummary", e)
            }
        }
    }

    // 进入积分明细页：重置为全部分类，一次性拉取全部分类并缓存，之后切换分类直接读缓存
    fun refreshScorePage() {
        scoreFilter = ScoreFilter.All
        lastScoreLoadTime = currentTimeMillis()
        ScoreFilter.entries.forEach { loadScoreFirstPage(it) }
        loadScoreSummary()
    }

    var scoreRefreshing by mutableStateOf(false)
        private set

    fun refreshScores() {
        if (scoreRefreshing) return
        scoreRefreshing = true
        viewModelScope.launch {
            try {
                lastScoreLoadTime = currentTimeMillis()
                val jobs = ScoreFilter.entries.mapNotNull { loadScoreFirstPage(it) } +
                    listOfNotNull(loadScoreSummary())
                jobs.joinAll()
            } finally {
                scoreRefreshing = false
            }
        }
    }

    // 钱包＆账单
    var billLoadingMore by mutableStateOf(false)
        private set
    var billHasMore by mutableStateOf(false)
        private set

    // 账单类型（1 未付款 / 3 已付款 / 2 待确认 / 4 付款失败 / 9 已取消）
    var billStatus by mutableStateOf(BILL_STATUS)
        private set

    private data class BillCacheEntry(
        val records: List<BillRecord>,
        val page: Int,
        val hasMore: Boolean
    )

    // 各账单类型的数据缓存，进入页面/刷新时清空，切换类型时命中缓存则不重复请求
    private var billCache = mutableMapOf<Int, BillCacheEntry>()

    private var billPage = 0
    private var billLoadToken = ""

    var refundProgress by mutableStateOf<RefundProgress?>(null)
        private set

    private fun walletToken(): String = state.account.appToken.ifEmpty { state.account.token }

    fun loadWallet(): Job? {
        val token = walletToken()
        if (token.isEmpty()) return null
        return viewModelScope.launch {
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
            } catch (e: Exception) {
                logError("loadWallet", e)
            }
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
            } catch (e: Exception) {
                logError("loadWalletDetail", e)
            }
        }
    }

    fun selectWallet(id: String) {
        if (state.activeWalletId == id) return
        rechargeProducts = emptyList()
        state = state.copy(activeWalletId = id)
        state.wallets.firstOrNull { it.id == id }?.let { loadWalletDetail(it) }
    }

    private fun loadBillFirstPage(token: String): Job {
        val status = billStatus
        return viewModelScope.launch {
            try {
                val result = api.getBillList(token, page = 0, size = BILL_PAGE_SIZE, status = status)
                if (walletToken() != token) return@launch
                val hasMore = result.total > result.records.size
                billCache[status] = BillCacheEntry(result.records, 0, hasMore)
                // 请求期间已切换到其它类型，仅写入缓存
                if (status != billStatus) return@launch
                billLoadToken = token
                billPage = 0
                billHasMore = hasMore
                state = state.copy(billRecords = result.records)
            } catch (e: Exception) {
                logError("loadBillFirstPage", e)
            }
        }
    }

    fun selectBillStatus(status: Int) {
        if (billStatus == status) return
        billStatus = status
        billLoadingMore = false
        val cached = billCache[status]
        if (cached != null) {
            billLoadToken = walletToken()
            billPage = cached.page
            billHasMore = cached.hasMore
            state = state.copy(billRecords = cached.records)
        } else {
            billLoadToken = ""
            billPage = 0
            billHasMore = false
            state = state.copy(billRecords = emptyList())
            loadBillsNoCooldown()
        }
    }

    fun loadBillsNoCooldown(): Job? {
        val token = walletToken()
        if (token.isEmpty()) return null
        return loadBillFirstPage(token)
    }

    fun loadMoreBills() {
        val token = walletToken()
        val status = billStatus
        if (token.isEmpty() || billLoadingMore || !billHasMore || billLoadToken != token) return
        billLoadingMore = true
        viewModelScope.launch {
            try {
                val nextPage = billPage + 1
                val result = api.getBillList(token, page = nextPage, size = BILL_PAGE_SIZE, status = status)
                if (walletToken() != token || status != billStatus) return@launch
                billPage = nextPage
                val seen = state.billRecords.map { it.id }.toMutableSet()
                val appended = result.records.filter { it.id.isEmpty() || seen.add(it.id) }
                val newRecords = state.billRecords + appended
                billHasMore = (billPage + 1) * BILL_PAGE_SIZE < result.total
                billCache[status] = BillCacheEntry(newRecords, billPage, billHasMore)
                state = state.copy(billRecords = newRecords)
            } catch (e: Exception) {
                logError("loadMoreBills", e)
            } finally {
                billLoadingMore = false
            }
        }
    }

    // 进入我的账单页：重置为默认（已付款），清空缓存后重新请求
    fun refreshBillPage() {
        billStatus = BILL_STATUS
        billCache = mutableMapOf()
        billPage = 0
        billHasMore = false
        billLoadingMore = false
        billLoadToken = ""
        state = state.copy(billRecords = emptyList())
        loadWallet()
        loadBillsNoCooldown()
    }

    var billRefreshing by mutableStateOf(false)
        private set

    fun refreshBills() {
        if (billRefreshing) return
        billRefreshing = true
        // 下拉刷新：清空缓存后重新请求当前类型
        billCache = mutableMapOf()
        viewModelScope.launch {
            try {
                listOfNotNull(loadWallet(), loadBillsNoCooldown()).joinAll()
            } finally {
                billRefreshing = false
            }
        }
    }

    // --- Spending stats (home) ---
    var spendingStats by mutableStateOf(SpendingStats())
        private set

    fun loadSpendingStats(): Job? {
        val token = walletToken()
        if (token.isEmpty()) return null
        val now = currentTimeMillis()
        if (now - lastSpendingLoadTime < 5000) return null
        lastSpendingLoadTime = now
        return viewModelScope.launch {
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
            } catch (e: Exception) {
                logError("loadSpendingStats", e)
            }
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
            } catch (e: Exception) {
                logError("loadRechargeProducts", e)
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
                ?: return AlipayPayResult(false, "", "")
            val orderInfo = api.prepayAlipay(token, orderId)
                ?: return AlipayPayResult(false, "", "")
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

    // 退款
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
                result
            } else {
                // 失败信息已由 API 层统一 Toast，这里返回空信息避免重复
                DevResult(false, result.code, "")
            }
        } catch (e: Exception) {
            DevResult(false, -1, "退款失败：${e.message}")
        } finally {
            refundSubmitting = false
        }
    }

    private data class MergedMissions(
        val missions: List<MissionInfo>,
        val weekMask: Int,
        val dailyAdId: String,
        val dailyScore: Int,
        val dailyToken: String,
        val validScore: Int?,
        val totalScore: Int?
    )

    // 合并两个登录渠道（appToken 1,1 与 token 1,5）的任务列表，按 adId 去重并记录来源 token
    private suspend fun fetchMergedMissions(): MergedMissions {
        val appToken = state.account.appToken
        val pointsToken = state.account.token
        val tokens = buildList {
            if (appToken.isNotEmpty()) add(appToken)
            if (pointsToken.isNotEmpty() && pointsToken != appToken) add(pointsToken)
        }
        val merged = LinkedHashMap<String, MissionInfo>()
        var weekMask = 0
        var dailyAdId = ""
        var dailyScore = 5
        var dailyToken = ""
        var validScore: Int? = null
        var totalScore: Int? = null
        val todayStart = getTodayStart(currentTimeMillis())
        for (tok in tokens) {
            val result = try {
                api.getMissionList(tok)
            } catch (e: Exception) {
                logError("fetchMergedMissions.missionLst", e)
                continue
            }
            if (dailyToken.isEmpty() && result.dailyAdId.isNotBlank()) {
                dailyAdId = result.dailyAdId
                dailyScore = result.dailyScore
                dailyToken = tok
            }
            weekMask = weekMask or result.weekMask
            result.validScore?.let { v -> validScore = validScore?.let { maxOf(it, v) } ?: v }
            result.totalScore?.let { v -> totalScore = totalScore?.let { maxOf(it, v) } ?: v }

            val todayCount = mutableMapOf<String, Int>()
            val scores = try {
                api.getScoreList(tok).records
            } catch (e: Exception) {
                logError("fetchMergedMissions.scoreLst", e)
                emptyList()
            }
            for (score in scores) {
                if (score.score > 0 && score.adId.isNotEmpty()) {
                    val timeMs = score.time.toLongOrNull() ?: continue
                    if (timeMs >= todayStart) {
                        todayCount[score.adId] = (todayCount[score.adId] ?: 0) + 1
                    }
                }
            }

            for (m in result.missions) {
                if (m.adId.isBlank()) continue
                val done = maxOf(m.dailyCompleted, todayCount[m.adId] ?: 0)
                val existing = merged[m.adId]
                if (existing == null) {
                    merged[m.adId] = MissionInfo(
                        adId = m.adId,
                        name = m.name,
                        score = m.score,
                        limit = m.limit,
                        dailyCompleted = done,
                        isDailySignin = m.isDailySignin,
                        sourceToken = tok
                    )
                } else {
                    val keepExisting = existing.limit > 0 || m.limit <= 0
                    merged[m.adId] = existing.copy(
                        name = existing.name.ifBlank { m.name },
                        score = maxOf(existing.score, m.score),
                        limit = if (keepExisting) existing.limit else m.limit,
                        dailyCompleted = maxOf(existing.dailyCompleted, done),
                        sourceToken = if (keepExisting) existing.sourceToken else tok
                    )
                }
            }
        }
        return MergedMissions(
            missions = merged.values.toList(),
            weekMask = weekMask,
            dailyAdId = dailyAdId,
            dailyScore = dailyScore,
            dailyToken = dailyToken,
            validScore = validScore,
            totalScore = totalScore
        )
    }

    fun loadMissions() {
        if (state.account.appToken.isEmpty() && state.account.token.isEmpty()) return
        val now = currentTimeMillis()
        if (now - lastMissionsLoadTime < 5000) return
        lastMissionsLoadTime = now
        val startApp = state.account.appToken
        val startPoints = state.account.token
        viewModelScope.launch {
            try {
                val merged = fetchMergedMissions()
                // 账号已切换/退出登录，丢弃过期结果
                if (state.account.appToken != startApp || state.account.token != startPoints) return@launch
                missions = merged.missions
                state = state.copy(
                    weekMask = merged.weekMask,
                    points = state.points.copy(
                        available = merged.validScore ?: state.points.available,
                        total = merged.totalScore ?: state.points.total
                    )
                )
            } catch (e: Exception) {
                logError("loadMissions", e)
            }
        }
    }

    fun runAllTasks() {
        if (state.taskCompleted || isLoading) return
        val uid = state.account.uid
        if ((state.account.appToken.isEmpty() && state.account.token.isEmpty()) || uid.isEmpty()) {
            errorMessage = "需要积分登录才能执行任务"
            return
        }
        val startApp = state.account.appToken
        val startPoints = state.account.token
        val isCurrentAccount = { state.account.appToken == startApp && state.account.token == startPoints }
        lastMissionsLoadTime = 0L
        lastScoreLoadTime = 0L
        requestNotificationPermission()
        showToast("开始运行积分任务")
        taskJob = viewModelScope.launch {
            isLoading = true
            errorMessage = null
            state = state.copy(taskLogs = emptyList())
            rateLimiter.clear()
            RunNotifications.updateTask(0)
            try {
                addTaskLog("正在加载任务列表...")
                val merged = fetchMergedMissions()
                if (merged.validScore != null || merged.totalScore != null) {
                    state = state.copy(
                        points = state.points.copy(
                            available = merged.validScore ?: state.points.available,
                            total = merged.totalScore ?: state.points.total
                        )
                    )
                }
                val missionList = merged.missions
                missions = missionList
                var gained = 0

                // Daily sign-in
                if (merged.dailyAdId.isNotBlank() && merged.dailyToken.isNotEmpty()) {
                    val signToken = merged.dailyToken
                    val weekDay = getDayOfWeek()
                    val alreadySigned = (merged.weekMask and (1 shl (weekDay - 1))) != 0
                    if (alreadySigned) {
                        addTaskLog("每日签到: 今日已签到")
                    } else {
                        val delay = rateLimiter.reserveDelay(signToken, currentTimeMillis())
                        if (delay > 0) {
                            addTaskLog("等待 ${delay / 1000} 秒...")
                            delay(delay.milliseconds)
                        }
                        addTaskLog("每日签到: 执行中...")
                        val signResult = api.signIn(signToken, uid, weekDay, merged.dailyAdId)
                        if (signResult.success) {
                            gained += merged.dailyScore
                            RunNotifications.updateTask(gained)
                            addTaskLog("每日签到: 成功 +${merged.dailyScore}分")
                        } else if (signResult.code == -98) {
                            addTaskLog("每日签到: 请求频繁，等待60秒重试...")
                            delay(60.seconds)
                            val retryResult = api.signIn(signToken, uid, weekDay, merged.dailyAdId)
                            if (retryResult.success) {
                                gained += merged.dailyScore
                                RunNotifications.updateTask(gained)
                                addTaskLog("每日签到: 成功 +${merged.dailyScore}分")
                            } else {
                                addTaskLog("每日签到: 失败 ${retryResult.message}")
                            }
                        } else {
                            addTaskLog("每日签到: 失败 ${signResult.message}")
                        }
                    }
                }

                // 任务
                val validMissions = missionList.filter { !it.isDailySignin && it.limit > 0 && it.score > 0 && it.sourceToken.isNotEmpty() }
                addTaskLog("共 ${validMissions.size} 个可执行任务")
                var executed = 0
                for (mission in validMissions) {
                    if (!isActive) break
                    val execToken = mission.sourceToken
                    val completed = mission.dailyCompleted
                    val maxCount = mission.limit
                    if (completed >= maxCount) {
                        addTaskLog("[${mission.name}] 已完成 $completed/$maxCount，跳过")
                        continue
                    }
                    for (round in (completed + 1)..maxCount) {
                        if (!isActive) break
                        val delay = rateLimiter.reserveDelay(execToken, currentTimeMillis())
                        if (delay > 0) {
                            delay(delay.milliseconds)
                        }
                        addTaskLog("[${mission.name}] ($round/$maxCount) 执行中...")
                        var execResult = api.executeMission(execToken, uid, mission.adId)
                        if (execResult.code == -98) {
                            addTaskLog("[${mission.name}] 请求频繁，等待60秒重试...")
                            delay(60.seconds)
                            execResult = api.executeMission(execToken, uid, mission.adId)
                        }
                        if (execResult.success) {
                            executed++
                            gained += mission.score
                            RunNotifications.updateTask(gained)
                            addTaskLog("[${mission.name}] ($round/$maxCount) 成功 +${mission.score}分")
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
                if (isActive && isCurrentAccount()) {
                    addTaskLog("任务完成，共获得 $gained 积分")
                    showToast("积分任务已完成，共获得 $gained 积分")
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
            } catch (_: CancellationException) {
                if (isCurrentAccount()) addTaskLog("任务已停止")
            } catch (e: Exception) {
                if (isCurrentAccount()) {
                    addTaskLog("任务执行异常: ${e.message}")
                    errorMessage = "任务执行失败: ${e.message}"
                }
            } finally {
                RunNotifications.removeTask()
                if (isCurrentAccount()) isLoading = false
                if (taskJob === coroutineContext[Job]) taskJob = null
            }
        }
    }

    fun stopTasks() {
        addTaskLog("正在停止...")
        taskJob?.cancel()
        taskJob = null
        isLoading = false
        RunNotifications.removeTask()
        showToast("积分任务已停止")
    }

    fun setTaskCompleted() {
        state = state.copy(taskCompleted = true)
    }

    private fun addTaskLog(message: String) {
        val time = currentTimeFormatted("HH:mm:ss")
        state = state.copy(taskLogs = state.taskLogs + "[$time] $message")
    }

    // 设置
    fun setDynamicColor(enabled: Boolean) {
        state = state.copy(dynamicColor = enabled)
        AppStorage.instance.saveBoolean(StorageKeys.DYNAMIC_COLOR, enabled)
    }

    fun setCustomColor(enabled: Boolean) {
        state = state.copy(customColor = enabled)
        AppStorage.instance.saveBoolean(StorageKeys.CUSTOM_COLOR, enabled)
    }

    fun setPaletteStyle(style: PaletteStyle) {
        state = state.copy(paletteStyle = style)
        AppStorage.instance.saveString(StorageKeys.PALETTE_STYLE, style.name)
    }

    fun setSeedColor(color: Int) {
        state = state.copy(seedColor = color)
        AppStorage.instance.saveInt(StorageKeys.SEED_COLOR, color)
    }

    fun setFloatingNav(enabled: Boolean) {
        state = state.copy(floatingNav = enabled)
        AppStorage.instance.saveBoolean(StorageKeys.FLOATING_NAV, enabled)
    }

    fun setAppBlur(enabled: Boolean) {
        state = state.copy(appBlur = enabled)
        AppStorage.instance.saveBoolean(StorageKeys.APP_BLUR, enabled)
    }

    fun setPredictiveBackEnabled(enabled: Boolean) {
        state = state.copy(predictiveBackEnabled = enabled)
        AppStorage.instance.saveBoolean(StorageKeys.PREDICTIVE_BACK, enabled)
    }

    fun setThemeMode(mode: ThemeMode) {
        state = state.copy(themeMode = mode)
        AppStorage.instance.saveString(StorageKeys.THEME_MODE, mode.name)
    }

    fun clearError() {
        errorMessage = null
    }

    var homeRefreshing by mutableStateOf(false)
        private set

    fun refreshHome() {
        if (homeRefreshing) return
        homeRefreshing = true
        viewModelScope.launch {
            try {
                lastDeviceLoadTime = 0L
                lastAccountLoadTime = 0L
                lastScoreLoadTime = 0L
                lastSpendingLoadTime = 0L
                listOfNotNull(
                    loadDeviceInfo(force = true),
                    loadAccountInfo(),
                    loadScoreInfoNoCooldown(),
                    loadScoreSummary(),
                    loadSpendingStats()
                ).joinAll()
            } finally {
                homeRefreshing = false
            }
        }
    }

    override fun onCleared() {
        AppLifecycle.onResumed = null
        super.onCleared()
    }

    fun checkForUpdate() {
        if (downloadingUpdate) {
            // 正在下载时再次点击：重新弹出下载进度对话框（同时收起通知）
            reopenUpdateProgressDialog()
            return
        }
        if (checkingUpdate) return
        checkingUpdate = true
        showToast("正在检查更新")
        viewModelScope.launch {
            try {
                val release = AppUpdate.fetchLatestRelease()
                val remoteVersion = release.tagName.removePrefix("v").trim()
                if (AppUpdate.compareVersions(remoteVersion, getAppVersion()) > 0) {
                    dismissToast()
                    val asset = AppUpdate.selectAsset(release, currentAbis())
                    if (asset == null || asset.browserDownloadUrl.isEmpty()) {
                        showToast("暂无本设备安装包")
                        return@launch
                    }
                    pendingUpdateUrl = asset.browserDownloadUrl
                    pendingUpdateSha256 = asset.digest?.substringAfter(':')?.takeIf { it.isNotBlank() }
                    pendingUpdateSize = asset.size
                    updateDialog = UpdateDialogState.Available(remoteVersion)
                } else {
                    dismissToast()
                    showToast("已是最新版")
                }
            } catch (e: Exception) {
                logError("checkForUpdate", e)
                dismissToast()
                showToast("检查更新失败")
            } finally {
                checkingUpdate = false
            }
        }
    }

    fun startUpdate() {
        if (downloadingUpdate) return
        val url = pendingUpdateUrl
        if (url.isEmpty()) {
            updateDialog = UpdateDialogState.None
            return
        }
        // 已下载的安装包与目标一致时直接安装，无需重复下载
        val existing = downloadedApkPathIfValid(pendingUpdateSha256, pendingUpdateSize)
        if (existing != null) {
            downloadedApkPath = existing
            progressDialogDismissed = false
            afterDownload(existing)
            return
        }
        downloadingUpdate = true
        progressDialogDismissed = false
        lastUpdateProgress = 0f
        downloadedApkPath = null
        updateDialog = UpdateDialogState.Downloading(0f)
        updateJob = viewModelScope.launch {
            try {
                val path = downloadApkToFile(AppUpdate.applyProxy(url, githubProxyUrl)) { progress ->
                    lastUpdateProgress = progress
                    if (progressDialogDismissed) {
                        showUpdateProgressNotification(progress)
                    } else {
                        updateDialog = UpdateDialogState.Downloading(progress)
                    }
                }
                downloadedApkPath = path
                cancelUpdateProgressNotification()
                afterDownload(path)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logError("startUpdate", e)
                cancelUpdateProgressNotification()
                showToast("下载失败：${e.message}")
                updateDialog = UpdateDialogState.None
            } finally {
                downloadingUpdate = false
                updateJob = null
            }
        }
    }

    fun hideUpdateProgressDialog() {
        progressDialogDismissed = true
        updateDialog = UpdateDialogState.None
        requestNotificationPermission()
        showUpdateProgressNotification(lastUpdateProgress)
    }

    fun reopenUpdateProgressDialog() {
        if (!downloadingUpdate) return
        progressDialogDismissed = false
        cancelUpdateProgressNotification()
        updateDialog = UpdateDialogState.Downloading(lastUpdateProgress)
    }

    fun stopUpdate() {
        updateJob?.cancel()
        updateJob = null
        downloadingUpdate = false
        progressDialogDismissed = false
        lastUpdateProgress = 0f
        downloadedApkPath = null
        cancelUpdateProgressNotification()
        deleteDownloadedApk()
        updateDialog = UpdateDialogState.None
    }

    private fun afterDownload(path: String) {
        if (canInstallPackages()) {
            installApkOrPrompt(path)
        } else {
            openInstallPermissionSettings()
            updateDialog = UpdateDialogState.AwaitingPermission
        }
    }

    private fun installApkOrPrompt(path: String) {
        updateDialog = if (installApk(path)) {
            UpdateDialogState.None
        } else {
            UpdateDialogState.NeedPermission
        }
    }

    private fun onAppResumed() {
        if (updateDialog != UpdateDialogState.AwaitingPermission) return
        val path = downloadedApkPath ?: return
        if (canInstallPackages()) {
            installApkOrPrompt(path)
        } else {
            updateDialog = UpdateDialogState.NeedPermission
        }
    }

    fun openInstallSettings() {
        val path = downloadedApkPath
        if (path != null && canInstallPackages()) {
            installApkOrPrompt(path)
        } else {
            openInstallPermissionSettings()
            updateDialog = UpdateDialogState.AwaitingPermission
        }
    }

    fun dismissUpdateDialog() {
        updateDialog = UpdateDialogState.None
    }

    fun setGithubProxy(url: String) {
        githubProxyUrl = url.trim()
        try {
            AppStorage.instance.saveString(StorageKeys.GITHUB_PROXY, githubProxyUrl)
        } catch (e: Exception) {
            logError("setGithubProxy", e)
        }
    }

    fun enableDeveloperMode() {
        developerMode = true
        try {
            AppStorage.instance.saveBoolean(StorageKeys.DEVELOPER_MODE, true)
        } catch (e: Exception) {
            logError("enableDeveloperMode", e)
        }
    }

    fun disableDeveloperMode() {
        developerMode = false
        try {
            AppStorage.instance.saveBoolean(StorageKeys.DEVELOPER_MODE, false)
        } catch (e: Exception) {
            logError("disableDeveloperMode", e)
        }
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
