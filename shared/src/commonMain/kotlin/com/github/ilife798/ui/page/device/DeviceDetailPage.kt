package com.github.ilife798.ui.page.device

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.ilife798.DeviceTile
import com.github.ilife798.DeviceTileResult
import com.github.ilife798.copyToClipboard
import com.github.ilife798.data.model.DeviceStartOptions
import com.github.ilife798.data.viewmodel.AppViewModel
import com.github.ilife798.data.viewmodel.buildDeviceDetailStartArgs
import com.github.ilife798.showToast
import com.github.ilife798.ui.component.BlurredTopAppBar
import com.github.ilife798.ui.component.ConfirmDialog
import com.github.ilife798.ui.component.DeviceIcon
import com.github.ilife798.ui.component.InfoRow
import com.github.ilife798.ui.component.PageScrollColumn
import com.github.ilife798.ui.component.SectionHeader
import com.github.ilife798.ui.component.StatusPill
import com.github.ilife798.ui.component.SwitchPreference
import com.github.ilife798.ui.component.statusTextFor
import com.github.ilife798.ui.theme.SettleAmber
import com.github.ilife798.ui.theme.primaryButtonColors
import com.github.ilife798.ui.theme.rememberAppBlurBackdrop
import com.github.ilife798.util.currentTimeMillis
import com.github.ilife798.util.formatMoney
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.preference.RadioButtonLocation
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val DTYPE_CHARGING = 120
private const val DTYPE_HAIR_DRIER = 20
private const val SENSOR_MULTIPLE = 12
private const val SENSOR_TIME_CTL = 13

// 支持的设备类型描述，其余走“设备”兜底
private fun dtypeLabel(dtype: Int): String =
    when (dtype) {
        6 -> "水表/淋浴器"
        8 -> "管线/饮水机"
        10 -> "洗衣机"
        20 -> "吹风机"
        else -> "设备"
    }

// 启动确认文案
private fun startConfirmMessage(
    dtype: Int,
    modeName: String,
): String =
    when (dtype) {
        DTYPE_CHARGING -> "确认开始充电？"
        10 -> "请确认已将衣物放入洗衣机，确认选择${modeName}模式"
        else -> "请确认选择的模式，点击确定之后，设备将开始运行"
    }

private data class UseButtonState(
    val text: String,
    val enabled: Boolean,
    val settle: Boolean,
    val isStart: Boolean,
)

