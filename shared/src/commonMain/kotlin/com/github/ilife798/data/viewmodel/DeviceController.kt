package com.github.ilife798.data.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.ilife798.AppStorage
import com.github.ilife798.RunNotifications
import com.github.ilife798.StorageKeys
import com.github.ilife798.data.api.APP_TYPE_DEVICE
import com.github.ilife798.data.api.APP_TYPE_POINTS
import com.github.ilife798.data.api.DevStatusResult
import com.github.ilife798.data.api.IlifeApi
import com.github.ilife798.data.model.AppState
import com.github.ilife798.data.model.Device
import com.github.ilife798.data.model.DeviceDetailInfo
import com.github.ilife798.data.model.HomeDeviceType
import com.github.ilife798.update.requestNotificationPermission
import com.github.ilife798.util.QrCodeParser
import com.github.ilife798.util.currentTimeMillis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val PAY_TYPE_WALLET = 91
private const val PAY_TYPE_ALIPAY = 21

// 运行中设备的状态轮询间隔（秒），用于感知机器端手动停止
private const val DEVICE_MONITOR_INTERVAL_SECONDS = 5L

// 设备列表、启停与扫码解析。
class DeviceController(
    private val scope: CoroutineScope,
    private val apiProvider: () -> IlifeApi,
    private val getState: () -> AppState,
    private val setState: ((AppState) -> AppState) -> Unit,
    private val onToast: (String) -> Unit,
    private val onLogError: (String, Exception) -> Unit,
    private val onSessionExpired: () -> Unit,
) {
    private val api: IlifeApi get() = apiProvider()

    // 选择的设备类型，默认取已收藏设备的类型（按 洗烘/吹风/饮水/淋浴 顺序取最上），
    // 用户手动选择后持久化，下次进入或重启沿用
    var homeDeviceType by mutableStateOf(HomeDeviceType.Shower)
        private set
    private var homeDeviceTypeExplicit = false

    // 开启自动抵扣（acc/upt useScore）
    var autoDeduct by mutableStateOf(true)
        private set

    // 扫码解析出的设备编号，供导航跳转到对应设备详情页
    var scannedDeviceId by mutableStateOf<String?>(null)
        private set

    // 当前打开的设备详情（模式/通道/商家等，status?more=true 全量数据）
    var deviceDetail by mutableStateOf<DeviceDetailInfo?>(null)
        private set

    var detailDeviceId by mutableStateOf<String?>(null)
        private set

    var pollingDeviceId by mutableStateOf<String?>(null)
        private set

    private val runningDevices = mutableMapOf<String, String>()
    private var deviceMonitorJob: Job? = null
    private var lastDeviceLoadTime = 0L

    private fun currentToken(): String = getState().account.preferredToken

    private fun isCurrentToken(token: String): Boolean {
        val account = getState().account
        return token.isNotEmpty() && (account.token == token || account.appToken == token)
    }

    fun loadHomeDeviceTypePref() {
        try {
            val saved = AppStorage.instance.getString(StorageKeys.HOME_DEVICE_TYPE)
            val type = HomeDeviceType.entries.firstOrNull { it.name == saved } ?: return
            homeDeviceType = type
            homeDeviceTypeExplicit = true
        } catch (e: Exception) {
            onLogError("loadHomeDeviceTypePref", e)
        }
    }

    fun selectHomeDeviceType(type: HomeDeviceType) {
        if (homeDeviceType == type && homeDeviceTypeExplicit) return
        homeDeviceType = type
        homeDeviceTypeExplicit = true
        try {
            AppStorage.instance.saveString(StorageKeys.HOME_DEVICE_TYPE, type.name)
        } catch (e: Exception) {
            onLogError("selectHomeDeviceType", e)
        }
    }

    fun updateAutoDeduct(enabled: Boolean) {
        autoDeduct = enabled
        val useApp = getState().account.appToken.isNotEmpty()
        val token = if (useApp) getState().account.appToken else getState().account.token
        val appType = if (useApp) APP_TYPE_DEVICE else APP_TYPE_POINTS
        if (token.isEmpty()) return
        scope.launch {
            try {
                val result = api.setUseScore(token, value = if (enabled) 1 else 0, appType = appType)
                if (!result.success) onToast("设置失败，请稍后再试")
            } catch (e: Exception) {
                onLogError("updateAutoDeduct", e)
            }
        }
    }

    private fun inferHomeDeviceType(devices: List<Device>) {
        if (homeDeviceTypeExplicit) return
        val dtypes = devices.map { it.dtype }.filter { it != 0 }.toSet()
        if (dtypes.isEmpty()) return
        // 优先精确匹配四类场景，全都不是时归入“其它”
        val matched =
            HomeDeviceType.entries
                .firstOrNull { it != HomeDeviceType.Other && it.deviceType in dtypes }
                ?: HomeDeviceType.Other.takeIf { dtypes.any { HomeDeviceType.fromDeviceType(it) == HomeDeviceType.Other } }
        matched?.let { homeDeviceType = it }
    }

    fun reset() {
        deviceMonitorJob?.cancel()
        deviceMonitorJob = null
        runningDevices.clear()
        pollingDeviceId = null
        deviceDetail = null
        detailDeviceId = null
        lastDeviceLoadTime = 0L
    }

    fun loadDeviceInfo(force: Boolean = false): Job? {
        val useApp = getState().account.appToken.isNotEmpty()
        val token = if (useApp) getState().account.appToken else getState().account.token
        val appType = if (useApp) APP_TYPE_DEVICE else APP_TYPE_POINTS
        if (token.isEmpty()) return null
        val now = currentTimeMillis()
        if (!force && now - lastDeviceLoadTime < LOAD_COOLDOWN_MS) return null
        lastDeviceLoadTime = now
        return scope.launch {
            try {
                val master = api.getMasterDevices(token)
                // 账号已切换/退出登录，丢弃过期结果
                if (!isCurrentToken(token)) return@launch
                // 登录态失效：主接口不再返回账户数据
                if (master.accountId.isEmpty() && master.devices.isEmpty()) {
                    onSessionExpired()
                    return@launch
                }
                val devices = master.devices
                val updatedDevices =
                    devices.map { dto ->
                        val realStatus =
                            try {
                                api.getDevStatus(token, dto.id, appType)
                            } catch (e: Exception) {
                                onLogError("loadDeviceInfo.getDevStatus", e)
                                null
                            }
                        Device(
                            id = dto.id,
                            name = dto.name,
                            geneStatus = realStatus?.geneStatus ?: dto.geneStatus,
                            deviceStatus = realStatus?.deviceStatus ?: 1,
                            dtype = dto.dtype,
                        )
                    }
                val account = getState().account
                setState {
                    it.copy(
                        devices = updatedDevices,
                        account = account.copy(uid = master.accountId.ifEmpty { account.uid }),
                    )
                }
                inferHomeDeviceType(updatedDevices)
            } catch (e: Exception) {
                onLogError("loadDeviceInfo", e)
            }
        }
    }

    fun addDevice(id: String) {
        val token = currentToken()
        if (token.isEmpty()) return
        scope.launch {
            try {
                val result = api.devFavo(token, id, remove = false)
                if (result.success) {
                    onToast("设备已添加")
                    // 乐观加入收藏列表，使详情页立即显示“已收藏”；
                    // 完整刷新与首页类型切换延迟到离开详情页时执行
                    setState { state ->
                        if (state.devices.any { it.id == id }) {
                            state
                        } else {
                            state.copy(
                                devices =
                                    state.devices +
                                        Device(
                                            id = id,
                                            name = "",
                                            geneStatus = 99,
                                            deviceStatus = 1,
                                            dtype = 0,
                                        ),
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                onLogError("addDevice", e)
                onToast("添加失败：${e.message}")
            }
        }
    }

    // 离开设备详情页：刷新首页设备列表，并按该设备类型切换首页类型选择
    fun onDeviceDetailClosed(deviceId: String) {
        scope.launch {
            try {
                loadDeviceInfo(force = true)?.join()
                getState()
                    .devices
                    .firstOrNull { it.id == deviceId }
                    ?.let { HomeDeviceType.fromDeviceType(it.dtype) }
                    ?.let { selectHomeDeviceType(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onLogError("onDeviceDetailClosed", e)
            }
        }
    }

    // 扫码结果在 scope 中解析，避免随扫码页/添加页离开 composition 被取消
    fun submitScannedRaw(raw: String) {
        scope.launch {
            val deviceId = resolveScanDeviceId(raw) ?: return@launch
            scannedDeviceId = deviceId
        }
    }

    fun consumeScannedDeviceId() {
        scannedDeviceId = null
    }

    // 解析扫码内容：路径形式直接就是设备编号，?id= 形式经 /qr/use 换取设备编号
    private suspend fun resolveScanDeviceId(raw: String): String? {
        val token = currentToken()
        if (token.isEmpty()) {
            onToast("请先登录")
            return null
        }
        val parsed = QrCodeParser.parse(raw)
        parsed.deviceId?.takeIf { it.isNotEmpty() }?.let { return it }
        val qrId = parsed.qrId
        if (qrId.isNullOrEmpty()) {
            onToast("未知二维码内容")
            return null
        }
        return try {
            when (val outcome = resolveQrUseResult(api.qrUse(token, qrId))) {
                is QrResolveOutcome.Device -> {
                    outcome.deviceId
                }

                QrResolveOutcome.UnsupportedType -> {
                    onToast("暂不支持该类型二维码")
                    null
                }

                QrResolveOutcome.MissingDevice -> {
                    onToast("二维码中不包含设备信息")
                    null
                }

                QrResolveOutcome.Failed -> {
                    null
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onLogError("resolveScanDeviceId", e)
            onToast("扫码解析失败：${e.message}")
            null
        }
    }

    // 收藏/取消收藏
    fun toggleDeviceFavorite(
        deviceId: String,
        followed: Boolean,
    ) {
        val token = currentToken()
        if (token.isEmpty()) return
        scope.launch {
            try {
                val result = api.devFavo(token, deviceId, remove = followed)
                if (result.success) {
                    onToast(if (followed) "取消收藏" else "收藏成功")
                    loadDeviceInfo(force = true)
                }
            } catch (e: Exception) {
                onLogError("toggleDeviceFavorite", e)
                onToast("操作失败：${e.message}")
            }
        }
    }

    // 设备详情页
    fun loadDeviceDetail(deviceId: String): Job? {
        val useApp = getState().account.appToken.isNotEmpty()
        val token = if (useApp) getState().account.appToken else getState().account.token
        val appType = if (useApp) APP_TYPE_DEVICE else APP_TYPE_POINTS
        if (token.isEmpty()) return null
        if (detailDeviceId != deviceId) {
            detailDeviceId = deviceId
            deviceDetail = null
        }
        return scope.launch {
            try {
                val detail = api.getDeviceDetail(token, deviceId, appType) ?: return@launch
                if (!isCurrentToken(token)) return@launch
                deviceDetail = detail
                setState { state ->
                    state.copy(
                        devices =
                            state.devices.map {
                                if (it.id == deviceId) {
                                    it.copy(
                                        name = it.name.ifEmpty { detail.name },
                                        geneStatus = detail.geneStatus,
                                        deviceStatus = detail.deviceStatus,
                                        dtype = if (it.dtype == 0) detail.dtype else it.dtype,
                                    )
                                } else {
                                    it
                                }
                            },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onLogError("loadDeviceDetail", e)
            }
        }
    }

    fun toggleDeviceRunning(
        deviceId: String,
        args: String = "",
        forceStop: Boolean = false,
    ) {
        val useApp = getState().account.appToken.isNotEmpty()
        val token = if (useApp) getState().account.appToken else getState().account.token
        val appType = if (useApp) APP_TYPE_DEVICE else APP_TYPE_POINTS
        if (token.isEmpty()) return
        val deviceName =
            getState()
                .devices
                .firstOrNull { it.id == deviceId }
                ?.name
                ?.ifEmpty { deviceId } ?: deviceId
        pollingDeviceId = deviceId
        scope.launch {
            try {
                if (!isCurrentToken(token)) return@launch
                val currentStatus = if (forceStop) null else api.getDevStatus(token, deviceId, appType)
                val starting = !forceStop && currentStatus?.geneStatus == 99
                val result =
                    if (starting) {
                        requestNotificationPermission()
                        // 优先钱袋支付（91），失败时自动切换支付宝免密支付（21）
                        val wallet =
                            api.devStart(
                                token,
                                deviceId,
                                appType,
                                ptype = PAY_TYPE_WALLET,
                                args = args,
                                reportError = false,
                            )
                        if (wallet.success) {
                            wallet
                        } else {
                            api.devStart(token, deviceId, appType, ptype = PAY_TYPE_ALIPAY, args = args)
                        }
                    } else {
                        api.devEnd(token, deviceId, appType)
                    }
                if (!result.success) return@launch
                onToast(if (starting) "设备已启动" else "设备已停止")
                RunNotifications.updateDevice(deviceId, deviceName, starting)
                if (starting) {
                    runningDevices[deviceId] = deviceName
                    ensureDeviceMonitor()
                } else {
                    runningDevices.remove(deviceId)
                }
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
                setState { state ->
                    state.copy(
                        devices =
                            state.devices.map {
                                if (it.id == deviceId) {
                                    it.copy(
                                        geneStatus = newStatus?.geneStatus ?: it.geneStatus,
                                        deviceStatus = newStatus?.deviceStatus ?: it.deviceStatus,
                                    )
                                } else {
                                    it
                                }
                            },
                    )
                }
                // 详情页同步最新状态
                if (newStatus != null) {
                    deviceDetail =
                        deviceDetail
                            ?.takeIf { it.id == deviceId }
                            ?.copy(geneStatus = newStatus.geneStatus, deviceStatus = newStatus.deviceStatus)
                }
                val isRunning =
                    newStatus?.geneStatus?.let { it != 99 }
                        ?: currentStatus?.geneStatus?.let { it != 99 }
                        ?: false
                // 启动后以轮询结果校正，停止时已立即上报“已停止”
                if (starting) {
                    RunNotifications.updateDevice(deviceId, deviceName, isRunning)
                    if (isRunning) {
                        runningDevices[deviceId] = deviceName
                        ensureDeviceMonitor()
                    } else {
                        runningDevices.remove(deviceId)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onToast("操作失败：${e.message}")
            } finally {
                if (pollingDeviceId == deviceId) pollingDeviceId = null
            }
        }
    }

    // 周期性检查应用启动后仍在运行的设备；机器端手动停止后收起其运行通知
    private fun ensureDeviceMonitor() {
        if (deviceMonitorJob?.isActive == true) return
        if (runningDevices.isEmpty()) return
        deviceMonitorJob =
            scope.launch {
                while (isActive && runningDevices.isNotEmpty()) {
                    delay(DEVICE_MONITOR_INTERVAL_SECONDS.seconds)
                    val token = currentToken()
                    if (token.isEmpty()) break
                    val appType = if (getState().account.appToken.isNotEmpty()) APP_TYPE_DEVICE else APP_TYPE_POINTS
                    runningDevices.toMap().forEach { (id, name) ->
                        val status =
                            try {
                                api.getDevStatus(token, id, appType)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                onLogError("deviceMonitor", e)
                                null
                            }
                        if (status?.geneStatus == 99) {
                            runningDevices.remove(id)
                            RunNotifications.updateDevice(id, name, false)
                            setState { state ->
                                state.copy(
                                    devices =
                                        state.devices.map {
                                            if (it.id == id) it.copy(geneStatus = 99) else it
                                        },
                                )
                            }
                        }
                    }
                }
            }
    }

    // 通知“停止”按钮：强制停止对应设备（不依赖当前状态判断）
    fun stopDevice(deviceId: String) {
        toggleDeviceRunning(deviceId, forceStop = true)
    }
}
