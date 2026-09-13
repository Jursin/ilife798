package com.github.ilife798.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.github.ilife798.AppRequests
import com.github.ilife798.data.viewmodel.AppViewModel

// 平台 Intent、桌面快捷方式与运行通知触发的导航/操作请求
@Composable
fun AppIntentEffects(
    viewModel: AppViewModel,
    mainPagerState: MainPagerState,
    navigate: (Page) -> Unit,
    onScanFromShortcut: () -> Unit,
    clearToRoot: () -> Unit,
) {
    // 桌面快捷方式“扫一扫”：冷启动与 onNewIntent 均通过该信号跳转扫码页
    val scanRequestId = AppRequests.scan.id
    LaunchedEffect(scanRequestId) {
        if (scanRequestId <= 0) return@LaunchedEffect
        onScanFromShortcut()
        navigate(Page.DeviceScan)
        AppRequests.scan.consume()
    }

    // 快捷设置图块：回到首页并触发对应设备的“启动按钮”流程，停在弹出对话框
    val startDeviceId = AppRequests.startDevice.value
    LaunchedEffect(startDeviceId) {
        val id = startDeviceId ?: return@LaunchedEffect
        clearToRoot()
        mainPagerState.animateToPage(0)
        viewModel.requestExternalDeviceStart(id)
        AppRequests.startDevice.consume()
    }

    // 桌面快捷方式“运行积分任务”：跳转到任务页并开始运行
    val runTasksRequestId = AppRequests.runTasks.id
    LaunchedEffect(runTasksRequestId) {
        if (runTasksRequestId <= 0) return@LaunchedEffect
        clearToRoot()
        mainPagerState.animateToPage(1)
        viewModel.runTasksFromShortcut()
        AppRequests.runTasks.consume()
    }

    // 点击“正在下载”通知：回到“我的”页并重新弹出下载对话框
    val updateRequestId = AppRequests.showUpdate.id
    LaunchedEffect(updateRequestId) {
        if (updateRequestId <= 0) return@LaunchedEffect
        clearToRoot()
        mainPagerState.animateToPage(2)
        viewModel.reopenUpdateProgressDialog()
        AppRequests.showUpdate.consume()
    }

    // 点击设备运行通知正文：仅回到首页
    val openHomeRequestId = AppRequests.openHome.id
    LaunchedEffect(openHomeRequestId) {
        if (openHomeRequestId <= 0) return@LaunchedEffect
        clearToRoot()
        mainPagerState.animateToPage(0)
        AppRequests.openHome.consume()
    }

    // 点击设备运行通知“停止”按钮：回到首页并停止对应设备
    val stopDeviceId = AppRequests.stopDevice.value
    LaunchedEffect(stopDeviceId) {
        val id = stopDeviceId ?: return@LaunchedEffect
        clearToRoot()
        mainPagerState.animateToPage(0)
        viewModel.stopDevice(id)
        AppRequests.stopDevice.consume()
    }

    // 点击积分任务通知正文：跳转到任务页
    val openTasksRequestId = AppRequests.openTasks.id
    LaunchedEffect(openTasksRequestId) {
        if (openTasksRequestId <= 0) return@LaunchedEffect
        clearToRoot()
        mainPagerState.animateToPage(1)
        AppRequests.openTasks.consume()
    }

    // 点击积分任务通知“停止”按钮：跳转到任务页并停止任务
    val stopTasksRequestId = AppRequests.stopTasks.id
    LaunchedEffect(stopTasksRequestId) {
        if (stopTasksRequestId <= 0) return@LaunchedEffect
        clearToRoot()
        mainPagerState.animateToPage(1)
        viewModel.stopTasks()
        AppRequests.stopTasks.consume()
    }
}
