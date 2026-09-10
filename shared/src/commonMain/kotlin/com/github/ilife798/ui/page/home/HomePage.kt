package com.github.ilife798.ui.page.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog
import com.github.ilife798.copyTextToClipboard
import com.github.ilife798.data.viewmodel.AppViewModel
import com.github.ilife798.showToast
import com.github.ilife798.ui.theme.WindowBlurEffect
import com.github.ilife798.ui.theme.appBarBlur
import com.github.ilife798.ui.theme.blurAppBarColor
import com.github.ilife798.ui.theme.captureForBlur
import com.github.ilife798.ui.theme.rememberAppBlurBackdrop
import kotlin.math.abs
import kotlin.math.round

@Composable
fun HomePage(viewModel: AppViewModel, onDeviceAddClick: () -> Unit = {}) {
    val state = viewModel.state
    val scrollBehavior = MiuixScrollBehavior()
    val blurBackdrop = rememberAppBlurBackdrop(state.appBlur)

    Scaffold(
        topBar = {
            TopAppBar(
                title = "主页",
                modifier = Modifier.appBarBlur(blurBackdrop),
                color = blurAppBarColor(blurBackdrop),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .captureForBlur(blurBackdrop)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .scrollEndHaptic()
                .overScrollVertical()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val stats = viewModel.spendingStats
            val isLoggedIn = state.account.appToken.isNotEmpty() || state.account.token.isNotEmpty()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SpendingCard(
                    label = "昨日花费",
                    value = stats.yesterday,
                    isLoggedIn = isLoggedIn,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                SpendingCard(
                    label = "今日花费",
                    value = stats.today,
                    isLoggedIn = isLoggedIn,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                SpendingCard(
                    label = "本月平均花费",
                    value = stats.monthAverage,
                    isLoggedIn = isLoggedIn,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
            DeviceCard(viewModel, onDeviceAddClick)
        }
    }
}

@Composable
private fun SpendingCard(label: String, value: Double, isLoggedIn: Boolean, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
            Text(
                text = if (isLoggedIn) "¥${formatMoney(value)}" else "--",
                style = MiuixTheme.textStyles.title2,
                color = MiuixTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatMoney(value: Double): String {
    val negative = value < 0
    val scaled = round(abs(value) * 100).toLong()
    val intPart = scaled / 100
    val frac = (scaled % 100).toString().padStart(2, '0')
    return (if (negative) "-" else "") + "$intPart.$frac"
}

@Composable
private fun DeviceCard(viewModel: AppViewModel, onDeviceAddClick: () -> Unit) {
    val devices = viewModel.state.devices
    val isLoggedIn = viewModel.state.account.appToken.isNotEmpty() || viewModel.state.account.token.isNotEmpty()

    // Toggle device dialog state
    var toggleDeviceId by remember { mutableStateOf<String?>(null) }
    var toggleDeviceName by remember { mutableStateOf("") }
    var toggleIsRunning by remember { mutableStateOf(false) }

    // Remove device dialog state
    var removeDeviceId by remember { mutableStateOf<String?>(null) }
    var removeDeviceName by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "设备",
                    style = MiuixTheme.textStyles.title2,
                    color = MiuixTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        enabled = isLoggedIn,
                        onClick = {
                            viewModel.forceRefresh()
                        }
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Refresh,
                            contentDescription = "刷新",
                            modifier = Modifier.size(20.dp),
                            tint = if (isLoggedIn) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                    IconButton(
                        enabled = isLoggedIn,
                        onClick = onDeviceAddClick
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Add,
                            contentDescription = "添加",
                            modifier = Modifier.size(20.dp),
                            tint = if (isLoggedIn) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            if (devices.isEmpty()) {
                Text(
                    text = if (!isLoggedIn) "请先登录" else "暂无设备",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                devices.forEachIndexed { index, device ->
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                    DeviceItem(
                        device = device,
                        isPolling = viewModel.pollingDeviceId == device.id,
                        onToggle = {
                            toggleDeviceId = device.id
                            toggleDeviceName = device.name.ifEmpty { device.id }
                            toggleIsRunning = device.geneStatus != 99
                        },
                        onRemove = {
                            removeDeviceId = device.id
                            removeDeviceName = device.name.ifEmpty { device.id }
                        }
                    )
                }
            }
        }
    }

    // Toggle device confirmation dialog
    if (toggleDeviceId != null) {
        WindowDialog(
            show = true,
            onDismissRequest = { toggleDeviceId = null },
            title = if (toggleIsRunning) "停止设备" else "启动设备",
            content = {
                WindowBlurEffect(useBlur = viewModel.state.appBlur)
                val dismiss = LocalDismissState.current
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (toggleIsRunning) "确定要停止 $toggleDeviceName 吗？"
                        else "确定要启动 $toggleDeviceName 吗？"
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = { dismiss?.invoke() },
                            text = "取消"
                        )
                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                toggleDeviceId?.let { viewModel.toggleDeviceRunning(it) }
                                dismiss?.invoke()
                            },
                            text = if (toggleIsRunning) "停止" else "启动",
                            colors = ButtonDefaults.textButtonColorsPrimary()
                        )
                    }
                }
            }
        )
    }

    // Remove device confirmation dialog
    if (removeDeviceId != null) {
        WindowDialog(
            show = true,
            onDismissRequest = { removeDeviceId = null },
            title = "删除设备",
            content = {
                WindowBlurEffect(useBlur = viewModel.state.appBlur)
                val dismiss = LocalDismissState.current
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "确定要删除 $removeDeviceName 吗？")
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = { dismiss?.invoke() },
                            text = "取消"
                        )
                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                removeDeviceId?.let { viewModel.removeDevice(it) }
                                dismiss?.invoke()
                            },
                            text = "删除",
                            colors = ButtonDefaults.textButtonColors(
                                textColor = MiuixTheme.colorScheme.error
                            )
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun DeviceItem(
    device: com.github.ilife798.data.model.Device,
    isPolling: Boolean,
    onToggle: () -> Unit,
    onRemove: () -> Unit
) {
    val isRunning = device.geneStatus != 99
    val isOffline = device.deviceStatus == 0
    val canToggle = !isPolling && !isOffline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (copyTextToClipboard(device.id)) showToast("已复制设备编号")
                else showToast("复制失败")
            }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name.ifEmpty { device.id },
                style = MiuixTheme.textStyles.title3,
                color = MiuixTheme.colorScheme.onSurface
            )
            if (device.name.isNotEmpty()) {
                Text(
                    text = device.id,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRunning) {
                Text(
                    text = "运行中",
                    style = MiuixTheme.textStyles.body2,
                    color = Color(0xFFFFD13D),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            } else if (isOffline) {
                Text(
                    text = "离线",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            } else {
                Text(
                    text = "在线",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            IconButton(
                enabled = canToggle,
                onClick = onToggle
            ) {
                Icon(
                    imageVector = if (isRunning) MiuixIcons.Pause else MiuixIcons.Play,
                    contentDescription = if (isRunning) "停止" else "启动",
                    modifier = Modifier.size(20.dp),
                    tint = when {
                        isRunning && canToggle -> Color(0xFFFFD13D)  // 运行中且可操作：黄色
                        isRunning && !canToggle -> Color(0xFFFFD13D).copy(alpha = 0.3f)  // 运行中不可操作：淡黄
                        !isRunning && canToggle -> MiuixTheme.colorScheme.primary  // 非运行且可操作：蓝色
                        else -> MiuixTheme.colorScheme.primary.copy(alpha = 0.3f)  // 非运行不可操作：淡蓝
                    }
                )
            }
            IconButton(
                enabled = !isPolling,
                onClick = onRemove
            ) {
                Icon(
                    imageVector = MiuixIcons.Delete,
                    contentDescription = "删除",
                    modifier = Modifier.size(20.dp),
                    tint = if (!isPolling) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.error.copy(alpha = 0.3f)
                )
            }
        }
    }
}