@Composable
fun DeviceDetailPage(
    viewModel: AppViewModel,
    deviceId: String,
    onBack: () -> Unit,
) {
    val state = viewModel.state
    val isLoggedIn = state.account.hasAnyToken
    val scrollBehavior = MiuixScrollBehavior()
    val blurBackdrop = rememberAppBlurBackdrop(state.appBlur)

    val liveDevice = state.devices.firstOrNull { it.id == deviceId }
    val detail = viewModel.deviceDetail?.takeIf { it.id == deviceId }

    LaunchedEffect(deviceId) {
        if (state.account.hasAnyToken) viewModel.loadDeviceDetail(deviceId)
    }

    val resolvedName =
        liveDevice?.name?.takeIf { it.isNotEmpty() }
            ?: detail?.name?.takeIf { it.isNotEmpty() }
    var cachedName by remember(deviceId) { mutableStateOf("") }
    if (!resolvedName.isNullOrEmpty()) cachedName = resolvedName
    val name = cachedName.ifEmpty { deviceId }
    val dtype = liveDevice?.dtype?.takeIf { it != 0 } ?: detail?.dtype ?: 0
    val geneStatus = liveDevice?.geneStatus ?: detail?.geneStatus ?: 0
    val deviceStatus = liveDevice?.deviceStatus ?: detail?.deviceStatus ?: 1

    val parts = detail?.parts ?: emptyList()
    val subs = detail?.subs ?: emptyList()
    val subCount = subs.size
    val sensors = detail?.sensors ?: emptyList()
    val goods = detail?.goods ?: emptyList()

    var selectedMode by remember(deviceId) { mutableStateOf<Int?>(null) }
    var selectedChannel by remember(deviceId) { mutableStateOf(-1) }
    var showStartConfirm by remember(deviceId) { mutableStateOf(false) }
    var showTileConfirm by remember(deviceId) { mutableStateOf(false) }

    val followed = state.devices.any { it.id == deviceId }

    // 服务端默认选中的通道（isSelect）预选
    LaunchedEffect(detail) {
        if (detail != null && selectedChannel < 0) {
            val index = detail.subs.indexOfFirst { it.isSelect }
            if (index >= 0) selectedChannel = index
        }
    }

    val passageDevice = dtype == DTYPE_HAIR_DRIER || dtype == DTYPE_CHARGING
    val timeCtl = SENSOR_TIME_CTL in sensors
    val hasRateParts = parts.any { it.rate > 0.0 }
    val partsNoRate = parts.isNotEmpty() && !hasRateParts
    val modeRequired =
        when {
            passageDevice -> timeCtl && parts.isNotEmpty()
            else -> hasRateParts
        }
    val channelRequired = subCount > 1 || (dtype == DTYPE_CHARGING && subCount > 0)
    val needsConfirm =
        when (dtype) {
            DTYPE_CHARGING -> true
            DTYPE_HAIR_DRIER -> false
            else -> modeRequired
        }

    val isPolling = viewModel.pollingDeviceId == deviceId
    val isOffline = deviceStatus == 0
    val buttonState =
        when {
            !isLoggedIn -> {
                UseButtonState("请先登录", enabled = false, settle = false, isStart = true)
            }

            isOffline -> {
                UseButtonState("设备离线", enabled = false, settle = false, isStart = true)
            }

            geneStatus == 98 -> {
                UseButtonState("禁用", enabled = false, settle = false, isStart = true)
            }

            partsNoRate -> {
                UseButtonState("未设置费率信息", enabled = false, settle = false, isStart = true)
            }

            geneStatus == 99 -> {
                UseButtonState(
                    if (dtype == DTYPE_CHARGING) "开始充电" else "立即使用",
                    enabled = true,
                    settle = false,
                    isStart = true,
                )
            }

            else -> {
                val now = currentTimeMillis()
                val endTime = detail?.geneEndTime ?: 0L
                val showRemain = endTime > now && SENSOR_MULTIPLE in sensors
                val remainMinutes = if (showRemain) ((endTime - now) / 60000).coerceAtLeast(1) else 0L
                UseButtonState(
                    if (showRemain) "剩余${remainMinutes}分钟" else "立即结算",
                    enabled = true,
                    settle = true,
                    isStart = false,
                )
            }
        }
    val buttonEnabled = buttonState.enabled && !isPolling

    fun doStart() {
        val options = DeviceStartOptions(parts = parts, subCount = subCount, goods = goods)
        val args =
            buildDeviceDetailStartArgs(
                options = options,
                partMode = selectedMode,
                channelIndex = selectedChannel,
                passageDevice = passageDevice,
            )
        // 始终优先钱包（91）支付，不足时 controller 内自动回退支付宝免密（21）
        viewModel.toggleDeviceRunning(deviceId, args)
    }

    fun onStartClick() {
        if (channelRequired && selectedChannel < 0) {
            showToast("请选择通道")
            return
        }
        if (modeRequired && selectedMode == null) {
            showToast("请先选择模式")
            return
        }
        if (needsConfirm) {
            showStartConfirm = true
        } else {
            doStart()
        }
    }

    Scaffold(
        topBar = {
            BlurredTopAppBar(
                title = "设备详情",
                blurBackdrop = blurBackdrop,
                scrollBehavior = scrollBehavior,
                onBack = onBack,
            )
        },
        bottomBar = {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MiuixTheme.colorScheme.surface)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { showTileConfirm = true },
                    ) {
                        Icon(
                            imageVector = MiuixIcons.GridView,
                            contentDescription = "创建快捷设置图块",
                            modifier = Modifier.size(22.dp),
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "图块", style = MiuixTheme.textStyles.body2)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { viewModel.toggleDeviceFavorite(deviceId, followed) },
                    ) {
                        Icon(
                            imageVector = if (followed) MiuixIcons.FavoritesFill else MiuixIcons.Favorites,
                            contentDescription = if (followed) "取消收藏" else "收藏",
                            modifier = Modifier.size(22.dp),
                            tint =
                                if (followed) {
                                    MiuixTheme.colorScheme.primary
                                } else {
                                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                                },
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (followed) "取消" else "收藏",
                            style = MiuixTheme.textStyles.body2,
                            color =
                                if (followed) {
                                    MiuixTheme.colorScheme.primary
                                } else {
                                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                                },
                        )
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (buttonState.isStart) onStartClick() else viewModel.toggleDeviceRunning(deviceId)
                        },
                        enabled = buttonEnabled,
                        colors =
                            if (buttonState.settle) {
                                // 琥珀底白字不明显，文字用黑色
                                ButtonDefaults.buttonColors(color = SettleAmber, contentColor = Color.Black)
                            } else {
                                primaryButtonColors(state.dynamicColor)
                            },
                    ) {
                        Text(text = buttonState.text, fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
    ) { paddingValues ->
        PageScrollColumn(blurBackdrop, scrollBehavior, paddingValues) {
            DeviceInfoCard(
                deviceId = deviceId,
                name = name,
                dtype = dtype,
                geneStatus = geneStatus,
                deviceStatus = deviceStatus,
                enterpriseName = detail?.enterpriseName ?: "",
                enterpriseAbbr = detail?.enterpriseAbbr ?: "",
                contactPhone = detail?.contactPhone ?: "",
            )

            if (isLoggedIn) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    SwitchPreference(
                        title = "开启积分自动抵扣",
                        checked = viewModel.autoDeduct,
                        onCheckedChange = { viewModel.updateAutoDeduct(it) },
                    )
                }
            }

            if (detail == null) {
                if (isLoggedIn || liveDevice != null) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionHeader("设备信息")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                InfiniteProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    size = 20.dp,
                                    strokeWidth = 2.dp,
                                    orbitingDotSize = 3.dp,
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = "正在加载设备信息…",
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        }
                    }
                }
            } else {
                if (parts.isNotEmpty()) {
                    ModeCard(
                        dtype = dtype,
                        parts = parts,
                        selectedMode = selectedMode,
                        onSelect = { selectedMode = it },
                    )
                }

                if (channelRequired) {
                    ChannelCard(
                        subCount = subCount,
                        subs = subs,
                        selectedChannel = selectedChannel,
                        onSelect = { selectedChannel = it },
                    )
                }
            }
        }
    }

    if (showStartConfirm) {
        val selectedPart = parts.firstOrNull { it.mode == selectedMode }
        val modeName = selectedPart?.name?.ifEmpty { "模式 $selectedMode" } ?: "模式 $selectedMode"
        ConfirmDialog(
            title = "启动设备",
            message = startConfirmMessage(dtype, modeName),
            appBlur = state.appBlur,
            confirmText = "启动",
            onConfirm = {
                showStartConfirm = false
                doStart()
            },
            onDismiss = { showStartConfirm = false },
        )
    }

    if (showTileConfirm) {
        ConfirmDialog(
            title = "创建设备快捷设置图块",
            message = "确定要创建设备快捷设置图块吗？",
            appBlur = state.appBlur,
            onConfirm = {
                DeviceTile.bind(deviceId, name) { result ->
                    showToast(
                        when (result) {
                            DeviceTileResult.ADDED -> "已添加设备快捷设置图块"
                            DeviceTileResult.ALREADY_ADDED -> "图块已存在，已更新为当前设备"
                            DeviceTileResult.UNSUPPORTED -> "请在快捷设置面板中手动添加图块"
                            DeviceTileResult.CANCELLED -> "已取消添加图块"
                            DeviceTileResult.FAILED -> "创建失败"
                        },
                    )
                }
                showTileConfirm = false
            },
            onDismiss = { showTileConfirm = false },
        )
    }
}

