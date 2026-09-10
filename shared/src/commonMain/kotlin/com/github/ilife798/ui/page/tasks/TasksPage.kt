package com.github.ilife798.ui.page.tasks

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import com.github.ilife798.ui.theme.primaryButtonColors
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import com.github.ilife798.ui.theme.captureForBlur
import top.yukonga.miuix.kmp.icon.extended.Copy
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
import com.github.ilife798.ui.theme.rememberAppBlurBackdrop
import com.github.ilife798.util.getDayOfWeek

@Composable
fun TasksPage(viewModel: AppViewModel) {
    val state = viewModel.state
    val missions = viewModel.missions
    val isLoading = viewModel.isLoading
    val scrollBehavior = MiuixScrollBehavior()
    val blurBackdrop = rememberAppBlurBackdrop(state.appBlur)
    var showStopDialog by remember { mutableStateOf(false) }

    // Check if all tasks are already completed
    LaunchedEffect(missions) {
        if (missions.isNotEmpty() && !viewModel.isLoading && !state.taskCompleted) {
            val allDone = missions.filter { !it.isDailySignin && it.limit > 0 && it.score > 0 }
                .all { it.dailyCompleted >= it.limit }
            val signInDone = missions.firstOrNull { it.isDailySignin }?.let {
                val weekDay = getDayOfWeek()
                (state.weekMask and (1 shl (weekDay - 1))) != 0
            } ?: true
            if (allDone && signInDone) {
                viewModel.setTaskCompleted()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "积分任务",
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
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    when {
                        isLoading -> showStopDialog = true
                        state.account.token.isEmpty() -> showToast("请先完成积分登录")
                        else -> viewModel.runAllTasks()
                    }
                },
                enabled = (!isLoading && !state.taskCompleted) || isLoading,
                colors = primaryButtonColors(state.dynamicColor)
            ) {
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfiniteProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            size = 18.dp,
                            strokeWidth = 2.dp,
                            orbitingDotSize = 3.dp
                        )
                        Text(text = "运行中，点击停止")
                    }
                } else {
                    Text(text = if (state.taskCompleted) "今日任务已完成" else "运行积分任务")
                }
            }

            if (missions.isNotEmpty()) {
                val signIn = missions.firstOrNull { it.isDailySignin }
                val regularMissions = missions.filter { !it.isDailySignin && it.limit > 0 && it.score > 0 }
                val allMissions = listOfNotNull(signIn) + regularMissions

                if (allMissions.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "任务列表 (${allMissions.size})",
                                style = MiuixTheme.textStyles.title2,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            allMissions.forEach { mission ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mission.name,
                                            style = MiuixTheme.textStyles.body2,
                                            color = MiuixTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (mission.isDailySignin) "每次 +${mission.score} 分"
                                            else "上限 ${mission.limit} 次 · 每次 +${mission.score} 分",
                                            style = MiuixTheme.textStyles.body2,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                        )
                                    }
                                    if (mission.isDailySignin) {
                                        val weekDay = getDayOfWeek()
                                        val alreadySigned = (state.weekMask and (1 shl (weekDay - 1))) != 0
                                        if (alreadySigned) {
                                            Text(
                                                text = "已签到",
                                                style = MiuixTheme.textStyles.body2,
                                                color = MiuixTheme.colorScheme.primary
                                            )
                                        }
                                    } else if (mission.dailyCompleted > 0) {
                                        Text(
                                            text = "已完成（${mission.dailyCompleted}/${mission.limit}）",
                                            style = MiuixTheme.textStyles.body2,
                                            color = if (mission.dailyCompleted >= mission.limit) MiuixTheme.colorScheme.primary
                                            else MiuixTheme.colorScheme.onSurfaceVariantSummary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                val isLoggedIn = state.account.appToken.isNotEmpty() || state.account.token.isNotEmpty()
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "任务列表",
                            style = MiuixTheme.textStyles.title2,
                            color = MiuixTheme.colorScheme.onSurface
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = if (isLoggedIn) "暂无数据" else "请先登录",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            if (state.taskLogs.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "运行日志",
                                style = MiuixTheme.textStyles.title2,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            IconButton(
                                minHeight = 35.dp,
                                minWidth = 35.dp,
                                backgroundColor = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                                onClick = {
                                    if (copyTextToClipboard(state.taskLogs.joinToString("\n"))) {
                                        showToast("已复制到剪贴板")
                                    } else {
                                        showToast("复制失败")
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Copy,
                                    contentDescription = "复制日志",
                                    modifier = Modifier.size(20.dp),
                                    tint = MiuixTheme.colorScheme.onSurface.copy(alpha = if (isSystemInDarkTheme()) 0.7f else 0.9f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        state.taskLogs.forEach { log ->
                            Text(
                                text = log,
                                style = MiuixTheme.textStyles.body2,
                                color = if (log.contains("成功")) MiuixTheme.colorScheme.primary
                                else if (log.contains("失败") || log.contains("异常")) MiuixTheme.colorScheme.error
                                else MiuixTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showStopDialog) {
        WindowDialog(
            show = true,
            onDismissRequest = { showStopDialog = false },
            title = "停止任务",
            content = {
                WindowBlurEffect(useBlur = state.appBlur)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "确定要停止正在运行的积分任务吗？")
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = { showStopDialog = false },
                            text = "取消"
                        )
                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.stopTasks()
                                showStopDialog = false
                            },
                            text = "停止",
                            colors = ButtonDefaults.textButtonColorsPrimary()
                        )
                    }
                }
            }
        )
    }
}
