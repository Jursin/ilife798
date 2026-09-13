package com.github.ilife798.ui.page.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.ilife798.DeviceTile
import com.github.ilife798.DeviceTileResult
import com.github.ilife798.copyToClipboard
import com.github.ilife798.data.model.HomeDeviceType
import com.github.ilife798.data.viewmodel.AppViewModel
import com.github.ilife798.showToast
import com.github.ilife798.ui.component.AppPullToRefresh
import com.github.ilife798.ui.component.BlurredTopAppBar
import com.github.ilife798.ui.component.ConfirmDialog
import com.github.ilife798.ui.component.EmptyStateText
import com.github.ilife798.ui.component.PageScrollColumn
import com.github.ilife798.ui.theme.RunningYellow
import com.github.ilife798.ui.theme.rememberAppBlurBackdrop
import com.github.ilife798.util.formatMoney
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HomePage(
    viewModel: AppViewModel,
    onDeviceAddClick: () -> Unit = {},
    bottomPadding: Dp,
) {
    val state = viewModel.state
    val scrollBehavior = MiuixScrollBehavior()
    val blurBackdrop = rememberAppBlurBackdrop(state.appBlur)

    Scaffold(
        topBar = { BlurredTopAppBar("首页", blurBackdrop, scrollBehavior) },
    ) { paddingValues ->
        AppPullToRefresh(viewModel.homeRefreshing, { viewModel.refreshHome() }, scrollBehavior, paddingValues) {
            PageScrollColumn(
                blurBackdrop = blurBackdrop,
                scrollBehavior = scrollBehavior,
                contentPadding = paddingValues,
                bottomPadding = bottomPadding,
            ) {
                val stats = viewModel.spendingStats
                val isLoggedIn = state.account.hasAnyToken
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SpendingCard(
                        label = "昨日花费",
                        value = stats.yesterday,
                        isLoggedIn = isLoggedIn,
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                    )
                    SpendingCard(
                        label = "今日花费",
                        value = stats.today,
                        isLoggedIn = isLoggedIn,
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                    )
                    SpendingCard(
                        label = "本月平均花费",
                        value = stats.monthAverage,
                        isLoggedIn = isLoggedIn,
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                    )
                }
                DeviceCard(viewModel, onDeviceAddClick)
            }
        }
    }
}