@Composable
private fun DeviceInfoCard(
    deviceId: String,
    name: String,
    dtype: Int,
    geneStatus: Int,
    deviceStatus: Int,
    enterpriseName: String,
    enterpriseAbbr: String,
    contactPhone: String,
) {
    val statusText = statusTextFor(deviceStatus, geneStatus, dtype)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DeviceIcon(dtype = dtype, size = 40.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = name,
                            style = MiuixTheme.textStyles.title3,
                            color = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        StatusPill(statusText)
                    }
                    Text(
                        text = deviceId,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier =
                            Modifier
                                .padding(top = 2.dp)
                                .clickable { copyToClipboard(deviceId, "已复制设备编号") },
                    )
                }
            }
            if (enterpriseName.isNotEmpty()) {
                InfoRow("商家", enterpriseName)
            }
            if (enterpriseAbbr.isNotEmpty() && enterpriseAbbr != enterpriseName) {
                InfoRow("服务商", enterpriseAbbr)
            }
            if (contactPhone.isNotEmpty()) {
                InfoRow("联系电话", contactPhone, onValueClick = { copyToClipboard(contactPhone, "已复制联系电话") })
            }
        }
    }
}

@Composable
private fun ModeCard(
    dtype: Int,
    parts: List<com.github.ilife798.data.model.DeviceOption>,
    selectedMode: Int?,
    onSelect: (Int) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader("选择需要的${dtypeLabel(dtype)}模式")
            parts.forEach { part ->
                val summary =
                    buildList {
                        if (part.rate > 0.0) add("¥${formatMoney(part.rate)}")
                        if (part.maxT > 0) add("最长 ${part.maxT}")
                    }.joinToString(" · ")
                RadioButtonPreference(
                    title = part.name.ifEmpty { "模式 ${part.mode}" },
                    summary = summary.ifEmpty { null },
                    selected = selectedMode == part.mode,
                    onClick = { onSelect(part.mode) },
                )
            }
        }
    }
}

@Composable
private fun ChannelCard(
    subCount: Int,
    subs: List<com.github.ilife798.data.model.DeviceSubState>,
    selectedChannel: Int,
    onSelect: (Int) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader("选择通道")
            repeat(subCount) { index ->
                val sub = subs.getOrNull(index)
                val available = sub?.available ?: true
                RadioButtonPreference(
                    title = "通道 ${index + 1}",
                    summary =
                        when {
                            !available && sub.err != 0 -> "故障"
                            !available -> "使用中"
                            else -> null
                        },
                    selected = selectedChannel == index,
                    onClick = { onSelect(index) },
                    radioButtonLocation = RadioButtonLocation.End,
                    enabled = available,
                )
            }
        }
    }
}