@Composable
private fun SpendingCard(
    label: String,
    value: Double,
    isLoggedIn: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Text(
                text = if (isLoggedIn) "¥${formatMoney(value)}" else "--",
                style = MiuixTheme.textStyles.title2,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun DeviceCard(
    viewModel: AppViewModel,
    onDeviceAddClick: () -> Unit,
) {
    val allDevices = viewModel.state.devices
    val homeType = viewModel.homeDeviceType
    val devices =
        remember(allDevices, homeType) {
            allDevices.filter { HomeDeviceType.fromDeviceType(it.dtype) == homeType }
        }
    val isLoggedIn = viewModel.state.account.hasAnyToken

    var toggleDeviceId by remember { mutableStateOf<String?>(null) }
    var toggleDeviceName by remember { mutableStateOf("") }
    var toggleIsRunning by remember { mutableStateOf(false) }

    var removeDeviceId by remember { mutableStateOf<String?>(null) }
    var removeDeviceName by remember { mutableStateOf("") }

    var tileDeviceId by remember { mutableStateOf<String?>(null) }
    var tileDeviceName by remember { mutableStateOf("") }

    // 快捷设置图块点击：定位到对应设备并触发“启动按钮”逻辑，停在对话框
    val externalDeviceId = viewModel.pendingExternalDeviceId
    LaunchedEffect(externalDeviceId, allDevices) {
        val id = externalDeviceId ?: return@LaunchedEffect
        val device = allDevices.firstOrNull { it.id == id } ?: return@LaunchedEffect
        HomeDeviceType.fromDeviceType(device.dtype)?.let { type ->
            if (viewModel.homeDeviceType != type) viewModel.selectHomeDeviceType(type)
        }
        if (device.geneStatus != 99) {
            toggleDeviceId = device.id
            toggleDeviceName = device.name.ifEmpty { device.id }
            toggleIsRunning = true
        } else {
            viewModel.prepareStartDevice(device)
        }
        viewModel.consumeExternalDeviceStart()
    }

    val deviceTypeEntry =
        remember(viewModel.homeDeviceType) {
            DropdownEntry(
                items =
                    HomeDeviceType.entries.map { type ->
                        DropdownItem(
                            text = type.label,
                            selected = viewModel.homeDeviceType == type,
                            onClick = { viewModel.selectHomeDeviceType(type) },
                        )
                    },
            )
        }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "设备",
                    style = MiuixTheme.textStyles.title2,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        enabled = isLoggedIn,
                        onClick = onDeviceAddClick,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Add,
                            contentDescription = "添加",
                            modifier = Modifier.size(20.dp),
                            tint =
                                if (isLoggedIn) {
                                    MiuixTheme.colorScheme.onSurface
                                } else {
                                    MiuixTheme.colorScheme.onSurface.copy(
                                        alpha = 0.3f,
                                    )
                                },
                        )
                    }
                    OverlayIconDropdownMenu(
                        entry = deviceTypeEntry,
                        enabled = isLoggedIn,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.More,
                            contentDescription = "选择设备类型",
                            modifier = Modifier.size(20.dp),
                            tint =
                                if (isLoggedIn) {
                                    MiuixTheme.colorScheme.onSurface
                                } else {
                                    MiuixTheme.colorScheme.onSurface.copy(
                                        alpha = 0.3f,
                                    )
                                },
                        )
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            if (devices.isEmpty()) {
                EmptyStateText(isLoggedIn, emptyText = "暂无${homeType.label}设备")
            } else {
                devices.forEachIndexed { index, device ->
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                    DeviceItem(
                        device = device,
                        isPolling = viewModel.pollingDeviceId == device.id,
                        onToggle = {
                            if (device.geneStatus != 99) {
                                toggleDeviceId = device.id
                                toggleDeviceName = device.name.ifEmpty { device.id }
                                toggleIsRunning = true
                            } else {
                                viewModel.prepareStartDevice(device)
                            }
                        },
                        onRemove = {
                            removeDeviceId = device.id
                            removeDeviceName = device.name.ifEmpty { device.id }
                        },
                        onLongPress = {
                            tileDeviceId = device.id
                            tileDeviceName = device.name.ifEmpty { device.id }
                        },
                    )
                }
            }
        }
    }

    if (devices.isNotEmpty() && !viewModel.homeTileCreated) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    contentColor = MiuixTheme.colorScheme.onSurface,
                ),
        ) {
            Text(
                text = "长按设备创建快捷设置图块。",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(16.dp),
            )
        }
    }

    if (toggleDeviceId != null) {
        ConfirmDialog(
            title = if (toggleIsRunning) "停止设备" else "启动设备",
            message = if (toggleIsRunning) "确定要停止 $toggleDeviceName 吗？" else "确定要启动 $toggleDeviceName 吗？",
            appBlur = viewModel.state.appBlur,
            confirmText = if (toggleIsRunning) "停止" else "启动",
            onConfirm = {
                toggleDeviceId?.let { viewModel.toggleDeviceRunning(it) }
                toggleDeviceId = null
            },
            onDismiss = { toggleDeviceId = null },
        )
    }

    // 启动选项对话框
    val pendingStart = viewModel.pendingStart
    if (pendingStart != null) {
        var selectedIndex by remember(pendingStart) { mutableStateOf(0) }
        ConfirmDialog(
            title = "启动设备",
            message = "确定要启动 ${pendingStart.deviceName} 吗？",
            appBlur = viewModel.state.appBlur,
            confirmText = "启动",
            onConfirm = { viewModel.confirmPendingStart(selectedIndex) },
            onDismiss = { viewModel.cancelPendingStart() },
        ) {
            val parts = pendingStart.options.parts
            if (parts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                parts.forEachIndexed { index, option ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { selectedIndex = index }
                                .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RadioButton(
                            selected = selectedIndex == index,
                            onClick = { selectedIndex = index },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.name.ifEmpty { "模式 ${option.mode}" },
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurface,
                            )
                            val detail =
                                buildList {
                                    if (option.rate > 0.0) add("¥${formatMoney(option.rate)}")
                                    if (option.maxT > 0) add("最长 ${option.maxT}")
                                }.joinToString(" · ")
                            if (detail.isNotEmpty()) {
                                Text(
                                    text = detail,
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        }
                    }
                }
            } else if (pendingStart.options.subCount > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                repeat(pendingStart.options.subCount) { index ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { selectedIndex = index }
                                .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RadioButton(
                            selected = selectedIndex == index,
                            onClick = { selectedIndex = index },
                        )
                        Text(
                            text = "通道 ${index + 1}",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }

    // 删除设备对话框
    if (removeDeviceId != null) {
        ConfirmDialog(
            title = "删除设备",
            message = "确定要删除 $removeDeviceName 吗？",
            appBlur = viewModel.state.appBlur,
            confirmText = "删除",
            destructive = true,
            onConfirm = {
                removeDeviceId?.let { viewModel.removeDevice(it) }
                removeDeviceId = null
            },
            onDismiss = { removeDeviceId = null },
        )
    }

    // 创建设备快捷设置图块对话框
    if (tileDeviceId != null) {
        ConfirmDialog(
            title = "创建设备快捷设置图块",
            message = "确定要创建设备快捷设置图块吗？",
            appBlur = viewModel.state.appBlur,
            onConfirm = {
                val id = tileDeviceId
                if (id != null) {
                    DeviceTile.bind(id, tileDeviceName) { result ->
                        if (result == DeviceTileResult.ADDED || result == DeviceTileResult.ALREADY_ADDED) {
                            viewModel.markHomeTileCreated()
                        }
                        showToast(
                            when (result) {
                                DeviceTileResult.ADDED -> "已添加设备快捷设置图块"
                                DeviceTileResult.ALREADY_ADDED -> "图块已存在，已更新为当前设备"
                                DeviceTileResult.UNSUPPORTED -> "请在快捷设置面板中手动添加图块"
                                DeviceTileResult.FAILED -> "创建失败"
                            },
                        )
                    }
                }
                tileDeviceId = null
            },
            onDismiss = { tileDeviceId = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceItem(
    device: com.github.ilife798.data.model.Device,
    isPolling: Boolean,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
    onLongPress: () -> Unit,
) {
    val isRunning = device.geneStatus != 99
    val isOffline = device.deviceStatus == 0
    val canToggle = !isPolling && !isOffline
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        copyToClipboard(device.id, "已复制设备编号")
                    },
                    onLongClick = onLongPress,
                ).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name.ifEmpty { device.id },
                style = MiuixTheme.textStyles.title3,
                color = MiuixTheme.colorScheme.onSurface,
            )
            if (device.name.isNotEmpty()) {
                Text(
                    text = device.id,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isRunning) {
                Text(
                    text = "运行中",
                    style = MiuixTheme.textStyles.body2,
                    color = RunningYellow,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            } else if (isOffline) {
                Text(
                    text = "离线",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            } else {
                Text(
                    text = "在线",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            IconButton(
                enabled = canToggle,
                onClick = onToggle,
            ) {
                Icon(
                    imageVector = if (isRunning) MiuixIcons.Pause else MiuixIcons.Play,
                    contentDescription = if (isRunning) "停止" else "启动",
                    modifier = Modifier.size(20.dp),
                    tint =
                        when {
                            isRunning && canToggle -> RunningYellow
                            isRunning && !canToggle -> RunningYellow.copy(alpha = 0.3f)
                            !isRunning && canToggle -> MiuixTheme.colorScheme.primary
                            else -> MiuixTheme.colorScheme.primary.copy(alpha = 0.3f)
                        },
                )
            }
            IconButton(
                enabled = !isPolling,
                onClick = onRemove,
            ) {
                Icon(
                    imageVector = MiuixIcons.Delete,
                    contentDescription = "删除",
                    modifier = Modifier.size(20.dp),
                    tint = if (!isPolling) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.error.copy(alpha = 0.3f),
                )
            }
        }
    }
}
